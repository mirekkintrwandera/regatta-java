/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.impl;

import com.jamf.regatta.core.Errors;
import com.jamf.regatta.core.RetryConfig;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import dev.failsafe.RetryPolicyBuilder;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class Impl {

    static final Predicate<Status> RETRY_NEVER = status -> false;
    static final Predicate<Status> RETRY_ALWAYS = status -> true;
    static final Predicate<Status> RETRY_TRANSIENT = Errors::isRetryable;

    private final Logger logger;
    private final RetryConfig retryCfg;

    Impl(RetryConfig retryCfg) {
        this.retryCfg = retryCfg;
        this.logger = LoggerFactory.getLogger(getClass());
    }

    /**
     * execute the task and retry it in case of failure.
     *
     * @param supplier      a function that returns a new Future.
     * @param resultConvert a function that converts Type S to Type T.
     * @param <S>           Source type
     * @param <T>           Converted Type.
     * @return a CompletableFuture with type T.
     */
    protected <S, T> T execute(
            Supplier<S> supplier,
            Function<S, T> resultConvert) {

        return execute(supplier, resultConvert, Errors::isRetryable);
    }

    /**
     * execute the task and retry it in case of failure.
     *
     * @param supplier      a function that returns a new Future.
     * @param resultConvert a function that converts Type S to Type T.
     * @param doRetry       a predicate to determine if a failure has to be retried
     * @param <S>           Source type
     * @param <T>           Converted Type.
     * @return a CompletableFuture with type T.
     */
    protected <S, T> T execute(Supplier<S> supplier, Function<S, T> resultConvert, Predicate<Status> doRetry) {
        return resultConvert.apply(Failsafe.with(retryPolicy(doRetry)).get(supplier::get));
    }

    protected <S> RetryPolicy<S> retryPolicy(Predicate<Status> doRetry) {
        RetryPolicyBuilder<S> policy = RetryPolicy.<S>builder()
                .onFailure(e -> {
                    logger.warn("retry failure (attempt: {}, error: {})",
                            e.getAttemptCount(),
                            e.getException() != null ? e.getException().getMessage() : "<none>");
                })
                .onRetry(e -> {
                    logger.debug("retry (attempt: {}, error: {})",
                            e.getAttemptCount(),
                            e.getLastException() != null ? e.getLastException().getMessage() : "<none>");
                })
                .onRetriesExceeded(e -> {
                    logger.warn("maximum number of auto retries reached (attempt: {}, error: {})",
                            e.getAttemptCount(),
                            e.getException() != null ? e.getException().getMessage() : "<none>");
                })
                .handleIf(throwable -> {
                    Status status = Status.fromThrowable(throwable);
                    return doRetry.test(status);
                })
                .withMaxRetries(retryCfg.maxAttempts())
                .withBackoff(
                        retryCfg.delay(),
                        retryCfg.maxDelay(),
                        retryCfg.unit());

        if (retryCfg.maxDuration() != null) {
            policy = policy.withMaxDuration(retryCfg.maxDuration());
        }

        return policy.build();
    }


}
