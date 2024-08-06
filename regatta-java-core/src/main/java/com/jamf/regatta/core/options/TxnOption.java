/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.options;

import java.util.concurrent.TimeUnit;

public class TxnOption {
    public static final TxnOption DEFAULT = builder().build();

    private final long timeout;
    private final TimeUnit timeoutUnit;

    private TxnOption(long timeout, TimeUnit timeoutUnit) {
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    public static TxnOption.Builder builder() {
        return new TxnOption.Builder();
    }

    public long getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeoutUnit() {
        return timeoutUnit;
    }

    /**
     * Builder to construct a txn option.
     */
    public static final class Builder {

        private long timeout = 30;
        private TimeUnit timeoutUnit = TimeUnit.SECONDS;

        private Builder() {
        }

        /**
         * Sets the operation timeout.
         *
         * @param amount timeout value.
         * @param unit   unit for value provided.
         * @return builder
         */
        public TxnOption.Builder withTimeout(long amount, TimeUnit unit) {
            this.timeout = amount;
            this.timeoutUnit = unit;
            return this;
        }

        /**
         * build the txn option.
         *
         * @return the table option
         */
        public TxnOption build() {
            return new TxnOption(this.timeout, this.timeoutUnit);
        }

    }
}
