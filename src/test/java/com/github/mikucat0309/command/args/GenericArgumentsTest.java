package com.github.mikucat0309.command.args;

import static com.github.mikucat0309.command.args.GenericArguments.allOf;
import static com.github.mikucat0309.command.args.GenericArguments.bool;
import static com.github.mikucat0309.command.args.GenericArguments.choices;
import static com.github.mikucat0309.command.args.GenericArguments.choicesInsensitive;
import static com.github.mikucat0309.command.args.GenericArguments.enumValue;
import static com.github.mikucat0309.command.args.GenericArguments.firstParsing;
import static com.github.mikucat0309.command.args.GenericArguments.integer;
import static com.github.mikucat0309.command.args.GenericArguments.longNum;
import static com.github.mikucat0309.command.args.GenericArguments.none;
import static com.github.mikucat0309.command.args.GenericArguments.optional;
import static com.github.mikucat0309.command.args.GenericArguments.optionalWeak;
import static com.github.mikucat0309.command.args.GenericArguments.remainingJoinedStrings;
import static com.github.mikucat0309.command.args.GenericArguments.repeated;
import static com.github.mikucat0309.command.args.GenericArguments.seq;
import static com.github.mikucat0309.command.args.GenericArguments.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.mikucat0309.command.CommandException;
import com.github.mikucat0309.command.CommandResult;
import com.github.mikucat0309.command.CommandSource;
import com.github.mikucat0309.command.args.parsing.InputTokenizer;
import com.github.mikucat0309.command.spec.CommandExecutor;
import com.github.mikucat0309.command.spec.CommandSpec;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


/*
 * Tests for all argument types contained in GenericArguments.
 */
public class GenericArgumentsTest {

    static final CommandExecutor NULL_EXECUTOR = (src, args) -> CommandResult.empty();
    private static final CommandSource MOCK_SOURCE;

    static {
        MOCK_SOURCE = Mockito.mock(CommandSource.class);
    }

    private static CommandContext parseForInput(String input, CommandElement element) throws ArgumentParseException {
        CommandSpec spec = CommandSpec.builder()
                .arguments(element)
                .executor(NULL_EXECUTOR)
                .build();
        final CommandArgs args = new CommandArgs(input, spec.getInputTokenizer().tokenize(input, false));
        final CommandContext context = new CommandContext();
        spec.populateContext(MOCK_SOURCE, args, context);
        return context;
    }

    @Test
    public void testNone() {
        assertThrows(CommandException.class,
                () -> parseForInput("a", none()));
    }

    @Test
    public void testSequence() {
        assertThrows(ArgumentParseException.class,
                () -> {
                    CommandElement el = seq(string("one"), string("two"), string("three"));
                    CommandContext context = parseForInput("a b c", el);
                    assertEquals("a", context.getOne("one").get());
                    assertEquals("b", context.getOne("two").get());
                    assertEquals("c", context.getOne("three").get());

                    parseForInput("a b", el);
                });

    }

    @Test
    public void testChoices() {
        assertThrows(ArgumentParseException.class,
                () -> {
                    CommandElement el = choices("val", ImmutableMap.of("a", "one", "b", "two"));
                    CommandContext context = parseForInput("a", el);
                    assertEquals("one", context.getOne("val").get());

                    parseForInput("A", el);
                });
    }

    @Test
    public void testChoicesInsensitive() {
        assertThrows(ArgumentParseException.class,
                () -> {
                    CommandElement el = choicesInsensitive("val", ImmutableMap.of("a", "one", "b", "two"));
                    CommandContext context = parseForInput("A", el);
                    assertEquals("one", context.getOne("val").get());

                    parseForInput("c", el);
                });
    }

    @Test
    public void testFirstParsing() throws ArgumentParseException {
        CommandElement el = firstParsing(integer("val"), string("val"));
        CommandContext context = parseForInput("word", el);
        assertEquals("word", context.getOne("val").get());

        context = parseForInput("42", el);
        assertEquals(42, context.getOne("val").get());
    }

    @Test
    public void testOptional() {
        assertThrows(ArgumentParseException.class,
                () -> {
                    CommandElement el = optional(string("val"));
                    CommandContext context = parseForInput("", el);
                    assertFalse(context.getOne("val").isPresent());

                    el = optional(string("val"), "def");
                    context = parseForInput("", el);
                    assertEquals("def", context.getOne("val").get());

                    el = seq(optionalWeak(integer("val")), string("str"));
                    context = parseForInput("hello", el);
                    assertEquals("hello", context.getOne("str").get());

                    el = seq(optional(integer("val"), string("str")));
                    parseForInput("hello", el);
                });
    }

    @Test
    public void testRepeated() throws ArgumentParseException {
        CommandContext context = parseForInput("1 1 2 3 5", repeated(integer("key"), 5));
        assertEquals(ImmutableList.<Object>of(1, 1, 2, 3, 5), ImmutableList.copyOf(context.getAll("key")));
    }

    @Test
    public void testAllOf() throws ArgumentParseException {
        CommandContext context = parseForInput("2 4 8 16 32 64 128", allOf(integer("key")));
        assertEquals(ImmutableList.<Object>of(2, 4, 8, 16, 32, 64, 128), ImmutableList.copyOf(context.getAll("key")));
    }

    @Test
    public void testString() throws ArgumentParseException {
        CommandContext context = parseForInput("\"here it is\"", string("a value"));
        assertEquals("here it is", context.getOne("a value").get());
    }

    @Test
    public void testInteger() throws ArgumentParseException {
        assertThrows(ArgumentParseException.class,
                () -> {
                    CommandElement el = integer("a value");
                    CommandContext context = parseForInput("52", el);
                    assertEquals(52, context.getOne("a value").get());

                    assertEquals(0xdead, parseForInput("0xdead", el).getOne("a value").get());
                    assertEquals(0b101010, parseForInput("0b101010", el).getOne("a value").get());

                    parseForInput("notanumber", integer("a value"));
                });
    }

    @Test
    public void testLong() {
        assertThrows(ArgumentParseException.class,
                () -> {
                    CommandContext context = parseForInput("524903294023901", longNum("a value"));
                    assertEquals(524903294023901L, context.getOne("a value").get());

                    parseForInput("notanumber", integer("a value"));
                });
    }

    @Test
    public void testBool() throws ArgumentParseException {
        assertThrows(ArgumentParseException.class,
                () -> {
                    CommandElement boolEl = bool("val");
                    assertEquals(true, parseForInput("true", boolEl).getOne("val").get());
                    assertEquals(true, parseForInput("t", boolEl).getOne("val").get());
                    assertEquals(false, parseForInput("f", boolEl).getOne("val").get());

                    parseForInput("notabool", boolEl);
                });
    }

    @Test
    public void testEnumValue() throws ArgumentParseException {
        assertThrows(ArgumentParseException.class,
                () -> {
                    CommandElement enumEl = enumValue("val", TestEnum.class);
                    assertEquals(TestEnum.ONE, parseForInput("one", enumEl).getOne("val").get());
                    assertEquals(TestEnum.TWO, parseForInput("TwO", enumEl).getOne("val").get());
                    assertEquals(TestEnum.RED, parseForInput("RED", enumEl).getOne("val").get());

                    parseForInput("notanel", enumEl);
                });
    }

    @Test
    public void testRemainingJoinedStrings() throws ArgumentParseException {
        CommandElement remainingJoined = remainingJoinedStrings("val");
        assertEquals("one", parseForInput("one", remainingJoined).getOne("val").get());
        assertEquals("one big string", parseForInput("one big string", remainingJoined).getOne("val").get());
    }

    @Test
    public void testFirstParsingWhenFirstSequenceSucceeds() throws ArgumentParseException {
        CommandArgs args = new CommandArgs("test test",
                InputTokenizer.spaceSplitString().tokenize("test test", true));
        CommandContext context = new CommandContext();
        CommandElement sut = GenericArguments.firstParsing(
                GenericArguments.seq(GenericArguments.literal("test", "test"),
                        GenericArguments.literal("test1", "test")),
                GenericArguments.seq(GenericArguments.literal("test2", "test"),
                        GenericArguments.literal("test3", "test"))
        );

        sut.parse(MOCK_SOURCE, args, context);
        assertFalse(context.hasAny("test2"));
        assertFalse(context.hasAny("test3"));
        assertTrue(context.hasAny("test"));
        assertTrue(context.hasAny("test"));
    }

    @Test
    public void testFirstParsingWhenFirstSequenceFails() throws ArgumentParseException {
        CommandArgs args = new CommandArgs("test test",
                InputTokenizer.spaceSplitString().tokenize("test test", true));
        CommandContext context = new CommandContext();
        CommandElement sut = GenericArguments.firstParsing(
                GenericArguments.seq(GenericArguments.literal("test", "test"),
                        GenericArguments.literal("test1", "notatest")),
                GenericArguments.seq(GenericArguments.literal("test2", "test"),
                        GenericArguments.literal("test3", "test"))
        );

        sut.parse(MOCK_SOURCE, args, context);
        assertTrue(context.hasAny("test2"));
        assertTrue(context.hasAny("test3"));
        assertFalse(context.hasAny("test"));
        assertFalse(context.hasAny("test1"));
    }

    private enum TestEnum {
        ONE, TWO, RED
    }

}
