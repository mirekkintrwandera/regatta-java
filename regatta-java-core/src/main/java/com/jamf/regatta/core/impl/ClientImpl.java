/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.impl;

import com.jamf.regatta.core.Client;
import com.jamf.regatta.core.Cluster;
import com.jamf.regatta.core.KV;
import io.grpc.Channel;

public final class ClientImpl implements Client {

    private final KV kvClient;
    private final Cluster clusterClient;

    public ClientImpl(Channel channel) {
        this.kvClient = new KVImpl(channel);
        this.clusterClient = new ClusterImpl(channel);
    }

    @Override
    public KV getKVClient() {
        return kvClient;
    }

    @Override
    public Cluster getClusterClient() {
        return clusterClient;
    }

    @Override
    public void close() throws Exception {
        kvClient.close();
    }
}
