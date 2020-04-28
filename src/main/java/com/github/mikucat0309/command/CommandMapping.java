package com.github.mikucat0309.command;

import java.util.Set;

public interface CommandMapping {

    String getPrimaryAlias();

    Set<String> getAllAliases();

    CommandCallable getCallable();

}
