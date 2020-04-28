package com.github.mikucat0309.command.util;

import java.util.function.Predicate;

import javax.annotation.Nullable;

/**
 * Predicate that determines if the input string starts with the provided
 * test string, case-insensitively.
 */
public final class StartsWithPredicate implements Predicate<String> {

    private final String test;

    /**
     * Create an new predicate.
     *
     * @param test The string to test input against
     */
    public StartsWithPredicate(String test) {
        this.test = test;
    }

    @Override
    public boolean test(@Nullable String input) {
        return input != null && input.toLowerCase().startsWith(this.test.toLowerCase());
    }
}
