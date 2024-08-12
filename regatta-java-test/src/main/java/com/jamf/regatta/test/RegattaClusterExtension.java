/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.test;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.Network;


/**
 * JUnit5 Extension to have regatta cluster in tests.
 */
public class RegattaClusterExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {

    private static final Map<String, RegattaCluster> CLUSTERS = new ConcurrentHashMap<>();

    private final RegattaCluster cluster;
    private final AtomicBoolean beforeAll;

    private RegattaClusterExtension(RegattaCluster cluster) {
        this.cluster = cluster;
        this.beforeAll = new AtomicBoolean();
    }

    public RegattaCluster cluster() {
        return this.cluster;
    }

    public void restart(long delay, TimeUnit unit) {
        try {
            this.cluster.restart(delay, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String clusterName() {
        return this.cluster.clusterName();
    }

    public List<URI> clientEndpoints() {
        return this.cluster.clientEndpoints();
    }

    public List<URI> peerEndpoints() {
        return this.cluster.peerEndpoints();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        this.beforeAll.set(true);
        before(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        before(context);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        this.beforeAll.set(false);
        after(context);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        after(context);
    }

    protected synchronized void before(ExtensionContext context) {
        RegattaCluster oldCluster = CLUSTERS.putIfAbsent(cluster.clusterName(), cluster);
        if (oldCluster == null) {
            cluster.start();
        }
    }

    protected synchronized void after(ExtensionContext context) {
        if (!this.beforeAll.get()) {
            try {
                cluster.close();
            } finally {
                CLUSTERS.remove(cluster.clusterName());
            }
        }
    }

    public static RegattaCluster cluster(String clusterName) {
        return CLUSTERS.get(clusterName);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Regatta.Builder builder = new Regatta.Builder();

        public Builder withClusterName(String clusterName) {
            builder.withClusterName(clusterName);
            return this;
        }

        public Builder withNodes(int nodes) {
            builder.withNodes(nodes);
            return this;
        }

        public Builder withSsl(boolean ssl) {
            builder.withSsl(ssl);
            return this;
        }

        public Builder withDebug(boolean debug) {
            builder.withDebug(debug);
            return this;
        }

        public Builder withAdditionalArgs(Collection<String> additionalArgs) {
            builder.withAdditionalArgs(additionalArgs);
            return this;
        }

        public Builder withImage(String image) {
            builder.withImage(image);
            return this;
        }

        public Builder withNetwork(Network network) {
            builder.withNetwork(network);
            return this;
        }

        public Builder withMountDirectory(boolean mountDirectory) {
            builder.withMountedDataDirectory(mountDirectory);
            return this;
        }

        public Builder withUser(String user) {
            builder.withUser(user);
            return this;
        }

        public RegattaClusterExtension build() {
            return new RegattaClusterExtension(builder.build());
        }
    }
}
