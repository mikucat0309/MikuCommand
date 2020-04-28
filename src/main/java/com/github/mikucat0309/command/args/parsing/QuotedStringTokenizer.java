package com.github.mikucat0309.command.args.parsing;

import com.github.mikucat0309.command.args.ArgumentParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class QuotedStringTokenizer implements InputTokenizer {

    private static final int CHAR_BACKSLASH = '\\';
    private static final int CHAR_SINGLE_QUOTE = '\'';
    private static final int CHAR_DOUBLE_QUOTE = '"';
    private final boolean handleQuotedStrings;
    private final boolean forceLenient;
    private final boolean trimTrailingSpace;

    QuotedStringTokenizer(boolean handleQuotedStrings, boolean forceLenient, boolean trimTrailingSpace) {
        this.handleQuotedStrings = handleQuotedStrings;
        this.forceLenient = forceLenient;
        this.trimTrailingSpace = trimTrailingSpace;
    }

    @Override
    public List<SingleArg> tokenize(String arguments, boolean lenient) throws ArgumentParseException {
        if (arguments.length() == 0) {
            return Collections.emptyList();
        }

        var state = new TokenizerState(arguments, lenient);
        var returnedArgs = new ArrayList<SingleArg>(arguments.length() / 4);
        if (this.trimTrailingSpace) {
            skipWhiteSpace(state);
        }
        while (state.hasMore()) {
            if (!this.trimTrailingSpace) {
                skipWhiteSpace(state);
            }
            int startIdx = state.getIndex() + 1;
            var arg = nextArg(state);
            returnedArgs.add(new SingleArg(arg, startIdx, state.getIndex()));
            if (this.trimTrailingSpace) {
                skipWhiteSpace(state);
            }
        }
        return returnedArgs;
    }

    // Parsing methods

    private void skipWhiteSpace(TokenizerState state) throws ArgumentParseException {
        if (!state.hasMore()) {
            return;
        }
        while (state.hasMore() && Character.isWhitespace(state.peek())) {
            state.next();
        }
    }

    private String nextArg(TokenizerState state) throws ArgumentParseException {
        var argBuilder = new StringBuilder();
        if (state.hasMore()) {
            int codePoint = state.peek();
            if (this.handleQuotedStrings && (codePoint == CHAR_DOUBLE_QUOTE || codePoint == CHAR_SINGLE_QUOTE)) {
                // quoted string
                parseQuotedString(state, codePoint, argBuilder);
            } else {
                parseUnquotedString(state, argBuilder);
            }
        }
        return argBuilder.toString();
    }

    private void parseQuotedString(TokenizerState state, int startQuotation, StringBuilder builder) throws ArgumentParseException {
        // Consume the start quotation character
        int nextCodePoint = state.next();
        if (nextCodePoint != startQuotation) {
            throw state.createException(String.format("Actual next character '%c' did not match expected quotation character '%c'",
                    nextCodePoint, startQuotation));
        }

        while (true) {
            if (!state.hasMore()) {
                if (state.isLenient() || this.forceLenient) {
                    return;
                }
                throw state.createException("Unterminated quoted string found");
            }
            nextCodePoint = state.peek();
            if (nextCodePoint == startQuotation) {
                state.next();
                return;
            } else if (nextCodePoint == CHAR_BACKSLASH) {
                parseEscape(state, builder);
            } else {
                builder.appendCodePoint(state.next());
            }
        }
    }

    private void parseUnquotedString(TokenizerState state, StringBuilder builder) throws ArgumentParseException {
        while (state.hasMore()) {
            int nextCodePoint = state.peek();
            if (Character.isWhitespace(nextCodePoint)) {
                return;
            } else if (nextCodePoint == CHAR_BACKSLASH) {
                parseEscape(state, builder);
            } else {
                builder.appendCodePoint(state.next());
            }
        }
    }

    private void parseEscape(TokenizerState state, StringBuilder builder) throws ArgumentParseException {
        state.next(); // Consume \
        builder.appendCodePoint(state.next()); // TODO: Unicode character escapes (\u00A7 type thing)?
    }

}
