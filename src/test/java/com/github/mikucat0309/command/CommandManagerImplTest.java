package com.github.mikucat0309.command;

import static com.github.mikucat0309.command.args.GenericArguments.bool;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.github.mikucat0309.command.spec.CommandSpec;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

class CommandManagerImplTest {

    private static final CommandManager cm = new CommandManagerImpl(LoggerFactory.getLogger(CommandManagerImplTest.class));

    @Test
    void registerAndSuggestionAndProcess() {
        var metaData = new MetaData("testcmd");
        var bl = new AtomicBoolean(false);
        var mapping = cm.register(metaData,
                CommandSpec.builder()
                        .description("test")
                        .arguments(
                                bool("testkey")
                        )
                        .executor((src, args) -> {
                            bl.set(args.<Boolean>getOne("testkey").get());
                            return CommandResult.success();
                        })
                        .build(),
                "test1"
        );
        assertTrue(mapping.isPresent());

        var suggestions = cm.getSuggestions(mock(CommandSource.class), "testcmd:t");
        assertEquals(List.of("testcmd:test1"), suggestions);

        cm.process(mock(CommandSource.class), "test1 true");
        assertTrue(bl.get());
        cm.process(mock(CommandSource.class), "test1 false");
        assertFalse(bl.get());
    }
}
