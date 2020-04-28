package com.github.mikucat0309.command.args;

import static com.github.mikucat0309.command.args.GenericArguments.flags;
import static com.github.mikucat0309.command.args.GenericArguments.integer;
import static com.github.mikucat0309.command.args.GenericArguments.none;
import static com.github.mikucat0309.command.args.GenericArguments.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.mikucat0309.command.CommandException;
import com.github.mikucat0309.command.CommandResult;
import com.github.mikucat0309.command.CommandSource;
import com.github.mikucat0309.command.args.parsing.InputTokenizer;
import com.github.mikucat0309.command.spec.CommandSpec;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


/*
 * Test for command flags.
 */
public class CommandFlagsTest {

    private static final CommandSource TEST_SOURCE = Mockito.mock(CommandSource.class);

    private static void process(CommandSpec spec, String arguments) throws CommandException {
        final CommandArgs args = new CommandArgs(arguments, spec.getInputTokenizer().tokenize(arguments, false));
        final CommandContext context = new CommandContext();
        spec.populateContext(TEST_SOURCE, args, context);
        spec.getExecutor().execute(TEST_SOURCE, context);
    }

    @Test
    public void testFlaggedCommand() throws CommandException {
        CommandSpec command = CommandSpec.builder()
                .arguments(flags()
                        .flag("a").valueFlag(integer("quot"), "q").buildWith(string("key")))
                .executor((src, args) -> {
                    assertEquals(true, args.getOne("a").get());
                    assertEquals(42, args.getOne("quot").get());
                    assertEquals("something", args.getOne("key").get());
                    return CommandResult.builder().successCount(3).build();
                })
                .build();
        process(command, "-a -q 42 something");
        process(command, "-aq 42 something");
        process(command, "-a something -q 42");
    }

    private CommandContext parseWithInput(CommandElement element, String input) throws ArgumentParseException {
        CommandContext context = new CommandContext();
        element.parse(TEST_SOURCE, new CommandArgs(input, InputTokenizer.quotedStrings(false).tokenize(input, false)), context);
        return context;
    }

    @Test
    public void testUnknownFlagBehaviorError() {
        assertThrows(
                ArgumentParseException.class,
                () -> {
                    CommandElement flags = flags()
                            .setUnknownLongFlagBehavior(CommandFlags.UnknownFlagBehavior.ERROR)
                            .setUnknownShortFlagBehavior(CommandFlags.UnknownFlagBehavior.ERROR)
                            .flag("h", "-help")
                            .buildWith(none());
                    CommandContext context = parseWithInput(flags, "-h");
                    assertTrue(context.hasAny("h"));

                    parseWithInput(flags, "--another");
                });
    }

    @Test
    public void testUnknownFlagBehaviorIgnore() throws ArgumentParseException {
        CommandElement flags = flags()
                .setUnknownLongFlagBehavior(CommandFlags.UnknownFlagBehavior.IGNORE)
                .setUnknownShortFlagBehavior(CommandFlags.UnknownFlagBehavior.IGNORE)
                .flag("h", "-help")
                .buildWith(none());

        CommandContext context = parseWithInput(flags, "-h --other -q");
        assertTrue(context.hasAny("h"));
        assertFalse(context.hasAny("other"));
        assertFalse(context.hasAny("q"));
    }

    @Test
    public void testUnknownFlagBehaviorAcceptNonValue() throws ArgumentParseException {
        CommandElement flags = flags()
                .setUnknownLongFlagBehavior(CommandFlags.UnknownFlagBehavior.ACCEPT_NONVALUE)
                .setUnknownShortFlagBehavior(CommandFlags.UnknownFlagBehavior.ACCEPT_NONVALUE)
                .flag("h", "-help")
                .buildWith(none());

        CommandContext context = parseWithInput(flags, "-h --other something -q else --forceargs=always");
        assertTrue(context.hasAny("h"));
        assertEquals(true, context.getOne("other").get());
        assertEquals(true, context.getOne("q").get());
        assertEquals("always", context.getOne("forceargs").get());
    }

    @Test
    public void testUnknownFlagBehaviorAcceptValue() throws ArgumentParseException {
        CommandElement flags = flags()
                .setUnknownLongFlagBehavior(CommandFlags.UnknownFlagBehavior.ACCEPT_VALUE)
                .setUnknownShortFlagBehavior(CommandFlags.UnknownFlagBehavior.ACCEPT_VALUE)
                .flag("h", "-help")
                .buildWith(none());

        CommandContext context = parseWithInput(flags, "-h --other something -q else --forceargs=always");
        assertTrue(context.hasAny("h"));
        assertEquals("something", context.getOne("other").get());
        assertEquals("else", context.getOne("q").get());
        assertEquals("always", context.getOne("forceargs").get());
    }
}
