package com.github.mikucat0309.command;


import com.google.common.base.Preconditions;

public class CommandNotFoundException extends CommandException {


    private static final long serialVersionUID = -7737592851768876427L;
    private final String command;

    public CommandNotFoundException(String command) {
        this("No such command", command);
    }

    public CommandNotFoundException(String message, String command) {
        super(message);
        this.command = Preconditions.checkNotNull(command, "command");
    }

    public String getCommand() {
        return this.command;
    }
}
