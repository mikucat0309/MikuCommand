package com.github.mikucat0309.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.mikucat0309.command.dispatcher.SimpleDispatcher;
import com.github.mikucat0309.command.spec.CommandSpec;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


/*
 * Test for basic commandspec creation.
 */
public class CommandSpecTest {

    @Test
    public void testNoArgsFunctional() throws CommandException {
        CommandSpec cmd = CommandSpec.builder()
                .executor((src, args) -> CommandResult.empty())
                .build();

        final SimpleDispatcher dispatcher = new SimpleDispatcher();
        dispatcher.register(cmd, "cmd");
        dispatcher.process(Mockito.mock(CommandSource.class), "cmd");
    }

    @Test
    public void testExecutorRequired() {
        NullPointerException thrown = assertThrows(
                NullPointerException.class,
                () -> CommandSpec.builder().build()
        );
        assertEquals("An executor is required", thrown.getMessage());

    }
}
