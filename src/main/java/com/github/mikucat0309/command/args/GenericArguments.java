package com.github.mikucat0309.command.args;

import com.flowpowered.math.vector.Vector3d;
import com.github.mikucat0309.command.CommandMessageFormatting;
import com.github.mikucat0309.command.CommandSource;
import com.github.mikucat0309.command.util.StartsWithPredicate;
import com.github.mikucat0309.command.util.Tristate;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;


@SuppressWarnings("unused")
public final class GenericArguments {

    private static final CommandElement NONE = new SequenceCommandElement(ImmutableList.of());
    private static final Map<String, Boolean> BOOLEAN_CHOICES = ImmutableMap.<String, Boolean>builder()
            .put("true", true)
            .put("t", true)
            .put("y", true)
            .put("yes", true)
            .put("verymuchso", true)
            .put("1", true)
            .put("false", false)
            .put("f", false)
            .put("n", false)
            .put("no", false)
            .put("notatall", false)
            .put("0", false)
            .build();

    private GenericArguments() {
    }

    public static CommandElement none() {
        return NONE;
    }

    public static CommandElement markTrue(String key) {
        return new MarkTrueCommandElement(key);
    }

    public static CommandElement vector3d(String key) {
        return new Vector3dCommandElement(key);
    }

    public static CommandFlags.Builder flags() {
        return new CommandFlags.Builder();
    }

    public static CommandElement seq(CommandElement... elements) {
        return new SequenceCommandElement(ImmutableList.copyOf(elements));
    }

    public static CommandElement choices(String key, Map<String, ?> choices) {
        return choices(key, choices, choices.size() <= ChoicesCommandElement.CUTOFF, true);
    }

    public static CommandElement choicesInsensitive(String key, Map<String, ?> choices) {
        return choices(key, choices, choices.size() <= ChoicesCommandElement.CUTOFF, false);
    }

    public static CommandElement choices(String key, Map<String, ?> choices, boolean choicesInUsage) {
        return choices(key, choices, choicesInUsage, true);
    }

    public static CommandElement choices(String key, Map<String, ?> choices, boolean choicesInUsage, boolean caseSensitive) {
        if (!caseSensitive) {
            Map<String, Object> immChoices = choices.entrySet().stream()
                    .collect(ImmutableMap.toImmutableMap(x -> x.getKey().toLowerCase(), Map.Entry::getValue));
            return choices(key, immChoices::keySet, selection -> immChoices.get(selection.toLowerCase()), choicesInUsage);
        }
        Map<String, Object> immChoices = ImmutableMap.copyOf(choices);
        return choices(key, immChoices::keySet, immChoices::get, choicesInUsage);
    }

    public static CommandElement choices(String key, Supplier<Collection<String>> keys, Function<String, ?> values) {
        return new ChoicesCommandElement(key, keys, values, Tristate.UNDEFINED);
    }

    public static CommandElement choices(String key, Supplier<Collection<String>> keys, Function<String, ?> values, boolean choicesInUsage) {
        return new ChoicesCommandElement(key, keys, values, choicesInUsage ? Tristate.TRUE : Tristate.FALSE);
    }

    public static CommandElement firstParsing(CommandElement... elements) {
        return new FirstParsingCommandElement(ImmutableList.copyOf(elements));
    }

    public static CommandElement optional(CommandElement element) {
        return new OptionalCommandElement(element, null, false);
    }

    public static CommandElement optional(CommandElement element, Object value) {
        return new OptionalCommandElement(element, value, false);
    }

    public static CommandElement optionalWeak(CommandElement element) {
        return new OptionalCommandElement(element, null, true);
    }

    public static CommandElement optionalWeak(CommandElement element, Object value) {
        return new OptionalCommandElement(element, value, true);
    }

    public static CommandElement repeated(CommandElement element, int times) {
        return new RepeatedCommandElement(element, times);
    }

    public static CommandElement allOf(CommandElement element) {
        return new AllOfCommandElement(element);
    }

    public static CommandElement string(String key) {
        return new StringElement(key);
    }

    public static CommandElement integer(String key) {
        return new NumericElement<>(key, Integer::parseInt, Integer::parseInt,
                input -> String.format("Expected an integer, but input '%s' was not", input));
    }

    public static CommandElement longNum(String key) {
        return new NumericElement<>(key, Long::parseLong, Long::parseLong, input -> String.format("Expected a long, but input '%s' was not", input));
    }

    public static CommandElement doubleNum(String key) {
        return new NumericElement<>(key, Double::parseDouble, null, input -> String.format("Expected a number, but input '%s' was not", input));
    }

    public static CommandElement bool(String key) {
        return GenericArguments.choices(key, BOOLEAN_CHOICES);
    }

    public static <T extends Enum<T>> CommandElement enumValue(String key, Class<T> type) {
        return new EnumValueElement<>(key, type);
    }

    // -- Argument types for basic java types

    public static CommandElement remainingJoinedStrings(String key) {
        return new RemainingJoinedStringsCommandElement(key, false);
    }

    public static CommandElement remainingRawJoinedStrings(String key) {
        return new RemainingJoinedStringsCommandElement(key, true);
    }

    public static CommandElement literal(String key, String... expectedArgs) {
        return new LiteralCommandElement(key, ImmutableList.copyOf(expectedArgs), true);
    }

    public static CommandElement literal(String key, @Nullable Object putValue, String... expectedArgs) {
        return new LiteralCommandElement(key, ImmutableList.copyOf(expectedArgs), putValue);
    }

    public static CommandElement onlyOne(CommandElement element) {
        return new OnlyOneCommandElement(element);
    }

    public static CommandElement url(String key) {
        return new UrlElement(key);
    }

    public static CommandElement ip(String key) {
        return new IpElement(key, false);
    }

    public static CommandElement ipOrSource(String key) {
        return new IpElement(key, true);
    }

    public static CommandElement bigDecimal(String key) {
        return new BigDecimalElement(key);
    }

    public static CommandElement bigInteger(String key) {
        return new BigIntegerElement(key);
    }

    public static CommandElement uuid(String key) {
        return new UuidElement(key);
    }

    public static CommandElement dateTime(String key) {
        return new DateTimeElement(key, false);
    }

    public static CommandElement dateTimeOrNow(String key) {
        return new DateTimeElement(key, true);
    }

    public static CommandElement duration(String key) {
        return new DurationElement(key);
    }

    public static CommandElement withSuggestions(CommandElement argument, Iterable<String> suggestions) {
        return withSuggestions(argument, suggestions, true);
    }

    public static CommandElement withSuggestions(CommandElement argument, Iterable<String> suggestions, boolean requireBegin) {
        return withSuggestions(argument, (s) -> suggestions, requireBegin);
    }

    public static CommandElement withSuggestions(CommandElement argument, Function<CommandSource, Iterable<String>> suggestions) {
        return withSuggestions(argument, suggestions, true);
    }

    public static CommandElement withSuggestions(CommandElement argument, Function<CommandSource,
            Iterable<String>> suggestions, boolean requireBegin) {
        return new WithSuggestionsElement(argument, suggestions, requireBegin);
    }

    public static CommandElement withConstrainedSuggestions(CommandElement argument, Predicate<String> predicate) {
        return new FilteredSuggestionsElement(argument, predicate);
    }

    static class MarkTrueCommandElement extends CommandElement {

        MarkTrueCommandElement(String key) {
            super(key);
        }

        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            return true;
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            return Collections.emptyList();
        }

        @Override
        public String getUsage(CommandSource src) {
            return "";
        }
    }

    private static class SequenceCommandElement extends CommandElement {

        private final List<CommandElement> elements;

        SequenceCommandElement(List<CommandElement> elements) {
            super(null);
            this.elements = elements;
        }

        @Override
        public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
            for (CommandElement element : this.elements) {
                element.parse(source, args, context);
            }
        }

        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            return null;
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            HashSet<String> completions = Sets.newHashSet();
            for (CommandElement element : this.elements) {
                var state = args.getSnapshot();
                var contextSnapshot = context.createSnapshot();
                try {
                    element.parse(src, args, context);

                    // If we get here, the parse occurred successfully.
                    // However, if nothing was consumed, then we should consider
                    // what could have been.
                    var afterSnapshot = context.createSnapshot();
                    if (state.equals(args.getSnapshot())) {
                        context.applySnapshot(contextSnapshot);
                        completions.addAll(element.complete(src, args, context));
                        args.applySnapshot(state);
                        context.applySnapshot(afterSnapshot);
                    } else if (args.hasNext()) {
                        completions.clear();
                    } else {
                        // What we might also have - we have no args left to parse so
                        // while the parse itself was successful, there could be other
                        // valid entries to add...
                        context.applySnapshot(contextSnapshot);
                        args.applySnapshot(state);
                        completions.addAll(element.complete(src, args, context));
                        if (!(element instanceof OptionalCommandElement)) {
                            break;
                        }

                        // The last element was optional, so we go back to before this
                        // element would have been parsed, and assume it never existed...
                        context.applySnapshot(contextSnapshot);
                        args.applySnapshot(state);
                    }
                } catch (ArgumentParseException ignored) {
                    args.applySnapshot(state);
                    context.applySnapshot(contextSnapshot);
                    completions.addAll(element.complete(src, args, context));
                    break;
                }
            }
            return Lists.newArrayList(completions);
        }

        @Override
        public String getUsage(CommandSource commander) {
            var build = new StringBuilder();
            for (Iterator<CommandElement> it = this.elements.iterator(); it.hasNext(); ) {
                var usage = it.next().getUsage(commander);
                if (!usage.isEmpty()) {
                    build.append(usage);
                    if (it.hasNext()) {
                        build.append(CommandMessageFormatting.SPACE_TEXT);
                    }
                }
            }
            return build.toString();
        }
    }

    private static class ChoicesCommandElement extends CommandElement {

        private static final int CUTOFF = 5;
        private final Supplier<Collection<String>> keySupplier;
        private final Function<String, ?> valueSupplier;
        private final Tristate choicesInUsage;

        ChoicesCommandElement(String key, Supplier<Collection<String>> keySupplier, Function<String, ?> valueSupplier, Tristate choicesInUsage) {
            super(key);
            this.keySupplier = keySupplier;
            this.valueSupplier = valueSupplier;
            this.choicesInUsage = choicesInUsage;
        }

        @Override
        public Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            var value = this.valueSupplier.apply(args.next());
            if (value == null) {
                throw args.createError(String.format("Argument was not a valid choice. Valid choices: %s", this.keySupplier.get().toString()));
            }
            return value;
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            var prefix = args.nextIfPresent().orElse("");
            return this.keySupplier.get().stream().filter(new StartsWithPredicate(prefix)).collect(ImmutableList.toImmutableList());
        }

        @Override
        public String getUsage(CommandSource commander) {
            var keys = this.keySupplier.get();
            if (this.choicesInUsage == Tristate.TRUE || (this.choicesInUsage == Tristate.UNDEFINED && keys.size() <= CUTOFF)) {
                var build = new StringBuilder();
                build.append(CommandMessageFormatting.LT_TEXT);
                for (Iterator<String> it = keys.iterator(); it.hasNext(); ) {
                    build.append(it.next());
                    if (it.hasNext()) {
                        build.append(CommandMessageFormatting.PIPE_TEXT);
                    }
                }
                build.append(CommandMessageFormatting.GT_TEXT);
                return build.toString();
            }
            return super.getUsage(commander);
        }
    }

    private static class FirstParsingCommandElement extends CommandElement {

        private final List<CommandElement> elements;

        FirstParsingCommandElement(List<CommandElement> elements) {
            super(null);
            this.elements = elements;
        }

        @Override
        public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
            ArgumentParseException lastException = null;
            for (CommandElement element : this.elements) {
                var startState = args.getSnapshot();
                var contextSnapshot = context.createSnapshot();
                try {
                    element.parse(source, args, context);
                    return;
                } catch (ArgumentParseException ex) {
                    lastException = ex;
                    args.applySnapshot(startState);
                    context.applySnapshot(contextSnapshot);
                }
            }
            if (lastException != null) {
                throw lastException;
            }
        }

        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            return null;
        }

        @Override
        public List<String> complete(final CommandSource src, final CommandArgs args, final CommandContext context) {
            return this.elements.stream()
                    .map(input -> {
                        if (input == null) {
                            return List.<String>of();
                        }

                        var snapshot = args.getSnapshot();
                        List<String> ret = input.complete(src, args, context);
                        args.applySnapshot(snapshot);
                        return ret;
                    })
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }

        @Override
        public String getUsage(CommandSource commander) {
            var ret = new StringBuilder();
            for (Iterator<CommandElement> it = this.elements.iterator(); it.hasNext(); ) {
                ret.append(it.next().getUsage(commander));
                if (it.hasNext()) {
                    ret.append(CommandMessageFormatting.PIPE_TEXT);
                }
            }
            return ret.toString();
        }
    }

    private static class OptionalCommandElement extends CommandElement {

        private final CommandElement element;
        @Nullable
        private final Object value;
        private final boolean considerInvalidFormatEmpty;

        OptionalCommandElement(CommandElement element, @Nullable Object value, boolean considerInvalidFormatEmpty) {
            super(null);
            this.element = element;
            this.value = value;
            this.considerInvalidFormatEmpty = considerInvalidFormatEmpty;
        }

        @Override
        public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
            if (!args.hasNext()) {
                var key = this.element.getKey();
                if (key != null && this.value != null) {
                    context.putArg(key, this.value);
                }
                return;
            }
            var startState = args.getSnapshot();
            try {
                this.element.parse(source, args, context);
            } catch (ArgumentParseException ex) {
                if (this.considerInvalidFormatEmpty || args.hasNext()) { // If there are more args, suppress. Otherwise, throw the error
                    args.applySnapshot(startState);
                    if (this.element.getKey() != null && this.value != null) {
                        context.putArg(this.element.getKey(), this.value);
                    }
                } else {
                    throw ex;
                }
            }
        }

        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            return args.hasNext() ? null : this.element.parseValue(source, args);
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            return this.element.complete(src, args, context);
        }

        @Override
        public String getUsage(CommandSource src) {
            var containingUsage = this.element.getUsage(src);
            if (containingUsage.isEmpty()) {
                return "";
            }
            return "[" + this.element.getUsage(src) + "]";
        }
    }

    private static class RepeatedCommandElement extends CommandElement {

        private final CommandElement element;
        private final int times;


        protected RepeatedCommandElement(CommandElement element, int times) {
            super(null);
            this.element = element;
            this.times = times;
        }

        @Override
        public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
            for (int i = 0; i < this.times; ++i) {
                this.element.parse(source, args, context);
            }
        }

        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            return null;
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            for (int i = 0; i < this.times; ++i) {
                var startState = args.getSnapshot();
                try {
                    this.element.parse(src, args, context);
                } catch (ArgumentParseException e) {
                    args.applySnapshot(startState);
                    return this.element.complete(src, args, context);
                }
            }
            return Collections.emptyList();
        }

        @Override
        public String getUsage(CommandSource src) {
            return this.times + '*' + this.element.getUsage(src);
        }
    }

    private static class AllOfCommandElement extends CommandElement {

        private final CommandElement element;


        protected AllOfCommandElement(CommandElement element) {
            super(null);
            this.element = element;
        }

        @Override
        public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
            while (args.hasNext()) {
                this.element.parse(source, args, context);
            }
        }

        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            return null;
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            while (args.hasNext()) {
                var startState = args.getSnapshot();
                try {
                    this.element.parse(src, args, context);
                } catch (ArgumentParseException e) {
                    args.applySnapshot(startState);
                    return this.element.complete(src, args, context);
                }
            }
            return Collections.emptyList();
        }

        @Override
        public String getUsage(CommandSource context) {
            return this.element.getUsage(context) + CommandMessageFormatting.STAR_TEXT;
        }
    }

    private abstract static class KeyElement extends CommandElement {

        private KeyElement(String key) {
            super(key);
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            return Collections.emptyList();
        }
    }

    private static class StringElement extends KeyElement {

        StringElement(String key) {
            super(key);
        }

        @Override
        public Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            return args.next();
        }
    }

    private static class NumericElement<T extends Number> extends KeyElement {

        private final Function<String, T> parseFunc;
        @Nullable
        private final BiFunction<String, Integer, T> parseRadixFunction;
        private final Function<String, String> errorSupplier;

        protected NumericElement(String key, Function<String, T> parseFunc, @Nullable BiFunction<String, Integer, T> parseRadixFunction,
                Function<String, String> errorSupplier) {
            super(key);
            this.parseFunc = parseFunc;
            this.parseRadixFunction = parseRadixFunction;
            this.errorSupplier = errorSupplier;
        }

        @Override
        public Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            var input = args.next();
            try {
                if (this.parseRadixFunction != null) {
                    if (input.startsWith("0x")) {
                        return this.parseRadixFunction.apply(input.substring(2), 16);
                    } else if (input.startsWith("0b")) {
                        return this.parseRadixFunction.apply(input.substring(2), 2);
                    }
                }
                return this.parseFunc.apply(input);
            } catch (NumberFormatException ex) {
                throw args.createError(this.errorSupplier.apply(input));
            }
        }
    }

    private static class EnumValueElement<T extends Enum<T>> extends PatternMatchingCommandElement {

        private final Class<T> type;
        private final Map<String, T> values;

        EnumValueElement(String key, Class<T> type) {
            super(key);
            this.type = type;
            this.values = Arrays.stream(type.getEnumConstants())
                    .collect(Collectors.toMap(value -> value.name().toLowerCase(),
                            Function.identity(), (value, value2) -> {
                                throw new UnsupportedOperationException(type.getCanonicalName() + " contains more than one enum constant "
                                        + "with the same name, only differing by capitalization, which is unsupported.");
                            }
                    ));
        }

        @Override
        protected Iterable<String> getChoices(CommandSource source) {
            return this.values.keySet();
        }

        @Override
        protected Object getValue(String choice) throws IllegalArgumentException {
            T value = this.values.get(choice.toLowerCase());
            if (value == null) {
                throw new IllegalArgumentException("No enum constant " + this.type.getCanonicalName() + "." + choice);
            }

            return value;
        }
    }

    private static class RemainingJoinedStringsCommandElement extends KeyElement {

        private final boolean raw;

        RemainingJoinedStringsCommandElement(String key, boolean raw) {
            super(key);
            this.raw = raw;
        }

        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            if (this.raw) {
                args.next();
                var ret = args.getRaw().substring(args.getRawPosition());
                while (args.hasNext()) { // Consume remaining args
                    args.next();
                }
                return ret;
            }
            var ret = new StringBuilder(args.next());
            while (args.hasNext()) {
                ret.append(' ').append(args.next());
            }
            return ret.toString();
        }

        @Override
        public String getUsage(CommandSource src) {
            return CommandMessageFormatting.LT_TEXT +
                    getKey() + CommandMessageFormatting.ELLIPSIS_TEXT + CommandMessageFormatting.GT_TEXT;
        }
    }

    private static class LiteralCommandElement extends CommandElement {

        private final List<String> expectedArgs;
        @Nullable
        private final Object putValue;

        protected LiteralCommandElement(@Nullable String key, List<String> expectedArgs, @Nullable Object putValue) {
            super(key);
            this.expectedArgs = ImmutableList.copyOf(expectedArgs);
            this.putValue = putValue;
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            for (String arg : this.expectedArgs) {
                String current;
                if (!(current = args.next()).equalsIgnoreCase(arg)) {
                    throw args.createError(String.format("Argument %s did not match expected next argument %s", current, arg));
                }
            }
            return this.putValue;
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            for (String arg : this.expectedArgs) {
                var next = args.nextIfPresent();
                if (next.isEmpty()) {
                    break;
                } else if (args.hasNext()) {
                    if (!next.get().equalsIgnoreCase(arg)) {
                        break;
                    }
                } else {
                    if (arg.toLowerCase().startsWith(next.get().toLowerCase())) { // Case-insensitive compare
                        return ImmutableList.of(arg); // TODO: Possibly complete all remaining args? Does that even work
                    }
                }
            }
            return ImmutableList.of();
        }

        @Override
        public String getUsage(CommandSource src) {
            return Joiner.on(' ').join(this.expectedArgs);
        }
    }

    private static class Vector3dCommandElement extends CommandElement {

        protected Vector3dCommandElement(@Nullable String key) {
            super(key);
        }

        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            String xStr;
            String yStr;
            String zStr;
            xStr = args.next();
            if (xStr.contains(",")) {
                String[] split = xStr.split(",");
                if (split.length != 3) {
                    throw args.createError(String.format("Comma-separated location must have 3 elements, not %s", split.length));
                }
                xStr = split[0];
                yStr = split[1];
                zStr = split[2];
            } else {
                yStr = args.next();
                zStr = args.next();
            }
            final double x = parseDouble(args, xStr);
            final double y = parseDouble(args, yStr);
            final double z = parseDouble(args, zStr);

            return new Vector3d(x, y, z);
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            var arg = args.nextIfPresent();
            // Traverse through the possible arguments. We can't really complete arbitrary integers
            if (arg.isPresent()) {
                if (!arg.get().contains(",") && args.hasNext()) {
                    arg = args.nextIfPresent();
                    if (args.hasNext()) {
                        return ImmutableList.of(args.nextIfPresent().get());
                    }
                }
                return ImmutableList.of(arg.get());
            }
            return ImmutableList.of();
        }

        private double parseDouble(CommandArgs args, String arg) throws ArgumentParseException {
            try {
                return Double.parseDouble(arg);
            } catch (NumberFormatException e) {
                throw args.createError(String.format("Expected input %s to be a double, but was not", arg));
            }
        }
    }

    private static class OnlyOneCommandElement extends CommandElement {

        private final CommandElement element;

        protected OnlyOneCommandElement(CommandElement element) {
            super(element.getKey());
            this.element = element;
        }

        @Override
        public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
            this.element.parse(source, args, context);
            if (context.getAll(this.element.getKey()).size() > 1) {
                var key = this.element.getKey();
                throw args.createError(String.format("Argument %s may have only one value!", key != null ? key : "unknown"));
            }
        }

        @Override
        public String getUsage(CommandSource src) {
            return this.element.getUsage(src);
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            return this.element.parseValue(source, args);
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            return this.element.complete(src, args, context);
        }
    }

    private static class UrlElement extends KeyElement {

        protected UrlElement(String key) {
            super(key);
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            var str = args.next();
            URL url;
            try {
                url = new URL(str);
            } catch (MalformedURLException ex) {
                throw new ArgumentParseException("Invalid URL!", ex, str, 0);
            }
            try {
                url.toURI();
            } catch (URISyntaxException ex) {
                throw new ArgumentParseException("Invalid URL!", ex, str, 0);
            }
            return url;
        }
    }

    private static class IpElement extends KeyElement {

        private final boolean self;

        protected IpElement(String key, boolean self) {
            super(key);
            this.self = self;
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            if (!args.hasNext() && this.self) {
                throw args.createError("No IP address was specified!");
            }
            var state = args.getSnapshot();
            var s = args.next();
            try {
                return InetAddress.getByName(s);
            } catch (UnknownHostException e) {
                throw args.createError("Invalid IP address!");
            }
        }

        @Override
        public String getUsage(CommandSource src) {
            return super.getUsage(src);
        }
    }

    private static class BigDecimalElement extends KeyElement {

        protected BigDecimalElement(String key) {
            super(key);
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            var next = args.next();
            try {
                return new BigDecimal(next);
            } catch (NumberFormatException ex) {
                throw args.createError(String.format("Expected a number, but input %s was not", next));
            }
        }
    }

    private static class BigIntegerElement extends KeyElement {

        protected BigIntegerElement(String key) {
            super(key);
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            var integerString = args.next();
            try {
                return new BigInteger(integerString);
            } catch (NumberFormatException ex) {
                throw args.createError(String.format("Expected an integer, but input %s was not", integerString));
            }
        }
    }

    private static class UuidElement extends KeyElement {

        protected UuidElement(String key) {
            super(key);
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            try {
                return UUID.fromString(args.next());
            } catch (IllegalArgumentException ex) {
                throw args.createError("Invalid UUID!");
            }
        }

    }

    private static class DateTimeElement extends CommandElement {

        private final boolean returnNow;

        protected DateTimeElement(String key, boolean returnNow) {
            super(key);
            this.returnNow = returnNow;
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            if (!args.hasNext() && this.returnNow) {
                return LocalDateTime.now();
            }
            var state = args.getSnapshot();
            var date = args.next();
            try {
                return LocalDateTime.parse(date);
            } catch (DateTimeParseException ex) {
                try {
                    return LocalDateTime.of(LocalDate.now(), LocalTime.parse(date));
                } catch (DateTimeParseException ex2) {
                    try {
                        return LocalDateTime.of(LocalDate.parse(date), LocalTime.MIDNIGHT);
                    } catch (DateTimeParseException ex3) {
                        if (this.returnNow) {
                            args.applySnapshot(state);
                            return LocalDateTime.now();
                        }
                        throw args.createError("Invalid date-time!");
                    }
                }
            }
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            var date = LocalDateTime.now().withNano(0).toString();
            if (date.startsWith(args.nextIfPresent().orElse(""))) {
                return ImmutableList.of(date);
            } else {
                return ImmutableList.of();
            }
        }

        @Override
        public String getUsage(CommandSource src) {
            if (!this.returnNow) {
                return super.getUsage(src);
            } else {
                return "[" + this.getKey() + "]";
            }
        }
    }

    private static class DurationElement extends KeyElement {

        protected DurationElement(String key) {
            super(key);
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            var s = args.next().toUpperCase();
            if (!s.contains("T")) {
                if (s.contains("D")) {
                    if (s.contains("H") || s.contains("M") || s.contains("S")) {
                        s = s.replace("D", "DT");
                    }
                } else {
                    if (s.startsWith("P")) {
                        s = "PT" + s.substring(1);
                    } else {
                        s = "T" + s;
                    }
                }
            }
            if (!s.startsWith("P")) {
                s = "P" + s;
            }
            try {
                return Duration.parse(s);
            } catch (DateTimeParseException ex) {
                throw args.createError("Invalid duration!");
            }
        }
    }

    private static class WithSuggestionsElement extends CommandElement {

        private final CommandElement wrapped;
        private final Function<CommandSource, Iterable<String>> suggestions;
        private final boolean requireBegin;

        protected WithSuggestionsElement(CommandElement wrapped, Function<CommandSource, Iterable<String>> suggestions, boolean requireBegin) {
            super(wrapped.getKey());
            this.wrapped = wrapped;
            this.suggestions = suggestions;
            this.requireBegin = requireBegin;
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            return this.wrapped.parseValue(source, args);
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            if (this.requireBegin) {
                var arg = args.nextIfPresent().orElse("");
                return ImmutableList.copyOf(StreamSupport.stream(this.suggestions.apply(src).spliterator(), false)
                        .filter(f -> f.startsWith(arg))
                        .collect(Collectors.toList()));
            } else {
                return ImmutableList.copyOf(this.suggestions.apply(src));
            }
        }

    }

    private static class FilteredSuggestionsElement extends CommandElement {

        private final CommandElement wrapped;
        private final Predicate<String> predicate;

        protected FilteredSuggestionsElement(CommandElement wrapped, Predicate<String> predicate) {
            super(wrapped.getKey());
            this.wrapped = wrapped;
            this.predicate = predicate;
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            return this.wrapped.parseValue(source, args);
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            return this.wrapped.complete(src, args, context).stream().filter(this.predicate).collect(ImmutableList.toImmutableList());
        }

    }

}
