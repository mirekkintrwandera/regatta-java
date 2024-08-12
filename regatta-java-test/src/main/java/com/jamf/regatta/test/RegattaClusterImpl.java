/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.test;

import org.testcontainers.containers.Network;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class RegattaClusterImpl implements RegattaCluster {
    private final List<RegattaContainer> containers;
    private final String clusterName;
    private final List<String> endpoints;

    public RegattaClusterImpl(
            String image,
            String clusterName,
            int nodes,
            boolean ssl,
            boolean debug,
            Collection<String> additionalArgs,
            Network network,
            boolean shouldMountDataDirectory,
            String user) {

        this.clusterName = clusterName;
        this.endpoints = IntStream.range(1, nodes + 1)
                .mapToObj(i -> "regatta-" + i)
                .collect(toList());
        this.containers = endpoints.stream()
                .map(e -> new RegattaContainer(image, e, endpoints)
                        .withClusterName(clusterName)
                        .withSsl(ssl)
                        .withAdditionalArgs(additionalArgs)
                        .withNetwork(network)
                        .withShouldMountDataDirectory(shouldMountDataDirectory)
                        .withUser(user))
                .collect(toList());
    }

    @Override
    public void start() {
        final CountDownLatch latch = new CountDownLatch(containers.size());
        final AtomicReference<Exception> failedToStart = new AtomicReference<>();

        for (RegattaContainer container : containers) {
            new Thread(() -> {
                try {
                    container.start();
                    Thread.sleep(5000);
                } catch (Exception e) {
                    failedToStart.set(e);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        try {
            latch.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (failedToStart.get() != null) {
            throw new IllegalStateException("Cluster failed to start", failedToStart.get());
        }
    }

    @Override
    public void stop() {
        for (RegattaContainer container : containers) {
            container.stop();
        }
    }

    @Override
    public void close() {
        for (RegattaContainer container : containers) {
            container.close();
        }
    }

    @Override
    public String clusterName() {
        return clusterName;
    }

    @Override
    public List<URI> clientEndpoints() {
        return containers.stream().map(RegattaContainer::clientEndpoint).collect(toList());
    }

    @Override
    public List<URI> peerEndpoints() {
        return containers.stream().map(RegattaContainer::peerEndpoint).collect(toList());
    }

    @Override
    public List<RegattaContainer> containers() {
        return Collections.unmodifiableList(containers);
    }
}
