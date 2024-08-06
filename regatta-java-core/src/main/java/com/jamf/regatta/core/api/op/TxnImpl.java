/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.api.op;

import com.google.protobuf.ByteString;
import com.jamf.regatta.core.api.ByteSequence;
import com.jamf.regatta.core.api.Txn;
import com.jamf.regatta.core.api.TxnResponse;
import com.jamf.regatta.proto.TxnRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class TxnImpl implements Txn {
    public static Txn newTxn(Function<TxnRequest, TxnResponse> f, ByteSequence table) {
        return new TxnImpl(f, table);
    }

    public TxnImpl(Function<TxnRequest, TxnResponse> f, ByteSequence table) {
        this.table = table;
        this.requestF = f;
    }

    private final Function<TxnRequest, TxnResponse> requestF;
    private final ByteSequence table;
    private final List<Cmp> cmpList = new ArrayList<>();
    private final List<Op> successOpList = new ArrayList<>();
    private final List<Op> failureOpList = new ArrayList<>();

    @Override
    public TxnImpl If(Cmp... cmps) {
        return If(Arrays.asList(cmps));
    }

    TxnImpl If(List<Cmp> cmps) {
        cmpList.addAll(cmps);
        return this;
    }

    @Override
    public TxnImpl Then(Op... ops) {
        return Then(Arrays.asList(ops));
    }

    TxnImpl Then(List<Op> ops) {
        successOpList.addAll(ops);
        return this;
    }

    @Override
    public TxnImpl Else(Op... ops) {
        return Else(Arrays.asList(ops));
    }

    TxnImpl Else(List<Op> ops) {
        failureOpList.addAll(ops);
        return this;
    }

    @Override
    public TxnResponse commit() {
        return this.requestF.apply(this.toTxnRequest());
    }

    private TxnRequest toTxnRequest() {
        TxnRequest.Builder requestBuilder = TxnRequest.newBuilder();

        requestBuilder.setTable(ByteString.copyFrom(this.table.getBytes()));

        for (Cmp c : this.cmpList) {
            requestBuilder.addCompare(c.toCompare());
        }

        for (Op o : this.successOpList) {
            requestBuilder.addSuccess(o.toRequestOp());
        }

        for (Op o : this.failureOpList) {
            requestBuilder.addFailure(o.toRequestOp());
        }

        return requestBuilder.build();
    }
}
