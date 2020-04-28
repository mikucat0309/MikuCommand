package com.github.mikucat0309.command.dispatcher;

import static com.github.mikucat0309.command.CommandMessageFormatting.SPACE_TEXT;
import static com.google.common.base.Preconditions.checkNotNull;

import com.github.mikucat0309.command.CommandCallable;
import com.github.mikucat0309.command.CommandException;
import com.github.mikucat0309.command.CommandMapping;
import com.github.mikucat0309.command.CommandMessageFormatting;
import com.github.mikucat0309.command.CommandNotFoundException;
import com.github.mikucat0309.command.CommandResult;
import com.github.mikucat0309.command.CommandSource;
import com.github.mikucat0309.command.ImmutableCommandMapping;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public final class SimpleDispatcher implements Dispatcher {

    public static final Disambiguator FIRST_DISAMBIGUATOR = (source, aliasUsed, availableOptions) -> {
        for (CommandMapping mapping : availableOptions) {
            if (mapping.getPrimaryAlias().toLowerCase().equals(aliasUsed.toLowerCase())) {
                return Optional.of(mapping);
            }
        }
        return Optional.of(availableOptions.get(0));
    };

    private final Disambiguator disambiguatorFunc;
    private final ListMultimap<String, CommandMapping> commands = ArrayListMultimap.create();

    public SimpleDispatcher() {
        this(FIRST_DISAMBIGUATOR);
    }

    public SimpleDispatcher(Disambiguator disambiguatorFunc) {
        this.disambiguatorFunc = disambiguatorFunc;
    }

    public Optional<CommandMapping> register(CommandCallable callable, String... alias) {
        checkNotNull(alias, "alias");
        return register(callable, Arrays.asList(alias));
    }

    public Optional<CommandMapping> register(CommandCallable callable, List<String> aliases) {
        return register(callable, aliases, Function.identity());
    }

    public synchronized Optional<CommandMapping> register(CommandCallable callable, List<String> aliases,
            Function<List<String>, List<String>> callback) {
        checkNotNull(aliases, "aliases");
        checkNotNull(callable, "callable");
        checkNotNull(callback, "callback");

        // Invoke the callback with the commands that /can/ be registered
        aliases = ImmutableList.copyOf(callback.apply(aliases));
        if (aliases.isEmpty()) {
            return Optional.empty();
        }
        var primary = aliases.get(0);
        var secondary = aliases.subList(1, aliases.size());
        var mapping = new ImmutableCommandMapping(callable, primary, secondary);

        for (String alias : aliases) {
            this.commands.put(alias.toLowerCase(), mapping);
        }

        return Optional.of(mapping);
    }

    public synchronized Collection<CommandMapping> remove(String alias) {
        return this.commands.removeAll(alias.toLowerCase());
    }

    public synchronized boolean removeAll(Collection<?> aliases) {
        checkNotNull(aliases, "aliases");

        boolean found = false;

        for (Object alias : aliases) {
            if (!this.commands.removeAll(alias.toString().toLowerCase()).isEmpty()) {
                found = true;
            }
        }

        return found;
    }

    public synchronized Optional<CommandMapping> removeMapping(CommandMapping mapping) {
        checkNotNull(mapping, "mapping");

        CommandMapping found = null;

        var it = this.commands.values().iterator();
        while (it.hasNext()) {
            var current = it.next();
            if (current.equals(mapping)) {
                it.remove();
                found = current;
            }
        }

        return Optional.ofNullable(found);
    }

    public synchronized boolean removeMappings(Collection<?> mappings) {
        checkNotNull(mappings, "mappings");

        boolean found = false;

        var it = this.commands.values().iterator();
        while (it.hasNext()) {
            if (mappings.contains(it.next())) {
                it.remove();
                found = true;
            }
        }

        return found;
    }

    @Override
    public synchronized Set<CommandMapping> getCommands() {
        return ImmutableSet.copyOf(this.commands.values());
    }

    @Override
    public synchronized Set<String> getPrimaryAliases() {
        var aliases = new HashSet<String>();

        for (CommandMapping mapping : this.commands.values()) {
            aliases.add(mapping.getPrimaryAlias());
        }

        return Collections.unmodifiableSet(aliases);
    }

    @Override
    public synchronized Set<String> getAliases() {
        var aliases = new HashSet<String>();

        for (CommandMapping mapping : this.commands.values()) {
            aliases.addAll(mapping.getAllAliases());
        }

        return Collections.unmodifiableSet(aliases);
    }

    @Override
    public Optional<CommandMapping> get(String alias) {
        return get(alias, null);
    }

    @Override
    public synchronized Optional<CommandMapping> get(String alias, @Nullable CommandSource source) {
        var results = this.commands.get(alias.toLowerCase());
        var result = Optional.<CommandMapping>empty();
        if (results.size() == 1) {
            result = Optional.of(results.get(0));
        } else if (results.size() > 1) {
            result = this.disambiguatorFunc.disambiguate(source, alias, results);
        }
        return result;
    }

    @Override
    public synchronized boolean containsAlias(String alias) {
        return this.commands.containsKey(alias.toLowerCase());
    }

    @Override
    public boolean containsMapping(CommandMapping mapping) {
        checkNotNull(mapping, "mapping");

        for (CommandMapping test : this.commands.values()) {
            if (mapping.equals(test)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public CommandResult process(CommandSource source, String commandLine) throws CommandException {
        final String[] argSplit = commandLine.split(" ", 2);
        var cmdOptional = get(argSplit[0], source);
        if (cmdOptional.isEmpty()) {
            throw new CommandNotFoundException("commands.generic.notFound", argSplit[0]);
        }
        var arguments = argSplit.length > 1 ? argSplit[1] : "";
        var mapping = cmdOptional.get();
        var spec = mapping.getCallable();
        try {
            return spec.process(source, arguments);
        } catch (CommandNotFoundException e) {
            throw new CommandException(String.format("No such child command: %s", e.getCommand()));
        }
    }

    @Override
    public List<String> getSuggestions(CommandSource src, final String arguments) throws CommandException {
        final String[] argSplit = arguments.split(" ", 2);
        var cmdOptional = get(argSplit[0], src);
        if (argSplit.length == 1) {
            return filterCommands(src, argSplit[0]).stream().collect(ImmutableList.toImmutableList());
        } else if (cmdOptional.isEmpty()) {
            return ImmutableList.of();
        }
        return cmdOptional.get().getCallable().getSuggestions(src, argSplit[1]);
    }

    @Override
    public Optional<String> getShortDescription(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getHelp(CommandSource source) {
        if (this.commands.isEmpty()) {
            return Optional.empty();
        }
        var build = new StringBuilder("Available commands:\n");
        for (Iterator<String> it = filterCommands(source).iterator(); it.hasNext(); ) {
            var mappingOpt = get(it.next(), source);
            if (mappingOpt.isEmpty()) {
                continue;
            }
            var mapping = mappingOpt.get();
            var description = mapping.getCallable().getShortDescription(source);
            build.append(mapping.getPrimaryAlias())
                    .append(SPACE_TEXT)
                    .append(description.orElse(mapping.getCallable().getUsage(source)));
            if (it.hasNext()) {
                build.append("\n");
            }
        }
        return Optional.of(build.toString());
    }

    private Set<String> filterCommands(final CommandSource src) {
        return this.commands.keySet();
    }

    // Filter out commands by String first
    private Set<String> filterCommands(final CommandSource src, String start) {
        ListMultimap<String, CommandMapping> map = Multimaps.filterKeys(this.commands,
                input -> input != null && input.toLowerCase().startsWith(start.toLowerCase()));
        return map.keySet();
    }

    public synchronized int size() {
        return this.commands.size();
    }

    @Override
    public String getUsage(final CommandSource source) {
        var build = new StringBuilder();
        var filteredCommands = filterCommands(source).stream()
                .filter(input -> {
                    if (input == null) {
                        return false;
                    }
                    var ret = get(input, source);
                    return ret.isPresent() && ret.get().getPrimaryAlias().equals(input);
                })
                .collect(Collectors.toList());

        for (Iterator<String> it = filteredCommands.iterator(); it.hasNext(); ) {
            build.append(it.next());
            if (it.hasNext()) {
                build.append(CommandMessageFormatting.PIPE_TEXT);
            }
        }
        return build.toString();
    }

    @Override
    public synchronized Set<CommandMapping> getAll(String alias) {
        return ImmutableSet.copyOf(this.commands.get(alias));
    }

    @Override
    public Multimap<String, CommandMapping> getAll() {
        return ImmutableMultimap.copyOf(this.commands);
    }
}
