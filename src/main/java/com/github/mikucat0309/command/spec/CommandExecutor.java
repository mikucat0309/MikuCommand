package com.github.mikucat0309.command.spec;

import com.github.mikucat0309.command.CommandException;
import com.github.mikucat0309.command.CommandResult;
import com.github.mikucat0309.command.CommandSource;
import com.github.mikucat0309.command.args.CommandContext;

@FunctionalInterface
public interface CommandExecutor {

    CommandResult execute(CommandSource src, CommandContext args) throws CommandException;
}
