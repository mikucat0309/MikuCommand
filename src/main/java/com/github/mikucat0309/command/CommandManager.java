package com.github.mikucat0309.command;

import com.github.mikucat0309.command.dispatcher.Dispatcher;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface CommandManager extends Dispatcher {

    Optional<CommandMapping> register(MetaData metaData, CommandCallable callable, String... alias);

    Optional<CommandMapping> register(MetaData metaData, CommandCallable callable, List<String> aliases);

    Optional<CommandMapping> register(MetaData metaData, CommandCallable callable, List<String> aliases,
            Function<List<String>, List<String>> callback);

    Optional<CommandMapping> removeMapping(CommandMapping mapping);

    int size();

    @Override
    CommandResult process(CommandSource source, String arguments);

    @Override
    List<String> getSuggestions(CommandSource source, String arguments);

}
