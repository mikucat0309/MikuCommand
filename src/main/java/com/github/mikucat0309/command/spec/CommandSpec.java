package com.github.mikucat0309.command.spec;

import static com.google.common.base.Preconditions.checkNotNull;

import com.github.mikucat0309.command.CommandCallable;
import com.github.mikucat0309.command.CommandException;
import com.github.mikucat0309.command.CommandResult;
import com.github.mikucat0309.command.CommandSource;
import com.github.mikucat0309.command.args.ArgumentParseException;
import com.github.mikucat0309.command.args.ChildCommandElementExecutor;
import com.github.mikucat0309.command.args.CommandArgs;
import com.github.mikucat0309.command.args.CommandContext;
import com.github.mikucat0309.command.args.CommandElement;
import com.github.mikucat0309.command.args.GenericArguments;
import com.github.mikucat0309.command.args.parsing.InputTokenizer;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

public final class CommandSpec implements CommandCallable {

    private final CommandElement args;
    private final CommandExecutor executor;
    private final Optional<String> description;
    private final Optional<String> extendedDescription;
    @Nullable
    private final String permission;
    private final InputTokenizer argumentParser;

    CommandSpec(CommandElement args, CommandExecutor executor, @Nullable String description, @Nullable String extendedDescription,
            @Nullable String permission, InputTokenizer parser) {
        this.args = args;
        this.executor = executor;
        this.permission = permission;
        this.description = Optional.ofNullable(description);
        this.extendedDescription = Optional.ofNullable(extendedDescription);
        this.argumentParser = parser;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void populateContext(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
        this.args.parse(source, args, context);
        if (args.hasNext()) {
            args.next();
            throw args.createError("Too many arguments!");
        }
    }

    public List<String> complete(CommandSource source, CommandArgs args, CommandContext context) {
        checkNotNull(source, "source");
        var ret = this.args.complete(source, args, context);
        return ImmutableList.copyOf(ret);
    }

    public CommandExecutor getExecutor() {
        return this.executor;
    }

    public InputTokenizer getInputTokenizer() {
        return this.argumentParser;
    }

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        var args = new CommandArgs(arguments, getInputTokenizer().tokenize(arguments, false));
        var context = new CommandContext();
        this.populateContext(source, args, context);
        return getExecutor().execute(source, context);
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        var args = new CommandArgs(arguments, getInputTokenizer().tokenize(arguments, true));
        var ctx = new CommandContext();
        ctx.putArg(CommandContext.TAB_COMPLETION, true);
        return complete(source, args, ctx);
    }

    @Override
    public Optional<String> getShortDescription(CommandSource source) {
        return this.description;
    }

    public Optional<String> getExtendedDescription(CommandSource source) {
        return this.extendedDescription;
    }

    @Override
    public String getUsage(CommandSource source) {
        checkNotNull(source, "source");
        return this.args.getUsage(source);
    }

    @Override
    public Optional<String> getHelp(CommandSource source) {
        checkNotNull(source, "source");
        var builder = new StringBuilder();
        this.getShortDescription(source).ifPresent((a) -> builder.append(a).append("\n"));
        builder.append(getUsage(source));
        this.getExtendedDescription(source).ifPresent((a) -> builder.append("\n").append(a));
        return Optional.of(builder.toString());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (CommandSpec) o;
        return Objects.equal(this.args, that.args)
                && Objects.equal(this.executor, that.executor)
                && Objects.equal(this.description, that.description)
                && Objects.equal(this.extendedDescription, that.extendedDescription)
                && Objects.equal(this.permission, that.permission)
                && Objects.equal(this.argumentParser, that.argumentParser);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.args, this.executor, this.description, this.extendedDescription, this.permission, this.argumentParser);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("args", this.args)
                .add("executor", this.executor)
                .add("description", this.description)
                .add("extendedDescription", this.extendedDescription)
                .add("permission", this.permission)
                .add("argumentParser", this.argumentParser)
                .toString();
    }

    public static final class Builder {

        private static final CommandElement DEFAULT_ARG = GenericArguments.none();
        private CommandElement args = DEFAULT_ARG;
        @Nullable
        private String description;
        @Nullable
        private String extendedDescription;
        @Nullable
        private String permission;
        @Nullable
        private CommandExecutor executor;
        @Nullable
        private Map<List<String>, CommandCallable> childCommandMap;
        private boolean childCommandFallback = true;
        private InputTokenizer argumentParser = InputTokenizer.quotedStrings(false);

        Builder() {
        }

        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }

        public Builder executor(CommandExecutor executor) {
            checkNotNull(executor, "executor");
            this.executor = executor;
            return this;
        }

        public Builder children(Map<List<String>, ? extends CommandCallable> children) {
            checkNotNull(children, "children");
            if (this.childCommandMap == null) {
                this.childCommandMap = new HashMap<>();
            }
            this.childCommandMap.putAll(children);
            return this;
        }

        public Builder child(CommandCallable child, String... aliases) {
            if (this.childCommandMap == null) {
                this.childCommandMap = new HashMap<>();
            }
            this.childCommandMap.put(ImmutableList.copyOf(aliases), child);
            return this;
        }

        public Builder child(CommandCallable child, Collection<String> aliases) {
            if (this.childCommandMap == null) {
                this.childCommandMap = new HashMap<>();
            }
            this.childCommandMap.put(ImmutableList.copyOf(aliases), child);
            return this;
        }

        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        public Builder extendedDescription(@Nullable String extendedDescription) {
            this.extendedDescription = extendedDescription;
            return this;
        }

        public Builder childArgumentParseExceptionFallback(boolean childCommandFallback) {
            this.childCommandFallback = childCommandFallback;
            return this;
        }

        public Builder arguments(CommandElement args) {
            checkNotNull(args, "args");
            this.args = GenericArguments.seq(args);
            return this;
        }

        public Builder arguments(CommandElement... args) {
            checkNotNull(args, "args");
            this.args = GenericArguments.seq(args);
            return this;
        }

        public Builder inputTokenizer(InputTokenizer parser) {
            checkNotNull(parser, "parser");
            this.argumentParser = parser;
            return this;
        }

        public CommandSpec build() {
            if (this.childCommandMap == null || this.childCommandMap.isEmpty()) {
                checkNotNull(this.executor, "An executor is required");
            } else if (this.executor == null) {
                var childCommandElementExecutor =
                        registerInDispatcher(new ChildCommandElementExecutor(null, null, false));
                if (this.args == DEFAULT_ARG) {
                    arguments(childCommandElementExecutor);
                } else {
                    arguments(this.args, childCommandElementExecutor);
                }
            } else {
                arguments(registerInDispatcher(new ChildCommandElementExecutor(this.executor, this.args, this.childCommandFallback)));
            }

            return new CommandSpec(this.args, this.executor, this.description, this.extendedDescription, this.permission,
                    this.argumentParser);
        }

        @SuppressWarnings({"ConstantConditions"})
        private ChildCommandElementExecutor registerInDispatcher(ChildCommandElementExecutor childDispatcher) {
            for (Map.Entry<List<String>, ? extends CommandCallable> spec : this.childCommandMap.entrySet()) {
                childDispatcher.register(spec.getValue(), spec.getKey());
            }

            executor(childDispatcher);
            return childDispatcher;
        }
    }
}
