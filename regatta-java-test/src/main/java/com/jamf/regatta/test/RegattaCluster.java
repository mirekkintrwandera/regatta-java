/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.test;

import org.testcontainers.lifecycle.Startable;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface RegattaCluster extends Startable {

    default void restart(long delay, TimeUnit unit) throws InterruptedException {
        stop();

        if (delay > 0) {
            unit.sleep(delay);
        }

        start();
    }

    String clusterName();

    List<URI> clientEndpoints();

    List<URI> peerEndpoints();

    List<RegattaContainer> containers();
}
