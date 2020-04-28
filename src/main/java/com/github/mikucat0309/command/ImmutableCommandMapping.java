package com.github.mikucat0309.command;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ImmutableCommandMapping implements CommandMapping {

    private final String primary;
    private final Set<String> aliases;
    private final CommandCallable callable;

    public ImmutableCommandMapping(CommandCallable callable, String primary, String... alias) {
        this(callable, primary, Arrays.asList(checkNotNull(alias, "alias")));
    }

    public ImmutableCommandMapping(CommandCallable callable, String primary, Collection<String> aliases) {
        checkNotNull(primary, "primary");
        checkNotNull(aliases, "aliases");
        this.primary = primary;
        this.aliases = new HashSet<>(aliases);
        this.aliases.add(primary);
        this.callable = checkNotNull(callable, "callable");
    }

    @Override
    public String getPrimaryAlias() {
        return this.primary;
    }

    @Override
    public Set<String> getAllAliases() {
        return Collections.unmodifiableSet(this.aliases);
    }

    @Override
    public CommandCallable getCallable() {
        return this.callable;
    }

    @Override
    public String toString() {
        return "ImmutableCommandMapping{"
                + "primary='" + this.primary + '\''
                + ", aliases=" + this.aliases
                + ", spec=" + this.callable
                + '}';
    }
}
