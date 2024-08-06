/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.impl;

import com.google.protobuf.ByteString;
import com.jamf.regatta.core.KV;
import com.jamf.regatta.core.api.KeyValue;
import com.jamf.regatta.core.api.PutResponse;
import com.jamf.regatta.core.api.Txn;
import com.jamf.regatta.core.api.TxnResponse;
import com.jamf.regatta.core.api.*;
import com.jamf.regatta.core.api.op.TxnImpl;
import com.jamf.regatta.core.encoding.SnappyCodec;
import com.jamf.regatta.core.options.*;
import com.jamf.regatta.proto.*;
import io.grpc.Channel;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class KVImpl implements KV {

    private final KVGrpc.KVBlockingStub stub;

    KVImpl(Channel managedChannel) {
        stub = KVGrpc.newBlockingStub(managedChannel).withCompression(SnappyCodec.NAME);
    }

    @Override
    public PutResponse put(ByteSequence table, ByteSequence key, ByteSequence value) {
        return put(table, key, value, PutOption.DEFAULT);
    }

    @Override
    public PutResponse put(ByteSequence table, ByteSequence key, ByteSequence value, PutOption option) {
        var request = PutRequest.newBuilder()
                .setTable(ByteString.copyFrom(table.getBytes()))
                .setKey(ByteString.copyFrom(key.getBytes()))
                .setValue(ByteString.copyFrom(value.getBytes()))
                .build();
        var put = stub.withDeadlineAfter(option.getTimeout(), option.getTimeoutUnit()).put(request);
        return new PutResponse(new Response.HeaderImpl(put.getHeader()), new KeyValue(ByteSequence.from(put.getPrevKv().getKey()), ByteSequence.from(put.getPrevKv().getValue())));
    }

    @Override
    public GetResponse get(ByteSequence table, ByteSequence key) {
        return get(table, key, GetOption.DEFAULT);
    }

    @Override
    public GetResponse get(ByteSequence table, ByteSequence key, GetOption option) {
        var request = RangeRequest.newBuilder()
                .setTable(ByteString.copyFrom(table.getBytes()))
                .setKey(ByteString.copyFrom(key.getBytes()))
                .setRangeEnd(option.getEndKey()
                        .map(byteSequence -> ByteString.copyFrom(byteSequence.getBytes()))
                        .orElseGet(() -> option.isPrefix() ? ByteString.copyFrom(OptionsUtil.prefixEndOf(key).getBytes()) : ByteString.EMPTY)
                )
                .setLimit(option.getLimit())
                .setCountOnly(option.isCountOnly())
                .setKeysOnly(option.isKeysOnly())
                .setLinearizable(!option.isSerializable())
                .build();

        var get = stub.withDeadlineAfter(option.getTimeout(), option.getTimeoutUnit()).range(request);
        var kvs = get.getKvsList().stream().map(keyValue -> new KeyValue(ByteSequence.from(keyValue.getKey()), ByteSequence.from(keyValue.getValue()))).toList();
        return new GetResponse(new Response.HeaderImpl(get.getHeader()), kvs, get.getCount());
    }

    @Override
    public Stream<GetResponse> iterate(ByteSequence table, ByteSequence key) {
        return iterate(table, key, GetOption.DEFAULT);
    }

    @Override
    public Stream<GetResponse> iterate(ByteSequence table, ByteSequence key, GetOption option) {
        var request = RangeRequest.newBuilder()
                .setTable(ByteString.copyFrom(table.getBytes()))
                .setKey(ByteString.copyFrom(key.getBytes()))
                .setRangeEnd(option.getEndKey()
                        .map(byteSequence -> ByteString.copyFrom(byteSequence.getBytes()))
                        .orElseGet(() -> option.isPrefix() ? ByteString.copyFrom(OptionsUtil.prefixEndOf(key).getBytes()) : ByteString.EMPTY)
                )
                .setLimit(option.getLimit())
                .setCountOnly(option.isCountOnly())
                .setKeysOnly(option.isKeysOnly())
                .setLinearizable(!option.isSerializable())
                .build();
        return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(stub.withDeadlineAfter(option.getTimeout(), option.getTimeoutUnit()).iterateRange(request), Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE),
                        false)
                .map(get -> new GetResponse(
                        new Response.HeaderImpl(get.getHeader()),
                        get.getKvsList().stream().map(keyValue -> new KeyValue(ByteSequence.from(keyValue.getKey()), ByteSequence.from(keyValue.getValue()))).toList(),
                        get.getCount()
                ));
    }

    @Override
    public DeleteResponse delete(ByteSequence table, ByteSequence key) {
        return delete(table, key, DeleteOption.DEFAULT);
    }

    @Override
    public DeleteResponse delete(ByteSequence table, ByteSequence key, DeleteOption option) {
        var request = DeleteRangeRequest.newBuilder()
                .setTable(ByteString.copyFrom(table.getBytes()))
                .setKey(ByteString.copyFrom(key.getBytes()))
                .setRangeEnd(option.getEndKey()
                        .map(byteSequence -> ByteString.copyFrom(byteSequence.getBytes()))
                        .orElseGet(() -> option.isPrefix() ? ByteString.copyFrom(OptionsUtil.prefixEndOf(key).getBytes()) : ByteString.EMPTY)
                )
                .setPrevKv(option.isPrevKV())
                .build();

        var delete = stub.withDeadlineAfter(option.getTimeout(), option.getTimeoutUnit()).deleteRange(request);
        var kvs = delete.getPrevKvsList().stream().map(keyValue -> new KeyValue(ByteSequence.from(keyValue.getKey()), ByteSequence.from(keyValue.getValue()))).toList();
        return new DeleteResponse(new Response.HeaderImpl(delete.getHeader()), kvs, delete.getDeleted());
    }

    @Override
    public Txn txn(ByteSequence table) {
        return txn(table, TxnOption.DEFAULT);
    }

    @Override
    public Txn txn(ByteSequence table, TxnOption option) {
        return TxnImpl.newTxn(request -> this.txn(request, option), table);
    }

    private TxnResponse txn(TxnRequest request, TxnOption option) {
        var txnResult = stub.withDeadlineAfter(option.getTimeout(), option.getTimeoutUnit()).txn(request);
        return new TxnResponse(new Response.HeaderImpl(txnResult.getHeader()), txnResult.getSucceeded(), txnResult.getResponsesList());
    }
}
