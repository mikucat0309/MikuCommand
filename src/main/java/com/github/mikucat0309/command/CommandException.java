package com.github.mikucat0309.command;


public class CommandException extends Exception {


    private static final long serialVersionUID = -985643118515829870L;
    private final boolean includeUsage;

    public CommandException(String message) {
        this(message, false);
    }

    public CommandException(String message, Throwable cause) {
        this(message, cause, false);
    }

    public CommandException(String message, boolean includeUsage) {
        super(message);
        this.includeUsage = includeUsage;
    }

    public CommandException(String message, Throwable cause, boolean includeUsage) {
        super(message, cause);
        this.includeUsage = includeUsage;
    }

    public boolean shouldIncludeUsage() {
        return this.includeUsage;
    }
}
