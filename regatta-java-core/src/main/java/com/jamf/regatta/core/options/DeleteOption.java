/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.options;

import com.jamf.regatta.core.api.ByteSequence;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class DeleteOption {
    public static final DeleteOption DEFAULT = builder().build();

    private final ByteSequence endKey;
    private final boolean prevKV;
    private final boolean prefix;
    private final long timeout;
    private final TimeUnit timeoutUnit;

    private DeleteOption(ByteSequence endKey, boolean prevKV, boolean prefix, long timeout, TimeUnit timeoutUnit) {
        this.endKey = endKey;
        this.prevKV = prevKV;
        this.prefix = prefix;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<ByteSequence> getEndKey() {
        return Optional.ofNullable(endKey);
    }

    /**
     * Whether to get the previous key/value pairs before deleting them.
     *
     * @return true if get the previous key/value pairs before deleting them, otherwise false.
     */
    public boolean isPrevKV() {
        return prevKV;
    }

    public boolean isPrefix() {
        return prefix;
    }

    public long getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeoutUnit() {
        return timeoutUnit;
    }

    public static final class Builder {
        private ByteSequence endKey;
        private boolean prevKV = false;
        private boolean prefix = false;
        private long timeout = 30;
        private TimeUnit timeoutUnit = TimeUnit.SECONDS;

        private Builder() {
        }

        /**
         * Set the end key of the delete request. If it is set, the delete request will delete the keys
         * from <i>key</i> to <i>endKey</i> (exclusive).
         *
         * <p>
         * If end key is '\0', the range is all keys {@literal >=}
         * key.
         *
         * <p>
         * If the end key is one bit larger than the given key, then it deletes all keys with
         * the prefix (the given key).
         *
         * <p>
         * If both key and end key are '\0', it deletes all keys.
         *
         * @param endKey end key
         * @return builder
         */
        public Builder withRange(ByteSequence endKey) {
            this.endKey = endKey;
            return this;
        }

        /**
         * Enables 'Delete' requests to delete all the keys by prefix.
         *
         * <p>
         *
         * @param prefix flag to delete all the keys by prefix
         * @return builder
         */
        public DeleteOption.Builder isPrefix(boolean prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Get the previous key/value pairs before deleting them.
         *
         * @param prevKV flag to get previous key/value pairs before deleting them.
         * @return builder
         */
        public Builder withPrevKV(boolean prevKV) {
            this.prevKV = prevKV;
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

        public DeleteOption build() {
            return new DeleteOption(this.endKey, this.prevKV, this.prefix, this.timeout, this.timeoutUnit);
        }

    }
}
