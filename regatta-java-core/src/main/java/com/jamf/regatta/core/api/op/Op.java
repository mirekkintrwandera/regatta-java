/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.api.op;

import com.google.protobuf.ByteString;
import com.jamf.regatta.core.api.ByteSequence;
import com.jamf.regatta.core.options.DeleteOption;
import com.jamf.regatta.core.options.GetOption;
import com.jamf.regatta.core.options.PutOption;
import com.jamf.regatta.proto.RequestOp;

/**
 * Etcd Operation.
 */
public abstract class Op {

    /**
     * Operation type.
     */
    public enum Type {
        PUT, RANGE, DELETE_RANGE
    }

    protected final Type type;
    protected final ByteString key;

    protected Op(Type type, ByteString key) {
        this.type = type;
        this.key = key;
    }

    abstract RequestOp toRequestOp();

    public static PutOp put(ByteSequence key, ByteSequence value, PutOption option) {
        return new PutOp(ByteString.copyFrom(key.getBytes()), ByteString.copyFrom(value.getBytes()), option);
    }

    public static GetOp get(ByteSequence key, GetOption option) {
        return new GetOp(ByteString.copyFrom(key.getBytes()), option);
    }

    public static DeleteOp delete(ByteSequence key, DeleteOption option) {
        return new DeleteOp(ByteString.copyFrom(key.getBytes()), option);
    }

    public static final class PutOp extends Op {

        private final ByteString value;
        private final PutOption option;

        private PutOp(ByteString key, ByteString value, PutOption option) {
            super(Type.PUT, key);
            this.value = value;
            this.option = option;
        }

        @Override
        RequestOp toRequestOp() {
            return RequestOp.newBuilder()
                    .setRequestPut(mapPutRequest(key, value, option)).build();
        }
    }

    public static final class GetOp extends Op {

        private final GetOption option;

        private GetOp(ByteString key, GetOption option) {
            super(Type.RANGE, key);
            this.option = option;
        }

        @Override
        RequestOp toRequestOp() {
            return RequestOp.newBuilder()
                    .setRequestRange(mapRangeRequest(key, option))
                    .build();
        }
    }

    public static final class DeleteOp extends Op {

        private final DeleteOption option;

        DeleteOp(ByteString key, DeleteOption option) {
            super(Type.DELETE_RANGE, key);
            this.option = option;
        }

        @Override
        RequestOp toRequestOp() {
            return RequestOp.newBuilder()
                    .setRequestDeleteRange(mapDeleteRequest(key, option))
                    .build();
        }
    }


    public static RequestOp.Range mapRangeRequest(ByteString key, GetOption option) {
        return RequestOp.Range.newBuilder()
                .setKey(key)
                .setCountOnly(option.isCountOnly())
                .setLimit(option.getLimit())
                .setKeysOnly(option.isKeysOnly())
                .build();

    }

    public static RequestOp.Put mapPutRequest(ByteString key, ByteString value, PutOption option) {
        return RequestOp.Put.newBuilder()
                .setKey(key)
                .setValue(value)
                .setPrevKv(option.getPrevKV())
                .build();
    }

    public static RequestOp.DeleteRange mapDeleteRequest(ByteString key, DeleteOption option) {
        return RequestOp.DeleteRange.newBuilder()
                .setKey(key)
                .setPrevKv(option.isPrevKV())
                .build();
    }
}
