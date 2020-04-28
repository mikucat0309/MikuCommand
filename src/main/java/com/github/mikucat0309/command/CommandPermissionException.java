package com.github.mikucat0309.command;


public class CommandPermissionException extends CommandException {

    private static final long serialVersionUID = -587890218552774105L;

    public CommandPermissionException() {
        this("You do not have permission to use this command!");
    }

    public CommandPermissionException(String message) {
        super(message);
    }

    public CommandPermissionException(String message, Throwable cause) {
        super(message, cause);
    }
}
