package com.github.mikucat0309.command.args;


import com.github.mikucat0309.command.CommandSource;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

public abstract class PatternMatchingCommandElement extends CommandElement {

    private static final String nullKeyArg = "argument";
    final boolean useRegex;

    protected PatternMatchingCommandElement(@Nullable String key, boolean yesIWantRegex) {
        super(key);
        this.useRegex = yesIWantRegex;
    }

    protected PatternMatchingCommandElement(@Nullable String key) {
        this(key, false);
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        var choices = getChoices(source);
        Iterable<Object> ret;
        var arg = args.next();

        // Check to see if we have an exact match first
        var exactMatch = getExactMatch(choices, arg);
        if (exactMatch.isPresent()) {
            // Return this as a collection as this can get transformed by the subclass.
            return Collections.singleton(exactMatch.get());
        }

        if (this.useRegex) {
            var pattern = getFormattedPattern(arg);
            ret = StreamSupport.stream(choices.spliterator(), false)
                    .filter(element -> pattern.matcher(element).find())
                    .map(this::getValue)
                    .collect(Collectors.toList());
        } else {
            ret = StreamSupport.stream(choices.spliterator(), false)
                    .filter(element -> element.regionMatches(true, 0, arg, 0, arg.length()))
                    .map(this::getValue)
                    .collect(Collectors.toList());
        }

        if (!ret.iterator().hasNext()) {
            throw args.createError(String.format("No values matching pattern '%s' present for %s!", arg, getKey() == null
                    ? nullKeyArg : getKey()));
        }
        return ret;
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        var choices = getChoices(src);
        var nextArg = args.nextIfPresent();
        if (nextArg.isPresent()) {
            if (useRegex) {
                choices = StreamSupport.stream(choices.spliterator(), false)
                        .filter(input -> getFormattedPattern(nextArg.get()).matcher(input).find())
                        .collect(Collectors.toList());
            } else {
                var arg = nextArg.get();
                choices = StreamSupport.stream(choices.spliterator(), false)
                        .filter(input -> input.regionMatches(true, 0, arg, 0, arg.length()))
                        .collect(Collectors.toList());
            }
        }
        return ImmutableList.copyOf(choices);
    }

    Pattern getFormattedPattern(String input) {
        if (!input.startsWith("^")) { // Anchor matches to the beginning -- this lets us use find()
            input = "^" + input;
        }
        return Pattern.compile(input, Pattern.CASE_INSENSITIVE);

    }

    protected Optional<Object> getExactMatch(final Iterable<String> choices, final String potentialChoice) {
        return Iterables.tryFind(choices, potentialChoice::equalsIgnoreCase).toJavaUtil().map(this::getValue);
    }

    protected abstract Iterable<String> getChoices(CommandSource source);

    protected abstract Object getValue(String choice) throws IllegalArgumentException;
}
