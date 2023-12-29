/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.options;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The options for put operation.
 */
public final class PutOption {
    public static final PutOption DEFAULT = builder().build();

    private final long leaseId;
    private final boolean prevKV;

    private PutOption(long leaseId, boolean prevKV) {
        this.leaseId = leaseId;
        this.prevKV = prevKV;
    }

    /**
     * Returns the builder.
     *
     * @return the builder
     * @deprecated use {@link #builder()}
     */
    @SuppressWarnings("InlineMeSuggester")
    @Deprecated
    public static Builder newBuilder() {
        return builder();
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

    /**
     * Builder to construct a put option.
     */
    public static final class Builder {

        private long leaseId = 0L;
        private boolean prevKV = false;

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
         * build the put option.
         *
         * @return the put option
         */
        public PutOption build() {
            return new PutOption(this.leaseId, this.prevKV);
        }

    }
}
