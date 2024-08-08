/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public record RetryConfig(
        int maxAttempts,
        int delay,
        int maxDelay,
        ChronoUnit unit,
        Duration maxDuration
) {
    public static final RetryConfig DEFAULT = new RetryConfig(4, 50, 1000, ChronoUnit.MILLIS, null);
    public static final RetryConfig NO_RETRY = new RetryConfig(0, 1, 10, ChronoUnit.MILLIS, null);
}
