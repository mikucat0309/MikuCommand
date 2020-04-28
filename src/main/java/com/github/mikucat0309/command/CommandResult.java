package com.github.mikucat0309.command;

import java.util.Optional;

import javax.annotation.Nullable;

public class CommandResult {

    private static final CommandResult EMPTY = builder().build();
    private static final CommandResult SUCCESS = builder().successCount(1).build();
    private final Optional<Integer> successCount;
    private final Optional<Integer> queryResult;

    CommandResult(@Nullable Integer successCount, @Nullable Integer queryResult) {
        this.successCount = Optional.ofNullable(successCount);
        this.queryResult = Optional.ofNullable(queryResult);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CommandResult empty() {
        return EMPTY;
    }

    public static CommandResult success() {
        return SUCCESS;
    }

    public static CommandResult successCount(int count) {
        return builder().successCount(count).build();
    }

    public static CommandResult queryResult(int count) {
        return builder().queryResult(count).build();
    }

    public Optional<Integer> getSuccessCount() {
        return this.successCount;
    }

    public Optional<Integer> getQueryResult() {
        return this.queryResult;
    }

    public static class Builder {

        @Nullable
        private Integer successCount;
        @Nullable
        private Integer queryResult;

        Builder() {
        }

        public Builder successCount(@Nullable Integer successCount) {
            this.successCount = successCount;
            return this;
        }

        public Builder queryResult(@Nullable Integer queryResult) {
            this.queryResult = queryResult;
            return this;
        }

        public CommandResult build() {
            return new CommandResult(this.successCount, this.queryResult);
        }
    }
}
