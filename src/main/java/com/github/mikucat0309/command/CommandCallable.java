package com.github.mikucat0309.command;

import java.util.List;
import java.util.Optional;

public interface CommandCallable {

    CommandResult process(CommandSource source, String arguments) throws CommandException;

    List<String> getSuggestions(CommandSource source, String arguments) throws CommandException;

    Optional<String> getShortDescription(CommandSource source);

    Optional<String> getHelp(CommandSource source);

    String getUsage(CommandSource source);

}
