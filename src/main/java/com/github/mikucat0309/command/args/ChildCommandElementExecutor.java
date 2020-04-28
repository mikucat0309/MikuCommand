package com.github.mikucat0309.command.args;

import static com.github.mikucat0309.command.CommandMessageFormatting.error;

import com.github.mikucat0309.command.CommandCallable;
import com.github.mikucat0309.command.CommandException;
import com.github.mikucat0309.command.CommandMapping;
import com.github.mikucat0309.command.CommandMessageFormatting;
import com.github.mikucat0309.command.CommandResult;
import com.github.mikucat0309.command.CommandSource;
import com.github.mikucat0309.command.dispatcher.SimpleDispatcher;
import com.github.mikucat0309.command.spec.CommandExecutor;
import com.github.mikucat0309.command.spec.CommandSpec;
import com.github.mikucat0309.command.util.StartsWithPredicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

public class ChildCommandElementExecutor extends CommandElement implements CommandExecutor {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final CommandElement NONE = GenericArguments.none();

    @Nullable
    private final CommandExecutor fallbackExecutor;
    @Nullable
    private final CommandElement fallbackElements;
    private final SimpleDispatcher dispatcher = new SimpleDispatcher(SimpleDispatcher.FIRST_DISAMBIGUATOR);
    private final boolean fallbackOnFail;

    @Deprecated
    public ChildCommandElementExecutor(@Nullable CommandExecutor fallbackExecutor) {
        this(fallbackExecutor, null, true);
    }

    public ChildCommandElementExecutor(@Nullable CommandExecutor fallbackExecutor, @Nullable CommandElement fallbackElements,
            boolean fallbackOnFail) {
        super("child" + COUNTER.getAndIncrement());
        this.fallbackExecutor = fallbackExecutor;
        this.fallbackElements = NONE == fallbackElements ? null : fallbackElements;
        this.fallbackOnFail = fallbackOnFail;
    }

    public Optional<CommandMapping> register(CommandCallable callable, List<String> aliases) {
        return this.dispatcher.register(callable, aliases);
    }

    public Optional<CommandMapping> register(CommandCallable callable, String... aliases) {
        return this.dispatcher.register(callable, aliases);
    }

    @Override
    public List<String> complete(final CommandSource src, CommandArgs args, CommandContext context) {
        ArrayList<String> completions = Lists.newArrayList();
        if (this.fallbackElements != null) {
            var state = args.getSnapshot();
            completions.addAll(this.fallbackElements.complete(src, args, context));
            args.applySnapshot(state);
        }

        var commandComponent = args.nextIfPresent();
        if (commandComponent.isEmpty()) {
            return ImmutableList.copyOf(filterCommands(src));
        }
        if (args.hasNext()) {
            var child = this.dispatcher.get(commandComponent.get(), src);
            if (child.isEmpty()) {
                return ImmutableList.of();
            }
            if (child.get().getCallable() instanceof CommandSpec) {
                return ((CommandSpec) child.get().getCallable()).complete(src, args, context);
            }
            args.nextIfPresent();
            var arguments = args.getRaw().substring(args.getRawPosition());
            while (args.hasNext()) {
                args.nextIfPresent();
            }
            try {
                return child.get()
                        .getCallable()
                        .getSuggestions(src, arguments);
            } catch (CommandException e) {
                var eText = e.getMessage();
                if (eText != null) {
                    src.sendMessage(error(eText));
                }
                return ImmutableList.of();
            }
        }
        completions.addAll(filterCommands(src).stream()
                .filter(new StartsWithPredicate(commandComponent.get()))
                .collect(ImmutableList.toImmutableList()));
        return completions;
    }

    private Set<String> filterCommands(final CommandSource src) {
        return this.dispatcher.getAll().keySet();
    }

    @Override
    public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
        if (this.fallbackExecutor != null && !args.hasNext()) {
            if (this.fallbackElements != null) {
                // there might be optionals to take account of that would parse this successfully.
                this.fallbackElements.parse(source, args, context);
            }

            return; // execute the fallback regardless in this scenario.
        }

        var state = args.getSnapshot();
        var key = args.next();
        var optionalCommandMapping = this.dispatcher.get(key, source);
        if (optionalCommandMapping.isPresent()) {
            var mapping = optionalCommandMapping.get();
            try {
                if ((mapping.getCallable() instanceof CommandSpec)) {
                    var spec = ((CommandSpec) mapping.getCallable());
                    spec.populateContext(source, args, context);
                } else {
                    if (args.hasNext()) {
                        args.next();
                    }

                    context.putArg(getKey() + "_args", args.getRaw().substring(args.getRawPosition()));
                    while (args.hasNext()) {
                        args.next();
                    }
                }

                // Success, add to context now so that we don't execute the wrong executor in the first place.
                context.putArg(getKey(), mapping);
            } catch (ArgumentParseException ex) {
                // If we get here, fallback to the elements, if they exist.
                args.applySnapshot(state);
                if (this.fallbackOnFail && this.fallbackElements != null) {
                    this.fallbackElements.parse(source, args, context);
                    return;
                }

                // Get the usage
                args.next();
                if (ex instanceof ArgumentParseException.WithUsage) {
                    // This indicates a previous child failed, so we just prepend our child
                    throw new ArgumentParseException.WithUsage(ex, key + " " + ((ArgumentParseException.WithUsage) ex).getUsage());
                }

                throw new ArgumentParseException.WithUsage(ex, key + " " + mapping.getCallable().getUsage(source));
            }
        } else {
            // Not a child, so let's continue with the fallback.
            if (this.fallbackExecutor != null && this.fallbackElements != null) {
                args.applySnapshot(state);
                this.fallbackElements.parse(source, args, context);
            } else {
                // If we have no elements to parse, then we throw this error - this is the only element
                // so specifying it implicitly means we have a child command to execute.
                throw args.createError(String.format("Input command %s was not a valid subcommand!", key));
            }
        }
    }

    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        return null;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        var mapping = args.<CommandMapping>getOne(getKey()).orElse(null);
        if (mapping == null) {
            if (this.fallbackExecutor == null) {
                throw new CommandException(
                        String.format("Invalid subcommand state -- no more than one mapping may be provided for child arg %s", getKey()));
            }
            return this.fallbackExecutor.execute(src, args);
        }
        if (mapping.getCallable() instanceof CommandSpec) {
            var spec = ((CommandSpec) mapping.getCallable());
            return spec.getExecutor().execute(src, args);
        }
        var arguments = args.<String>getOne(getKey() + "_args").orElse("");
        return mapping.getCallable().process(src, arguments);
    }

    @Override
    public String getUsage(CommandSource src) {
        var usage = this.dispatcher.getUsage(src);
        if (this.fallbackElements == null) {
            return usage;
        }

        var elementUsage = this.fallbackElements.getUsage(src);
        if (elementUsage.isEmpty()) {
            return usage;
        }

        return usage + CommandMessageFormatting.PIPE_TEXT + elementUsage;
    }
}
