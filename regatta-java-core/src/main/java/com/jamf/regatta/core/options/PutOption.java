/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.options;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The options for put operation.
 */
public final class PutOption {
    public static final PutOption DEFAULT = builder().build();

    private final long leaseId;
    private final boolean prevKV;
    private final long timeout;
    private final TimeUnit timeoutUnit;

    private PutOption(long leaseId, boolean prevKV, long timeout, TimeUnit timeoutUnit) {
        this.leaseId = leaseId;
        this.prevKV = prevKV;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the lease id.
     *
     * @return the lease id
     */
    public long getLeaseId() {
        return this.leaseId;
    }

    /**
     * Get the previous KV.
     *
     * @return the prevKV
     */
    public boolean getPrevKV() {
        return this.prevKV;
    }

    public long getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeoutUnit() {
        return timeoutUnit;
    }

    /**
     * Builder to construct a put option.
     */
    public static final class Builder {

        private long leaseId = 0L;
        private boolean prevKV = false;
        private long timeout = 30;
        private TimeUnit timeoutUnit = TimeUnit.SECONDS;

        private Builder() {
        }

        /**
         * Assign a <i>leaseId</i> for a put operation. Zero means no lease.
         *
         * @param leaseId lease id to apply to a put operation
         * @return builder
         * @throws IllegalArgumentException if lease is less than zero.
         */
        public Builder withLeaseId(long leaseId) {
            checkArgument(leaseId >= 0, "leaseId should greater than or equal to zero: leaseId=%s", leaseId);
            this.leaseId = leaseId;
            return this;
        }

        /**
         * When withPrevKV is set, put response contains previous key-value pair.
         *
         * @return builder
         */
        public Builder withPrevKV() {
            this.prevKV = true;
            return this;
        }

        /**
         * Sets the operation timeout.
         *
         * @param amount timeout value.
         * @param unit   unit for value provided.
         * @return builder
         */
        public Builder withTimeout(long amount, TimeUnit unit) {
            this.timeout = amount;
            this.timeoutUnit = unit;
            return this;
        }

        /**
         * build the put option.
         *
         * @return the put option
         */
        public PutOption build() {
            return new PutOption(this.leaseId, this.prevKV, this.timeout, this.timeoutUnit);
        }

    }
}
