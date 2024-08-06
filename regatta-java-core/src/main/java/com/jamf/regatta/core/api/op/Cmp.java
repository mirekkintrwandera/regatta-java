/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.api.op;

import com.google.protobuf.ByteString;
import com.jamf.regatta.core.api.ByteSequence;
import com.jamf.regatta.core.api.Txn;
import com.jamf.regatta.proto.Compare;

/**
 * The compare predicate in {@link Txn}.
 */
public class Cmp {

    public enum Op {
        EQUAL, GREATER, LESS, NOT_EQUAL
    }

    private final ByteString key;
    private final Op op;
    private final CmpTarget<?> target;

    public Cmp(ByteSequence key, Op compareOp, CmpTarget<?> target) {
        this.key = ByteString.copyFrom(key.getBytes());
        this.op = compareOp;
        this.target = target;
    }

    protected Compare toCompare() {
        Compare.Builder compareBuilder = Compare.newBuilder().setKey(this.key);
        switch (this.op) {
            case EQUAL:
                compareBuilder.setResult(Compare.CompareResult.EQUAL);
                break;
            case GREATER:
                compareBuilder.setResult(Compare.CompareResult.GREATER);
                break;
            case LESS:
                compareBuilder.setResult(Compare.CompareResult.LESS);
                break;
            case NOT_EQUAL:
                compareBuilder.setResult(Compare.CompareResult.NOT_EQUAL);
                break;
            default:
                throw new IllegalArgumentException("Unexpected compare type (" + this.op + ")");
        }

        Compare.CompareTarget target = this.target.getTarget();
        Object value = this.target.getTargetValue();

        compareBuilder.setTarget(target);
        switch (target) {
            // Eventually add more targets.
            case VALUE:
                compareBuilder.setValue((ByteString) value);
                break;
            default:
                throw new IllegalArgumentException("Unexpected target type (" + target + ")");
        }

        return compareBuilder.build();
    }
}
