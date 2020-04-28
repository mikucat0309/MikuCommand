package com.github.mikucat0309.command.args.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.mikucat0309.command.args.ArgumentParseException;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class QuotedStringTokenizerTest {

    private static List<String> parseFrom(String args) throws ArgumentParseException {
        return new QuotedStringTokenizer(true, false, false)
                .tokenize(args, false)
                .stream()
                .map(SingleArg::getValue)
                .collect(Collectors.toList());
    }

    @Test
    public void testEmptyString() throws ArgumentParseException {
        assertEquals(Collections.<String>emptyList(), parseFrom(""));
    }

    @Test
    public void testUnquotedString() throws ArgumentParseException {
        assertEquals(ImmutableList.of("first", "second", "third"),
                parseFrom("first second third"));
    }

    @Test
    public void testFlagString() throws ArgumentParseException {
        assertEquals(ImmutableList.of("-abc", "value", "something", "--a=b", "--", "pure", "strings"),
                parseFrom("-abc value something --a=b -- pure strings"));
    }

    @Test
    public void testSingleQuotedString() throws ArgumentParseException {
        assertEquals(ImmutableList.of("a", "single quoted string", "is", "here"),
                parseFrom("a 'single quoted string' is here"));
    }

    @Test
    public void testDoubleQuotedString() throws ArgumentParseException {
        assertEquals(ImmutableList.of("a", "double quoted string", "is", "here"),
                parseFrom("a \"double quoted string\" is here"));
    }

    @Test
    public void testUnterminatedQuote() {
        ArgumentParseException thrown = assertThrows(
                ArgumentParseException.class,
                () -> parseFrom("an \"unterminated quoted string is bad"));
        assertEquals(
                "Unterminated quoted string found\n"
                        + "an \"unterminated quoted string is bad\n"
                        + "                                    ^",
                thrown.getMessage());
    }

    @Test
    public void testEscape() throws ArgumentParseException {
        assertEquals(ImmutableList.of("this", "demonstrates escapes", "\"of", "various", "characters'"),
                parseFrom("this demonstrates\\ escapes \\\"of 'various' characters'"));
    }

    @Test
    public void testTrailingSpace() throws ArgumentParseException {
        assertEquals(ImmutableList.of("a", "test", "argument", "string", ""), parseFrom("a test argument string "));
    }
}
