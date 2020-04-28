package com.github.mikucat0309.command.args.parsing;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public final class SingleArg {

    private final String value;
    private final int startIdx;
    private final int endIdx;

    public SingleArg(String value, int startIdx, int endIdx) {
        this.value = value;
        this.startIdx = startIdx;
        this.endIdx = endIdx;
    }

    public String getValue() {
        return this.value;
    }

    public int getStartIdx() {
        return this.startIdx;
    }

    public int getEndIdx() {
        return this.endIdx;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SingleArg)) {
            return false;
        }
        var singleArg = (SingleArg) o;
        return this.startIdx == singleArg.startIdx
                && this.endIdx == singleArg.endIdx
                && Objects.equal(this.value, singleArg.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.value, this.startIdx, this.endIdx);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("value", this.value)
                .add("startIdx", this.startIdx)
                .add("endIdx", this.endIdx)
                .toString();
    }
}
