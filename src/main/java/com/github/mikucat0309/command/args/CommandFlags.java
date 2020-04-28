package com.github.mikucat0309.command.args;

import com.github.mikucat0309.command.CommandSource;
import com.github.mikucat0309.command.util.StartsWithPredicate;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public final class CommandFlags extends CommandElement {

    @Nullable
    private final CommandElement childElement;
    private final Map<List<String>, CommandElement> usageFlags;
    private final Map<String, CommandElement> shortFlags;
    private final Map<String, CommandElement> longFlags;
    private final UnknownFlagBehavior unknownShortFlagBehavior;
    private final UnknownFlagBehavior unknownLongFlagBehavior;
    private final boolean anchorFlags;

    protected CommandFlags(@Nullable CommandElement childElement, Map<List<String>, CommandElement> usageFlags,
            Map<String, CommandElement> shortFlags, Map<String, CommandElement> longFlags, UnknownFlagBehavior unknownShortFlagBehavior,
            UnknownFlagBehavior unknownLongFlagBehavior, boolean anchorFlags) {
        super(null);
        this.childElement = childElement;
        this.usageFlags = usageFlags;
        this.shortFlags = shortFlags;
        this.longFlags = longFlags;
        this.unknownShortFlagBehavior = unknownShortFlagBehavior;
        this.unknownLongFlagBehavior = unknownLongFlagBehavior;
        this.anchorFlags = anchorFlags;
    }

    @Override
    public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
        var state = args.getSnapshot();
        while (args.hasNext()) {
            var arg = args.next();
            if (arg.startsWith("-")) {
                var start = args.getSnapshot();
                boolean remove;
                if (arg.startsWith("--")) { // Long flag
                    remove = parseLongFlag(source, arg.substring(2), args, context);
                } else {
                    remove = parseShortFlags(source, arg.substring(1), args, context);
                }
                if (remove) {
                    args.removeArgs(start, args.getSnapshot());
                }
            } else if (this.anchorFlags) {
                break;
            }
        }
        // We removed the arguments so we don't parse them as they have already been parsed as flags,
        // so don't restore them here!
        args.applySnapshot(state, false);
        if (this.childElement != null) {
            this.childElement.parse(source, args, context);
        }
    }

    private boolean parseLongFlag(CommandSource source, String longFlag, CommandArgs args, CommandContext context) throws ArgumentParseException {
        String[] flagSplit = longFlag.split("=", 2);
        var flag = flagSplit[0].toLowerCase();
        var element = this.longFlags.get(flag);
        if (element == null) {
            switch (this.unknownLongFlagBehavior) {
                case ERROR:
                    throw args.createError(String.format("Unknown long flag %s specified", flagSplit[0]));
                case ACCEPT_NONVALUE:
                    context.putArg(flag, flagSplit.length == 2 ? flagSplit[1] : true);
                    return true;
                case ACCEPT_VALUE:
                    context.putArg(flag, flagSplit.length == 2 ? flagSplit[1] : args.next());
                    return true;
                case IGNORE:
                    return false;
                default:
                    throw new Error("New UnknownFlagBehavior added without corresponding case clauses");
            }
        } else if (flagSplit.length == 2) {
            args.insertArg(flagSplit[1]);
        }
        element.parse(source, args, context);
        return true;
    }

    private boolean parseShortFlags(CommandSource source, String shortFlags, CommandArgs args, CommandContext context) throws ArgumentParseException {
        for (int i = 0; i < shortFlags.length(); i++) {
            var shortFlag = shortFlags.substring(i, i + 1);
            var element = this.shortFlags.get(shortFlag);
            if (element == null) {
                switch (this.unknownShortFlagBehavior) {
                    case IGNORE:
                        if (i == 0) {
                            return false;
                        }
                        throw args.createError(String.format("Unknown short flag %s specified", shortFlag));
                    case ERROR:
                        throw args.createError(String.format("Unknown short flag %s specified", shortFlag));
                    case ACCEPT_NONVALUE:
                        context.putArg(shortFlag, true);
                        break;
                    case ACCEPT_VALUE:
                        context.putArg(shortFlag, args.next());
                        break;
                    default:
                        throw new Error("New UnknownFlagBehavior added without corresponding case clauses");
                }
            } else {
                element.parse(source, args, context);
            }
        }
        return true;
    }

    @Override
    public String getUsage(CommandSource src) {
        var builder = new ArrayList<String>();
        for (Map.Entry<List<String>, CommandElement> arg : this.usageFlags.entrySet()) {
            builder.add("[");
            for (Iterator<String> it = arg.getKey().iterator(); it.hasNext(); ) {
                var flag = it.next();
                builder.add(flag.length() > 1 ? "--" : "-");
                builder.add(flag);
                if (it.hasNext()) {
                    builder.add("|");
                }
            }
            var usage = arg.getValue().getUsage(src);
            if (usage.trim().length() > 0) {
                builder.add(" ");
                builder.add(usage);
            }
            builder.add("]");
            builder.add(" ");
        }

        if (this.childElement != null) {
            builder.add(this.childElement.getUsage(src));
        }
        return String.join("", builder);
    }

    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) {
        return null;
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        var state = args.getSnapshot();
        while (args.hasNext()) {
            var next = args.nextIfPresent().get();
            if (next.startsWith("-")) {
                var start = args.getSnapshot();
                List<String> ret;
                if (next.startsWith("--")) {
                    ret = tabCompleteLongFlag(next.substring(2), src, args, context);
                } else {
                    ret = tabCompleteShortFlags(next.substring(1), src, args, context);
                }
                if (ret != null) {
                    return ret;
                }
                args.removeArgs(start, args.getSnapshot());
            } else if (this.anchorFlags) {
                break;
            }
        }
        // the modifications are intentional
        args.applySnapshot(state, false);

        // Prevent tab completion gobbling up an argument if the value parsed.
        if (!args.hasNext() && !args.getRaw().matches("\\s+$")) {
            return ImmutableList.of();
        }
        return this.childElement != null ? childElement.complete(src, args, context) : ImmutableList.of();
    }

    @Nullable
    private List<String> tabCompleteLongFlag(String longFlag, CommandSource src, CommandArgs args, CommandContext context) {
        String[] flagSplit = longFlag.split("=", 2);
        boolean isSplitFlag = flagSplit.length == 2;
        var element = this.longFlags.get(flagSplit[0].toLowerCase());
        if (element == null || !isSplitFlag && !args.hasNext()) {
            return this.longFlags.keySet().stream()
                    .filter(new StartsWithPredicate(flagSplit[0]))
                    .map(f -> "--" + f)
                    .collect(Collectors.toList());
        } else if (isSplitFlag) {
            args.insertArg(flagSplit[1]);
        }
        var state = args.getSnapshot();
        List<String> completion;
        try {
            element.parse(src, args, context);
            if (args.getSnapshot().equals(state)) {
                // Not iterated, but succeeded. Check completions to account for optionals
                completion = element.complete(src, args, context);
            } else {
                args.previous();
                var res = args.peek();
                completion = element.complete(src, args, context);
                if (!completion.contains(res)) {
                    completion = ImmutableList.<String>builder().addAll(completion).add(res).build();
                }
            }
        } catch (ArgumentParseException ex) {
            args.applySnapshot(state);
            completion = element.complete(src, args, context);
        }

        if (completion.isEmpty()) {
            if (isSplitFlag) {
                return ImmutableList.of(); // so we don't overwrite the flag
            }
            return null;
        }

        if (isSplitFlag) {
            return completion.stream().map(x -> "--" + flagSplit[0] + "=" + x).collect(Collectors.toList());
        }
        return completion;
    }

    @Nullable
    private List<String> tabCompleteShortFlags(String shortFlags, CommandSource src, CommandArgs args, CommandContext context) {
        for (int i = 0; i < shortFlags.length(); i++) {
            var element = this.shortFlags.get(shortFlags.substring(i, i + 1));
            if (element == null) {
                if (i == 0 && this.unknownShortFlagBehavior == UnknownFlagBehavior.ACCEPT_VALUE) {
                    args.nextIfPresent();
                    return null;
                }
            } else {
                var start = args.getSnapshot();
                try {
                    element.parse(src, args, context);

                    // if the iterator hasn't moved, then just try to complete, no point going backwards.
                    if (args.getSnapshot().equals(start)) {
                        return element.complete(src, args, context);
                    }

                    // if we managed to parse this, then go back to get the completions for it.
                    args.previous();
                    var currentText = args.peek();

                    // ensure this is returned as a valid option
                    var elements = element.complete(src, args, context);
                    if (!elements.contains(currentText)) {
                        return ImmutableList.<String>builder().add(args.peek()).addAll(element.complete(src, args, context)).build();
                    } else {
                        return elements;
                    }
                } catch (ArgumentParseException ex) {
                    args.applySnapshot(start);
                    return element.complete(src, args, context);
                }
            }
        }
        return null;
    }

    public enum UnknownFlagBehavior {
        ERROR,
        ACCEPT_NONVALUE,

        ACCEPT_VALUE,
        IGNORE

    }

    public static class Builder {

        private static final Function<String, CommandElement> MARK_TRUE_FUNC = GenericArguments::markTrue;
        private final Map<List<String>, CommandElement> usageFlags = new HashMap<>();
        private final Map<String, CommandElement> shortFlags = new HashMap<>();
        private final Map<String, CommandElement> longFlags = new HashMap<>();
        private UnknownFlagBehavior unknownLongFlagBehavior = UnknownFlagBehavior.ERROR;
        private UnknownFlagBehavior unknownShortFlagBehavior = UnknownFlagBehavior.ERROR;
        private boolean anchorFlags = false;

        Builder() {
        }

        private Builder flag(Function<String, CommandElement> func, String... specs) {
            var availableFlags = new ArrayList<String>(specs.length);
            CommandElement el = null;
            for (String spec : specs) {
                if (spec.startsWith("-")) {
                    var flagKey = spec.substring(1);
                    if (el == null) {
                        el = func.apply(flagKey);
                    }
                    availableFlags.add(flagKey);
                    this.longFlags.put(flagKey.toLowerCase(), el);
                } else {
                    for (int i = 0; i < spec.length(); ++i) {
                        var flagKey = spec.substring(i, i + 1);
                        if (el == null) {
                            el = func.apply(flagKey);
                        }
                        availableFlags.add(flagKey);
                        this.shortFlags.put(flagKey, el);
                    }
                }
            }
            this.usageFlags.put(availableFlags, el);
            return this;
        }

        public Builder flag(String... specs) {
            return flag(MARK_TRUE_FUNC, specs);
        }

        //        public Builder permissionFlag(final String flagPermission, String... specs) {
        //            return flag(input -> requiringPermission(markTrue(input), flagPermission),specs);
        //        }

        public Builder valueFlag(CommandElement value, String... specs) {
            return flag(ignore -> value, specs);
        }

        @Deprecated
        public Builder setAcceptsArbitraryLongFlags(boolean acceptsArbitraryLongFlags) {
            setUnknownLongFlagBehavior(acceptsArbitraryLongFlags ? UnknownFlagBehavior.ACCEPT_NONVALUE : UnknownFlagBehavior.ERROR);
            return this;
        }

        public Builder setUnknownLongFlagBehavior(UnknownFlagBehavior behavior) {
            this.unknownLongFlagBehavior = behavior;
            return this;
        }

        public Builder setUnknownShortFlagBehavior(UnknownFlagBehavior behavior) {
            this.unknownShortFlagBehavior = behavior;
            return this;
        }

        public Builder setAnchorFlags(boolean anchorFlags) {
            this.anchorFlags = anchorFlags;
            return this;
        }

        public CommandElement buildWith(CommandElement wrapped) {
            return new CommandFlags(wrapped, this.usageFlags, this.shortFlags, this.longFlags, this.unknownShortFlagBehavior,
                    this.unknownLongFlagBehavior, this.anchorFlags);
        }
    }
}
