package com.github.mikucat0309.command.args;


import com.github.mikucat0309.command.args.parsing.SingleArg;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class CommandArgs {

    private final String rawInput;
    private final List<SingleArg> args;
    private int index = -1;

    public CommandArgs(String rawInput, List<SingleArg> args) {
        this.rawInput = rawInput;
        this.args = new ArrayList<>(args);
    }

    public boolean hasNext() {
        return this.index + 1 < this.args.size();
    }

    public String peek() throws ArgumentParseException {
        if (!hasNext()) {
            throw createError("Not enough arguments");
        }
        return this.args.get(this.index + 1).getValue();
    }

    public String next() throws ArgumentParseException {
        if (!hasNext()) {
            throw createError("Not enough arguments!");
        }
        return this.args.get(++this.index).getValue();
    }

    public Optional<String> nextIfPresent() {
        return hasNext() ? Optional.of(this.args.get(++this.index).getValue()) : Optional.empty();
    }

    public ArgumentParseException createError(String message) {
        return new ArgumentParseException(message, this.rawInput, this.index < 0 ? 0 : this.args.get(this.index).getStartIdx());
    }

    public List<String> getAll() {
        return this.args.stream().map(SingleArg::getValue).collect(Collectors.toUnmodifiableList());
    }

    List<SingleArg> getArgs() {
        return this.args;
    }

    @Deprecated
    public Object getState() {
        return getSnapshot();
    }

    @Deprecated
    public void setState(Object state) {
        if (!(state instanceof Snapshot)) {
            throw new IllegalArgumentException("Provided state was not of appropriate format returned by getState!");
        }

        applySnapshot((Snapshot) state, false); // keep parity with before
    }

    public String getRaw() {
        return this.rawInput;
    }

    public String get(int index) {
        return this.args.get(index).getValue();
    }

    public void insertArg(String value) {
        int index = this.index < 0 ? 0 : this.args.get(this.index).getEndIdx();
        this.args.add(this.index + 1, new SingleArg(value, index, index));
    }

    @Deprecated
    public void removeArgs(Object startState, Object endState) {
        if (!(startState instanceof Integer) || !(endState instanceof Integer)) {
            throw new IllegalArgumentException("One of the states provided was not of the correct type!");
        }

        removeArgs((int) startState, (int) endState);
    }

    public void removeArgs(Snapshot startSnapshot, Snapshot endSnapshot) {
        removeArgs(startSnapshot.index, endSnapshot.index);
    }

    private void removeArgs(int startIdx, int endIdx) {
        if (this.index >= startIdx) {
            if (this.index < endIdx) {
                this.index = startIdx - 1;
            } else {
                this.index -= (endIdx - startIdx) + 1;
            }
        }
        if (endIdx >= startIdx) {
            this.args.subList(startIdx, endIdx + 1).clear();
        }
    }

    public int size() {
        return this.args.size();
    }

    void previous() {
        if (this.index > -1) {
            --this.index;
        }
    }

    public int getRawPosition() {
        return this.index < 0 ? 0 : this.args.get(this.index).getStartIdx();
    }

    public Snapshot getSnapshot() {
        return new Snapshot(this.index, this.args);
    }

    public void applySnapshot(Snapshot snapshot) {
        applySnapshot(snapshot, true);
    }

    public void applySnapshot(Snapshot snapshot, boolean resetArgs) {
        this.index = snapshot.index;
        if (resetArgs) {
            this.args.clear();
            this.args.addAll(snapshot.args);
        }
    }

    public static final class Snapshot {

        final int index;
        final ImmutableList<SingleArg> args;

        Snapshot(int index, List<SingleArg> args) {
            this.index = index;
            this.args = ImmutableList.copyOf(args);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            var snapshot = (Snapshot) o;
            return this.index == snapshot.index &&
                    Objects.equals(this.args, snapshot.args);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.index, this.args);
        }
    }
}
