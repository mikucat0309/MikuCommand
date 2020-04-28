package com.github.mikucat0309.command.args.parsing;

import com.github.mikucat0309.command.args.ArgumentParseException;

class TokenizerState {

    private final boolean lenient;
    private final String buffer;
    private int index = -1;

    TokenizerState(String buffer, boolean lenient) {
        this.buffer = buffer;
        this.lenient = lenient;
    }

    // Utility methods
    public boolean hasMore() {
        return this.index + 1 < this.buffer.length();
    }

    public int peek() throws ArgumentParseException {
        if (!hasMore()) {
            throw createException("Buffer overrun while parsing args");
        }
        return this.buffer.codePointAt(this.index + 1);
    }

    public int next() throws ArgumentParseException {
        if (!hasMore()) {
            throw createException("Buffer overrun while parsing args");
        }
        return this.buffer.codePointAt(++this.index);
    }

    public ArgumentParseException createException(String message) {
        return new ArgumentParseException(message, this.buffer, this.index);
    }

    public boolean isLenient() {
        return this.lenient;
    }

    public int getIndex() {
        return this.index;
    }
}
