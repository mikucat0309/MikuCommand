package com.github.mikucat0309.command.util;

public enum Tristate {
    TRUE(true) {
        @Override
        public Tristate and(Tristate other) {
            return other == TRUE || other == UNDEFINED ? TRUE : FALSE;
        }

        @Override
        public Tristate or(Tristate other) {
            return TRUE;
        }
    },
    FALSE(false) {
        @Override
        public Tristate and(Tristate other) {
            return FALSE;
        }

        @Override
        public Tristate or(Tristate other) {
            return other == TRUE ? TRUE : FALSE;
        }
    },
    UNDEFINED(false) {
        @Override
        public Tristate and(Tristate other) {
            return other;
        }

        @Override
        public Tristate or(Tristate other) {
            return other;
        }
    };

    private final boolean val;

    Tristate(boolean val) {
        this.val = val;
    }

    public static Tristate fromBoolean(boolean val) {
        return val ? TRUE : FALSE;
    }

    public abstract Tristate and(Tristate other);

    public abstract Tristate or(Tristate other);

    public boolean asBoolean() {
        return this.val;
    }
}
