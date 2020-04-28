package com.github.mikucat0309.command.dispatcher;

import com.github.mikucat0309.command.CommandCallable;
import com.github.mikucat0309.command.CommandMapping;
import com.github.mikucat0309.command.CommandSource;
import com.google.common.collect.Multimap;

import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

public interface Dispatcher extends CommandCallable {

    Set<? extends CommandMapping> getCommands();

    Set<String> getPrimaryAliases();

    Set<String> getAliases();

    Optional<? extends CommandMapping> get(String alias);

    Optional<? extends CommandMapping> get(String alias, @Nullable CommandSource source);

    Set<? extends CommandMapping> getAll(String alias);

    Multimap<String, CommandMapping> getAll();

    boolean containsAlias(String alias);

    boolean containsMapping(CommandMapping mapping);
}
