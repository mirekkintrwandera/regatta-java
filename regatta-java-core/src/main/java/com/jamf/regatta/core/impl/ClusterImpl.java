/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.impl;

import com.jamf.regatta.core.Cluster;
import com.jamf.regatta.core.RetryConfig;
import com.jamf.regatta.core.api.Member;
import com.jamf.regatta.core.api.MemberListResponse;
import com.jamf.regatta.proto.ClusterGrpc;
import com.jamf.regatta.proto.MemberListRequest;
import io.grpc.Channel;

public class ClusterImpl extends Impl implements Cluster {

    private final ClusterGrpc.ClusterBlockingStub stub;

    ClusterImpl(Channel managedChannel, RetryConfig retryConfig) {
        super(retryConfig);
        stub = ClusterGrpc.newBlockingStub(managedChannel);
    }

    @Override
    public MemberListResponse memberList() {
        return execute(
                () -> stub.memberList(MemberListRequest.getDefaultInstance()),
                ml -> new MemberListResponse(
                        ml.getCluster(),
                        ml.getMembersList().stream()
                                .map(member -> new Member(
                                        member.getId(),
                                        member.getName(),
                                        member.getPeerURLsList().stream().toList(),
                                        member.getClientURLsList().stream().toList()
                                ))
                                .toList()),
                RETRY_TRANSIENT
        );
    }

}
