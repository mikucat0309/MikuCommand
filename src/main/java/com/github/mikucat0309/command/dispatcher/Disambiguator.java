package com.github.mikucat0309.command.dispatcher;

import com.github.mikucat0309.command.CommandMapping;
import com.github.mikucat0309.command.CommandSource;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

@FunctionalInterface
public interface Disambiguator {

    Optional<CommandMapping> disambiguate(@Nullable CommandSource source, String aliasUsed, List<CommandMapping> availableOptions);

}
