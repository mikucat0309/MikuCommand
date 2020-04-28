package com.github.mikucat0309.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.github.mikucat0309.command.args.ArgumentParseException;
import com.github.mikucat0309.command.args.GenericArguments;
import com.github.mikucat0309.command.dispatcher.SimpleDispatcher;
import com.github.mikucat0309.command.spec.CommandSpec;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * Tests for child commands.
 */
public class ChildCommandsTest {

    @Test
    public void testEmptyChildrenWorks() throws CommandException {
        final AtomicBoolean parent = new AtomicBoolean();
        final CommandSpec spec = CommandSpec.builder()
                .children(ImmutableMap.<List<String>, CommandSpec>of())
                .executor((s, c) -> {
                    parent.set(true);
                    return CommandResult.success();
                })
                .build();
        final SimpleDispatcher execute = new SimpleDispatcher();
        execute.register(spec, "emptyparent");
        execute.process(mock(CommandSource.class), "emptyparent");

        assertTrue(parent.get());
    }

    @Test
    public void testEmptyChildrenWorksWithArgument() throws CommandException {
        final AtomicBoolean parent = new AtomicBoolean();
        final CommandSpec spec = CommandSpec.builder()
                .arguments(GenericArguments.optional(GenericArguments.string("a")))
                .children(ImmutableMap.<List<String>, CommandSpec>of())
                .executor((s, c) -> {
                    parent.set(true);
                    return CommandResult.success();
                })
                .build();
        final SimpleDispatcher execute = new SimpleDispatcher();
        execute.register(spec, "emptyparentwith");
        execute.process(mock(CommandSource.class), "emptyparentwith child");

        assertTrue(parent.get());
    }

    @Test
    public void testEmptyChildrenWorksWithOptionalArgument() throws CommandException {
        final AtomicBoolean parent = new AtomicBoolean();
        final CommandSpec spec = CommandSpec.builder()
                .arguments(GenericArguments.optional(GenericArguments.string("b")))
                .children(ImmutableMap.<List<String>, CommandSpec>builder()
                        .put(Lists.newArrayList("aaa"),
                                CommandSpec.builder().executor((s, c) -> CommandResult.empty()).build()).build())
                .executor((s, c) -> {
                    parent.set(true);
                    return CommandResult.success();
                })
                .build();
        final SimpleDispatcher execute = new SimpleDispatcher();
        execute.register(spec, "emptyparentwithopt");
        execute.process(mock(CommandSource.class), "emptyparentwithopt");

        assertTrue(parent.get());
    }

    @Test
    public void testSimpleChildCommand() throws CommandException {
        final AtomicBoolean childExecuted = new AtomicBoolean();
        final CommandSpec spec = CommandSpec.builder()
                .children(ImmutableMap.of(ImmutableList.of("child"), CommandSpec.builder()
                        .executor((src, args) -> {
                            childExecuted.set(true);
                            return CommandResult.builder().successCount(1).build();
                        })
                        .build()))
                .build();
        final SimpleDispatcher execute = new SimpleDispatcher();
        execute.register(spec, "parent");
        execute.process(mock(CommandSource.class), "parent child");

        assertTrue(childExecuted.get());
    }

    @Test
    public void testSimpleChildCommandIsSuppressedOnError() throws CommandException {
        final AtomicBoolean parentExecuted = new AtomicBoolean();
        final AtomicBoolean childExecuted = new AtomicBoolean();
        final CommandSpec spec = CommandSpec.builder()
                .children(ImmutableMap.of(ImmutableList.of("child"), CommandSpec.builder()
                        .arguments(GenericArguments.literal("test", "test"))
                        .executor((src, args) -> {
                            childExecuted.set(true);
                            return CommandResult.builder().successCount(1).build();
                        })
                        .build()))
                .arguments(GenericArguments.literal("t", "child"))
                .executor((src, args) -> {
                    parentExecuted.set(true);
                    return CommandResult.success();
                })
                .build();
        final SimpleDispatcher execute = new SimpleDispatcher();
        execute.register(spec, "parent");
        execute.process(mock(CommandSource.class), "parent child");

        assertFalse(childExecuted.get());
        assertTrue(parentExecuted.get());
    }

    @Test
    public void testSimpleChildCommandIsThrownOnErrorWhenSelected() throws CommandException {
        final AtomicBoolean parentExecuted = new AtomicBoolean();
        final AtomicBoolean childExecuted = new AtomicBoolean();
        final CommandSpec spec = CommandSpec.builder()
                .children(ImmutableMap.of(ImmutableList.of("child"), CommandSpec.builder()
                        .arguments(GenericArguments.literal("test", "test"))
                        .executor((src, args) -> {
                            childExecuted.set(true);
                            return CommandResult.builder().successCount(1).build();
                        })
                        .build()))
                .arguments(GenericArguments.literal("t", "child"))
                .executor((src, args) -> {
                    parentExecuted.set(true);
                    return CommandResult.success();
                })
                .childArgumentParseExceptionFallback(false)
                .build();
        final SimpleDispatcher execute = new SimpleDispatcher();
        execute.register(spec, "parent");

        try {
            execute.process(mock(CommandSource.class), "parent child");
        } catch (ArgumentParseException ex) {
            // ignored - we check this with the booleans
        }

        assertFalse(childExecuted.get());
        assertFalse(parentExecuted.get());
    }

    @Test
    public void testErrorOnNonExistentChildWithNoExecutor() throws CommandException {
        final CommandSpec spec = CommandSpec.builder()
                .children(ImmutableMap.of(ImmutableList.of("child"), CommandSpec.builder()
                        .executor((src, args) -> CommandResult.builder().successCount(1).build())
                        .build()))
                .childArgumentParseExceptionFallback(false)
                .build();
        final SimpleDispatcher execute = new SimpleDispatcher();
        execute.register(spec, "parent");

        try {
            execute.process(mock(CommandSource.class), "parent wrong");
        } catch (ArgumentParseException ex) {
            assertEquals("Input command wrong was not a valid subcommand!\nwrong\n^", ex.getMessage());
        }
    }

    @Test
    public void testErrorOnNonExistentChildWithNoOtherParameters() throws CommandException {
        final CommandSpec spec = CommandSpec.builder()
                .children(ImmutableMap.of(ImmutableList.of("child"), CommandSpec.builder()
                        .executor((src, args) -> CommandResult.builder().successCount(1).build())
                        .build()))
                .childArgumentParseExceptionFallback(false)
                .executor((src, args) -> CommandResult.success())
                .build();
        final SimpleDispatcher execute = new SimpleDispatcher();
        execute.register(spec, "parent");

        try {
            execute.process(mock(CommandSource.class), "parent wrong");
        } catch (ArgumentParseException ex) {
            assertEquals("Input command wrong was not a valid subcommand!\nwrong\n^", ex.getMessage());
        }
    }

}
