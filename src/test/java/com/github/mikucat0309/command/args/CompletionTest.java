package com.github.mikucat0309.command.args;

import static org.hamcrest.MatcherAssert.assertThat;

import com.github.mikucat0309.command.CommandSource;
import com.github.mikucat0309.command.args.parsing.InputTokenizer;
import com.github.mikucat0309.command.args.parsing.SingleArg;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.hamcrest.CoreMatchers;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

/*
 * Tests tab completion in commands when optionals are involved
 */
public class CompletionTest {

    private static final CommandElement first = GenericArguments.optionalWeak(
            GenericArguments.choices(
                    "first", ImmutableMap.of("arg", "arg", "arg1", "arg1", "test1", "test1")));

    private static final CommandElement firstNotOptional =
            GenericArguments.choices(
                    "first", ImmutableMap.of("arg", "arg", "arg1", "arg1", "test1", "test1"));
    private static final CommandElement flag = GenericArguments.flags().valueFlag(first, "-flag").buildWith(GenericArguments.none());
    private static final CommandElement second = GenericArguments.optionalWeak(
            GenericArguments.choices(
                    "second", ImmutableMap.of("arg", "arg", "arg2", "arg2", "test2", "test2")));
    private static final CommandElement secondNotOptional =
            GenericArguments.choices("third", ImmutableMap.of("arg3", "arg3", "arg4", "arg4"));
    private static final CommandElement integer = GenericArguments.integer("int");

    private static final CommandElement secondFlag = GenericArguments.flags().valueFlag(firstNotOptional, "-flag").buildWith(second);
    private static final CommandElement secondFlagNotOptional =
            GenericArguments.flags().valueFlag(firstNotOptional, "-flag").buildWith(secondNotOptional);
    private static final CommandElement secondFlagInt = GenericArguments.flags().valueFlag(firstNotOptional, "-flag").buildWith(integer);
    private static final CommandElement secondFlagWithInt = GenericArguments.flags().valueFlag(integer, "-flag").buildWith(secondNotOptional);

    private static final CommandElement third = GenericArguments.optionalWeak(
            GenericArguments.choices("third", ImmutableMap.of("arg3", "arg3", "arg4", "arg4")));
    private static final CommandElement thirdNotOptional =
            GenericArguments.choices("third", ImmutableMap.of("arg3", "arg3", "arg4", "arg4"));
    private static final CommandElement sflag2 = GenericArguments.flags().valueFlag(secondNotOptional, "f").buildWith(GenericArguments.none());
    private static final CommandElement element1 = GenericArguments.seq(first, second, third);
    private static final CommandElement element2 = GenericArguments.seq(first, second, thirdNotOptional);
    private static final CommandElement element3 = GenericArguments.seq(first, secondNotOptional, thirdNotOptional);

    private static final CommandElement tr = GenericArguments.optionalWeak(GenericArguments.markTrue("true"));
    private static final CommandElement element4 = GenericArguments.seq(tr, first, second);

    public static List<Object[]> getTests() {
        ImmutableList.Builder<Object[]> tests = ImmutableList.builder();
        tests.add(new Object[]{"", Lists.newArrayList("arg", "arg1", "test1", "arg2", "test2", "arg3", "arg4"), element1});
        tests.add(new Object[]{"a", Lists.newArrayList("arg", "arg1", "arg2", "arg3", "arg4"), element1});
        tests.add(new Object[]{"arg a", Lists.newArrayList("arg", "arg2", "arg3", "arg4"), element1});
        tests.add(new Object[]{"arg a", Lists.newArrayList("arg3", "arg4"), element3});
        tests.add(new Object[]{"arg", Lists.newArrayList("arg", "arg1", "arg2", "arg3", "arg4"), element1});
        tests.add(new Object[]{"arg1", Lists.newArrayList("arg1"), element1});
        tests.add(new Object[]{"arg1 arg", Lists.newArrayList("arg", "arg2", "arg3", "arg4"), element1});
        tests.add(new Object[]{"arg", Lists.newArrayList("arg", "arg1", "arg2", "arg3", "arg4"), element2});
        tests.add(new Object[]{"arg1", Lists.newArrayList("arg1"), element2});
        tests.add(new Object[]{"arg1", Lists.newArrayList("arg1"), element3});
        tests.add(new Object[]{"arg1 arg", Lists.newArrayList("arg3", "arg4"), element3});
        tests.add(new Object[]{"arg1", Lists.newArrayList("arg1"), element4});
        tests.add(new Object[]{"t", Lists.newArrayList("test1", "test2"), element4});
        tests.add(new Object[]{"--f", Lists.newArrayList("--flag"), flag}); // got an optional value
        tests.add(new Object[]{"-f a", Lists.newArrayList("arg3", "arg4"), sflag2}); // not got an optional value
        tests.add(new Object[]{"-f arg", Lists.newArrayList("arg3", "arg4"), sflag2}); // not got an optional value
        tests.add(new Object[]{"-f arg4", Lists.newArrayList("arg4"), sflag2}); // not got an optional value
        tests.add(new Object[]{"--flag a", Lists.newArrayList("arg", "arg1"), secondFlagNotOptional});
        tests.add(new Object[]{"--flag a", Lists.newArrayList("arg", "arg1"), secondFlagInt});
        tests.add(new Object[]{"--flag=a", Lists.newArrayList("--flag=arg", "--flag=arg1"), secondFlagNotOptional});
        tests.add(new Object[]{"--flag=a", Lists.newArrayList("--flag=arg", "--flag=arg1"), secondFlag});
        tests.add(new Object[]{"--flag=a", Lists.newArrayList("--flag=arg", "--flag=arg1"), secondFlagInt});
        tests.add(new Object[]{"--flag=a", Lists.newArrayList(), secondFlagWithInt});
        tests.add(new Object[]{"--flag=b", Lists.newArrayList(), secondFlag});
        tests.add(new Object[]{"--flag a", Lists.newArrayList("arg3", "arg4"), secondFlagWithInt}); // the flag has no completable value,
        // best we can do.
        return tests.build();
    }

    @ParameterizedTest(name = "{index}: {0} - {1}")
    @MethodSource("getTests")
    public void testTabCompletion(String input, List<String> output, CommandElement element) throws ArgumentParseException {
        List<SingleArg> args = InputTokenizer.quotedStrings(false).tokenize(input, true);
        CommandArgs commandArgs = new CommandArgs(input, args);
        List<String> results = element.complete(Mockito.mock(CommandSource.class), commandArgs, new CommandContext());

        assertThat("Expected [" + String.join(", ", output) + "] but got ["
                        + String.join(", ", results) + "] from arg string \""
                        + args.stream().map(SingleArg::getValue).collect(Collectors.joining(" ")) + "\"",
                results,
                IsIterableContainingInAnyOrder.containsInAnyOrder(output.stream().map(CoreMatchers::equalTo).collect(Collectors.toList())));
    }

}
