package com.github.mikucat0309.command.args;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class CommandContext {

    public static final String TARGET_BLOCK_ARG = "targetblock-pos048658"; // Random junk afterwards so we don't accidentally conflict with other args

    public static final String TAB_COMPLETION = "tab-complete-50456"; // Random junk afterwards so we don't accidentally conflict with other args

    private final Multimap<String, Object> parsedArgs;

    public CommandContext() {
        this.parsedArgs = ArrayListMultimap.create();
    }

    @SuppressWarnings("unchecked")
    public <T> Collection<T> getAll(String key) {
        return Collections.unmodifiableCollection((Collection<T>) this.parsedArgs.get(key));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOne(String key) {
        var values = this.parsedArgs.get(key);
        if (values.size() != 1) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) values.iterator().next());
    }

    @SuppressWarnings("unchecked")
    public <T> T requireOne(String key)
            throws NoSuchElementException, IllegalArgumentException, ClassCastException {
        var values = this.parsedArgs.get(key);
        if (values.size() == 1) {
            return (T) values.iterator().next();
        } else if (values.isEmpty()) {
            throw new NoSuchElementException();
        }

        throw new IllegalArgumentException();
    }

    public void putArg(String key, Object value) {
        checkNotNull(value, "value");
        this.parsedArgs.put(key, value);
    }

    public boolean hasAny(String key) {
        return this.parsedArgs.containsKey(key);
    }

    public Snapshot createSnapshot() {
        return new Snapshot(this.parsedArgs);
    }

    public void applySnapshot(Snapshot snapshot) {
        this.parsedArgs.clear();
        this.parsedArgs.putAll(snapshot.args);
    }

    public static final class Snapshot {

        final Multimap<String, Object> args;

        Snapshot(Multimap<String, Object> args) {
            this.args = ArrayListMultimap.create(args);
        }

    }
}
