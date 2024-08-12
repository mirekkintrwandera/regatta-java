/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.data.example;

import com.jamf.regatta.core.Client;
import com.jamf.regatta.core.api.ByteSequence;
import com.jamf.regatta.test.RegattaClusterExtension;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.net.ssl.SSLException;

import static org.assertj.core.api.Assertions.assertThat;

class RegattaSpringDataExampleApplicationIntegrationTest {

    @RegisterExtension
    private static final RegattaClusterExtension cluster = RegattaClusterExtension.builder().withNodes(3).build();

    @Test
    void testExample() throws SSLException, InterruptedException {
        Client client = Client.builder()
                .negotiationType(NegotiationType.PLAINTEXT)
                .target("cluster://" + cluster.clusterName())
                .build();
        client.getTablesClient().createTable("test");
        Thread.sleep(30000);
        client.getKVClient().put(ByteSequence.fromUtf8String("test"), ByteSequence.fromUtf8String("test"), ByteSequence.fromUtf8String("test"));
        var res = client.getKVClient().get(ByteSequence.fromUtf8String("test"), ByteSequence.fromUtf8String("test"));
        assertThat(res).isNotNull();
        assertThat(res.count()).isEqualTo(1);
        assertThat(res.kvs().get(0).key()).isEqualTo(ByteSequence.fromUtf8String("test"));
        assertThat(res.kvs().get(0).value()).isEqualTo(ByteSequence.fromUtf8String("test"));
    }

}
