package com.github.mikucat0309.command;

import static com.github.mikucat0309.command.CommandMessageFormatting.error;
import static com.google.common.base.Preconditions.checkNotNull;

import com.github.mikucat0309.command.args.ArgumentParseException;
import com.github.mikucat0309.command.dispatcher.Disambiguator;
import com.github.mikucat0309.command.dispatcher.SimpleDispatcher;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.annotation.Nullable;


public class CommandManagerImpl implements CommandManager {

    private static final Pattern SPACE_PATTERN = Pattern.compile(" ", Pattern.LITERAL);
    private final Logger logger;
    private final SimpleDispatcher dispatcher;
    private final Multimap<MetaData, CommandMapping> owners = HashMultimap.create();
    private final Map<CommandMapping, MetaData> reverseOwners = new ConcurrentHashMap<>();
    private final Object lock = new Object();


    public CommandManagerImpl(Logger logger) {
        this(logger, SimpleDispatcher.FIRST_DISAMBIGUATOR);
    }

    public CommandManagerImpl(Logger logger, Disambiguator disambiguator) {
        this.logger = logger;
        this.dispatcher = new SimpleDispatcher(disambiguator);
    }

    private static String buildAliasDescription(final boolean caseChanged, final boolean spaceFound) {
        String description = caseChanged ? "an uppercase character" : "";
        if (spaceFound) {
            if (!description.isEmpty()) {
                description += " and ";
            }
            description += "a space";
        }
        return description;
    }

    @Override
    public Optional<CommandMapping> register(MetaData metaData, CommandCallable callable, String... alias) {
        return register(metaData, callable, Arrays.asList(alias));
    }

    @Override
    public Optional<CommandMapping> register(MetaData metaData, CommandCallable callable, List<String> aliases) {
        return register(metaData, callable, aliases, Function.identity());
    }

    @Override
    public Optional<CommandMapping> register(MetaData metaData, CommandCallable callable, List<String> aliases,
            Function<List<String>, List<String>> callback) {
        checkNotNull(metaData, "plugin");

        synchronized (this.lock) {
            // <namespace>:<alias> for all commands
            List<String> aliasesWithPrefix = new ArrayList<>(aliases.size() * 3);
            for (final String originalAlias : aliases) {
                final String alias = this.fixAlias(metaData, originalAlias);
                if (aliasesWithPrefix.contains(alias)) {
                    this.logger.debug("Plugin '{}' is attempting to register duplicate alias '{}'", metaData.id, alias);
                    continue;
                }
                final Collection<CommandMapping> ownedCommands = this.owners.get(metaData);
                for (CommandMapping mapping : this.dispatcher.getAll(alias)) {
                    if (ownedCommands.contains(mapping)) {
                        throw new IllegalArgumentException("A plugin may not register multiple commands for the same alias ('" + alias + "')!");
                    }
                }

                aliasesWithPrefix.add(alias);
                aliasesWithPrefix.add(metaData.id + ':' + alias);
            }

            Optional<CommandMapping> mapping = this.dispatcher.register(callable, aliasesWithPrefix, callback);

            if (mapping.isPresent()) {
                this.owners.put(metaData, mapping.get());
                this.reverseOwners.put(mapping.get(), metaData);
            }

            return mapping;
        }
    }

    private String fixAlias(final MetaData metaData, final String original) {
        String fixed = original.toLowerCase(Locale.ENGLISH);
        final boolean caseChanged = !original.equals(fixed);
        final boolean spaceFound = original.indexOf(' ') > -1;
        if (spaceFound) {
            fixed = SPACE_PATTERN.matcher(fixed).replaceAll("");
        }
        if (caseChanged || spaceFound) {
            final String description = buildAliasDescription(caseChanged, spaceFound);
            this.logger.warn("Plugin '{}' is attempting to register command '{}' with {} - adjusting to '{}'", metaData.id, original, description,
                    fixed);
        }
        return fixed;
    }

    @Override
    public Optional<CommandMapping> removeMapping(CommandMapping mapping) {
        synchronized (this.lock) {
            Optional<CommandMapping> removed = this.dispatcher.removeMapping(mapping);

            removed.ifPresent(this::forgetMapping);

            return removed;
        }
    }

    private void forgetMapping(CommandMapping mapping) {
        Iterator<CommandMapping> it = this.owners.values().iterator();
        while (it.hasNext()) {
            if (it.next().equals(mapping)) {
                it.remove();
                break;
            }
        }
    }

    public Set<MetaData> getMetaDataSet() {
        synchronized (this.lock) {
            return ImmutableSet.copyOf(this.owners.keySet());
        }
    }

    @Override
    public Set<CommandMapping> getCommands() {
        return this.dispatcher.getCommands();
    }

    @Override
    public Set<String> getPrimaryAliases() {
        return this.dispatcher.getPrimaryAliases();
    }

    @Override
    public Set<String> getAliases() {
        return this.dispatcher.getAliases();
    }

    @Override
    public Optional<CommandMapping> get(String alias) {
        return this.dispatcher.get(alias);
    }

    @Override
    public Optional<? extends CommandMapping> get(String alias, @Nullable CommandSource source) {
        return this.dispatcher.get(alias, source);
    }

    @Override
    public Set<? extends CommandMapping> getAll(String alias) {
        return this.dispatcher.getAll(alias);
    }

    @Override
    public Multimap<String, CommandMapping> getAll() {
        return this.dispatcher.getAll();
    }

    @Override
    public boolean containsAlias(String alias) {
        return this.dispatcher.containsAlias(alias);
    }

    @Override
    public boolean containsMapping(CommandMapping mapping) {
        return this.dispatcher.containsMapping(mapping);
    }

    @Override
    public CommandResult process(final CommandSource source, final String command) {

        String commandLine;
        final String[] argSplit = command.split(" ", 2);
        commandLine = command;

        try {
            try {
                return this.dispatcher.process(source, commandLine);
            } catch (InvocationCommandException ex) {
                if (ex.getCause() != null) {
                    throw ex.getCause();
                }
            } catch (CommandPermissionException ex) {
                String text = ex.getMessage();
                if (text != null) {
                    source.sendMessage(error(text));
                }
            } catch (CommandException ex) {
                String text = ex.getMessage();
                if (text != null) {
                    source.sendMessage(error(text));
                }

                if (ex.shouldIncludeUsage()) {
                    final Optional<CommandMapping> mapping = this.dispatcher.get(argSplit[0], source);
                    if (mapping.isPresent()) {
                        String usage;
                        if (ex instanceof ArgumentParseException.WithUsage) {
                            usage = ((ArgumentParseException.WithUsage) ex).getUsage();
                        } else {
                            usage = mapping.get().getCallable().getUsage(source);
                        }

                        source.sendMessage(error(String.format("Usage: /%s %s", argSplit[0], usage)));
                    }
                }
            }
        } catch (Throwable thr) {
            StringBuilder excBuilder;
            if (thr instanceof Exception) {
                String text = thr.getMessage();
                excBuilder = text == null ? new StringBuilder("null") : new StringBuilder();
            } else {
                excBuilder = new StringBuilder(thr.getMessage());
            }
            final StringWriter writer = new StringWriter();
            thr.printStackTrace(new PrintWriter(writer));
            excBuilder.append(writer.toString()
                    .replace("\t", "    ")
                    .replace("\r\n", "\n")
                    .replace("\r", "\n"));

            source.sendMessage(error(String.format("Error occurred while executing command: %s", excBuilder.toString())));
            this.logger.error(String
                    .format("Error occurred while executing command '%s' for source %s: %s", commandLine, source.toString(), thr.getMessage()), thr);
        }
        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource src, String arguments) {
        try {
            List<String> suggestions = new ArrayList<>(this.dispatcher.getSuggestions(src, arguments));
            return ImmutableList.copyOf(suggestions);
        } catch (CommandException e) {
            src.sendMessage(error(String.format("Error getting suggestions: %s", e.getMessage())));
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error occured while tab completing '%s'", arguments), e);
        }
    }

    @Override
    public Optional<String> getShortDescription(CommandSource source) {
        return this.dispatcher.getShortDescription(source);
    }

    @Override
    public Optional<String> getHelp(CommandSource source) {
        return this.dispatcher.getHelp(source);
    }

    @Override
    public String getUsage(CommandSource source) {
        return this.dispatcher.getUsage(source);
    }

    @Override
    public int size() {
        return this.dispatcher.size();
    }

}