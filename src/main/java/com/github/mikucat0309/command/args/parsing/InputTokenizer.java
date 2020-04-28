package com.github.mikucat0309.command.args.parsing;

import com.github.mikucat0309.command.args.ArgumentParseException;

import java.util.List;

public interface InputTokenizer {

    static InputTokenizer quotedStrings(boolean forceLenient) {
        return new QuotedStringTokenizer(true, forceLenient, false);
    }

    static InputTokenizer spaceSplitString() {
        return SpaceSplitInputTokenizer.INSTANCE;
    }

    static InputTokenizer rawInput() {
        return RawStringInputTokenizer.INSTANCE;
    }

    List<SingleArg> tokenize(String arguments, boolean lenient) throws ArgumentParseException;

}
