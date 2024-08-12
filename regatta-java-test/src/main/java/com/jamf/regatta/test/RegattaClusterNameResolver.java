/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.test;

import com.google.auto.service.AutoService;
import com.google.common.base.Preconditions;
import io.grpc.*;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.SharedResourceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

public class RegattaClusterNameResolver extends NameResolver {
    public static final String SCHEME = "cluster";
    private static final Logger LOGGER = LoggerFactory.getLogger(RegattaClusterNameResolver.class);

    private final Object lock;
    private final String authority;
    private final URI targetUri;

    private volatile boolean shutdown;
    private volatile boolean resolving;

    private Executor executor;
    private Listener listener;

    public RegattaClusterNameResolver(URI targetUri) {
        this.lock = new Object();
        this.targetUri = targetUri;
        this.authority = targetUri.getAuthority();
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
            if (authority == null) {
                throw new RuntimeException("Unable to resolve endpoint " + targetUri);
            }

            RegattaCluster cluster = RegattaClusterExtension.cluster(authority);
            if (cluster == null) {
                throw new RuntimeException("Unable to find cluster " + authority);
            }

            List<EquivalentAddressGroup> servers = new ArrayList<>();

            for (RegattaContainer container : cluster.containers()) {
                try {
                    EquivalentAddressGroup ag = new EquivalentAddressGroup(
                            container.getClientAddress(),
                            Attributes.newBuilder()
                                    .set(EquivalentAddressGroup.ATTR_AUTHORITY_OVERRIDE, container.node())
                                    .build());

                    servers.add(ag);
                } catch (IllegalStateException | IllegalArgumentException e) {
                    LOGGER.debug(
                            "Failure computing AddressGroup for cluster {}, {}",
                            cluster.clusterName(),
                            e.getMessage());
                }
            }

            if (!servers.isEmpty()) {
                savedListener.onAddresses(servers, Attributes.EMPTY);
            }

        } catch (Exception e) {
            LOGGER.warn("Error while getting list of servers", e);
            savedListener.onError(Status.NOT_FOUND);
        } finally {
            resolving = false;
        }
    }

    @AutoService(NameResolverProvider.class)
    public static class RegattaClusterResolverProvider extends NameResolverProvider {
        @Override
        protected boolean isAvailable() {
            return true;
        }

        @Override
        protected int priority() {
            return 6;
        }

        @Override
        public String getDefaultScheme() {
            return RegattaClusterNameResolver.SCHEME;
        }

        @Override
        public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
            return RegattaClusterNameResolver.SCHEME.equals(targetUri.getScheme())
                    ? new RegattaClusterNameResolver(targetUri)
                    : null;
        }
    }
}
