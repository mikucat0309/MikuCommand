package com.github.mikucat0309.command.args.parsing;

import java.util.Collections;
import java.util.List;

class RawStringInputTokenizer implements InputTokenizer {

    static final RawStringInputTokenizer INSTANCE = new RawStringInputTokenizer();

    private RawStringInputTokenizer() {
    }

    @Override
    public List<SingleArg> tokenize(String arguments, boolean lenient) {
        return Collections.singletonList(new SingleArg(arguments, 0, arguments.length()));
    }
}
