package com.github.mikucat0309.command.args.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Collectors;

class SpaceTokenizerTest {

    private static List<String> parseFrom(String args) {
        return SpaceSplitInputTokenizer.INSTANCE.tokenize(args, false).stream()
                .map(SingleArg::getValue).collect(Collectors.toList());
    }

    static List<Object[]> testParameters() {
        return Lists.newArrayList(
                new Object[]{"", ImmutableList.of()},
                new Object[]{" ", ImmutableList.of()},
                new Object[]{"first second third", ImmutableList.of("first", "second", "third")},
                new Object[]{"first second third ", ImmutableList.of("first", "second", "third", "")},
                new Object[]{"first second  third", ImmutableList.of("first", "second", "third")},
                new Object[]{"first second  third ", ImmutableList.of("first", "second", "third", "")},
                new Object[]{"-abc value something --a=b -- pure strings",
                        ImmutableList.of("-abc", "value", "something", "--a=b", "--", "pure", "strings")},
                new Object[]{"a 'single quoted string' is here",
                        ImmutableList.of("a", "'single", "quoted", "string'", "is", "here")},
                new Object[]{"a \"double quoted string\" is here",
                        ImmutableList.of("a", "\"double", "quoted", "string\"", "is", "here")},
                new Object[]{"an \"unterminated quoted string is okay",
                        ImmutableList.of("an", "\"unterminated", "quoted", "string", "is", "okay")},
                new Object[]{"a test argument string ", ImmutableList.of("a", "test", "argument", "string", "")}
        );
    }

    @ParameterizedTest(name = "{index}: {0} - {1}")
    @MethodSource("testParameters")
    void test(String input, List<String> output) {
        assertEquals(output, parseFrom(input));
    }
}
