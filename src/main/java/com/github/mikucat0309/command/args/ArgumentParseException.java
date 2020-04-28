package com.github.mikucat0309.command.args;

import com.github.mikucat0309.command.CommandException;
import com.google.common.base.Strings;

public class ArgumentParseException extends CommandException {

    private static final long serialVersionUID = 8689033230447619239L;
    private final String source;
    private final int position;

    public ArgumentParseException(String message, String source, int position) {
        super(message, true);
        this.source = source;
        this.position = position;
    }

    public ArgumentParseException(String message, Throwable cause, String source, int position) {
        super(message, cause, true);
        this.source = source;
        this.position = position;
    }

    @Override
    public String getMessage() {
        var superText = super.getMessage();
        if (this.source.isEmpty()) {
            return super.getMessage();
        } else if (superText == null) {
            return getAnnotatedPosition();
        } else {
            return superText + '\n' + getAnnotatedPosition();
        }
    }

    public String getAnnotatedPosition() {
        var source = this.source;
        int position = this.position;
        if (source.length() > 80) {
            if (position >= 37) {
                int startPos = position - 37;
                int endPos = Math.min(source.length(), position + 37);
                if (endPos < source.length()) {
                    source = "..." + source.substring(startPos, endPos) + "...";
                } else {
                    source = "..." + source.substring(startPos, endPos);
                }
                position -= 40;
            } else {
                source = source.substring(0, 77) + "...";
            }
        }
        return source + "\n" + Strings.repeat(" ", position) + "^";
    }

    public int getPosition() {
        return this.position;
    }

    public String getSourceString() {
        return this.source;
    }

    public static class WithUsage extends ArgumentParseException {

        private static final long serialVersionUID = -8184628779569449691L;
        private final String usage;

        public WithUsage(ArgumentParseException wrapped, String usage) {
            super(wrapped.getMessage(), wrapped.getCause(), wrapped.getSourceString(), wrapped.getPosition());
            this.usage = usage;
        }

        public String getUsage() {
            return this.usage;
        }

    }

}
