/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.resolver;

import com.google.common.base.Preconditions;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.SharedResourceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

public abstract class AbstractNameResolver extends NameResolver {
    public static final int REGATTA_CLIENT_PORT = 8443;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNameResolver.class);

    private final Object lock;
    private final String authority;
    private final URI targetUri;

    private volatile boolean shutdown;
    private volatile boolean resolving;

    private Executor executor;
    private Listener listener;

    public AbstractNameResolver(URI targetUri) {
        this.lock = new Object();
        this.targetUri = targetUri;
        this.authority = MoreObjects.fisrtNonNull(targetUri.getAuthority(), "");
    }

    public URI getTargetUri() {
        return targetUri;
    }

    @Override
    public String getServiceAuthority() {
        return authority;
    }

    @Override
    public void start(Listener listener) {
        synchronized (lock) {
            Preconditions.checkState(this.listener == null, "already started");
            this.executor = SharedResourceHolder.get(GrpcUtil.SHARED_CHANNEL_EXECUTOR);
            this.listener = Objects.requireNonNull(listener, "listener");
            resolve();
        }
    }

    @Override
    public final synchronized void refresh() {
        resolve();
    }

    @Override
    public void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;

        synchronized (lock) {
            if (executor != null) {
                executor = SharedResourceHolder.release(GrpcUtil.SHARED_CHANNEL_EXECUTOR, executor);
            }
        }
    }

    private void resolve() {
        if (resolving || shutdown) {
            return;
        }
        synchronized (lock) {
            executor.execute(this::doResolve);
        }
    }

    private void doResolve() {
        Listener savedListener;
        synchronized (lock) {
            if (shutdown) {
                return;
            }
            resolving = true;
            savedListener = listener;
        }

        try {
            List<EquivalentAddressGroup> groups = computeAddressGroups();
            if (groups.isEmpty()) {
                throw Status.INVALID_ARGUMENT.withDescription("Unable to resolve endpoint " + targetUri).asRuntimeException();
            }

            savedListener.onAddresses(groups, Attributes.EMPTY);

        } catch (Exception e) {
            LOGGER.warn("Error while getting list of servers", e);
            savedListener.onError(Status.NOT_FOUND);
        } finally {
            resolving = false;
        }
    }

    protected abstract List<EquivalentAddressGroup> computeAddressGroups();
}
