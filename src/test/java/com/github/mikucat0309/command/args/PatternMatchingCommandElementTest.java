package com.github.mikucat0309.command.args;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.mikucat0309.command.CommandSource;
import com.github.mikucat0309.command.args.parsing.InputTokenizer;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

public class PatternMatchingCommandElementTest {

    private final InputTokenizer TOKENIZER = InputTokenizer.quotedStrings(true);
    private CommandElement testElement;
    private String testKey;

    @BeforeEach
    public void initialize() {
        this.testKey = "testkey";
        this.testElement = new TestCommandElement(this.testKey);
    }

    @Test
    public void testGetOneElementThatIsSubstringOfAnother() throws ArgumentParseException {
        CommandSource commandSource = Mockito.mock(CommandSource.class);
        CommandArgs args = new CommandArgs("test1", TOKENIZER.tokenize("test1", true));
        CommandContext context = new CommandContext();
        this.testElement.parse(commandSource, args, context);
        assertEquals("test1", context.requireOne(this.testKey));
    }

    @Test
    public void testGetAllElementsThatMatchTestSubstring() throws ArgumentParseException {
        CommandSource commandSource = Mockito.mock(CommandSource.class);
        CommandArgs args = new CommandArgs("test", TOKENIZER.tokenize("test", true));
        CommandContext context = new CommandContext();
        this.testElement.parse(commandSource, args, context);
        Collection<String> elements = context.getAll(this.testKey);
        assertTrue(elements.contains("test1"));
        assertTrue(elements.contains("test2"));
        assertTrue(elements.contains("test123"));
        assertTrue(elements.contains("test124"));
    }

    @Test
    public void testGetAllElementsThatMatchTest1Substring() throws ArgumentParseException {
        CommandSource commandSource = Mockito.mock(CommandSource.class);
        CommandArgs args = new CommandArgs("test12", TOKENIZER.tokenize("test12", true));
        CommandContext context = new CommandContext();
        this.testElement.parse(commandSource, args, context);
        Collection<String> elements = context.getAll(this.testKey);
        assertTrue(elements.contains("test123"));
        assertTrue(elements.contains("test124"));
        assertFalse(elements.contains("test1"));
        assertFalse(elements.contains("test2"));
    }

    @Test
    public void testGibberishThrowsException() {

        assertThrows(
                ArgumentParseException.class,
                () -> {
                    CommandSource commandSource = Mockito.mock(CommandSource.class);
                    CommandArgs args = new CommandArgs("gibberish", TOKENIZER.tokenize("gibberish", true));
                    CommandContext context = new CommandContext();
                    this.testElement.parse(commandSource, args, context);
                }
        );
    }

    public static final class TestCommandElement extends PatternMatchingCommandElement {

        private final List<String> choices = ImmutableList.of(
                "test1",
                "test2",
                "test123",
                "test124"
        );

        protected TestCommandElement(@Nullable String key) {
            super(key, false);
        }

        @Override
        protected Iterable<String> getChoices(CommandSource source) {
            return this.choices;
        }

        @Override
        protected Object getValue(String choice) throws IllegalArgumentException {
            if (this.choices.contains(choice)) {
                return choice;
            }
            throw new IllegalArgumentException("Nope");
        }

    }

}
