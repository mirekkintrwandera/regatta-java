/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core;

import com.jamf.regatta.core.api.*;
import com.jamf.regatta.core.options.DeleteOption;
import com.jamf.regatta.core.options.GetOption;
import com.jamf.regatta.core.options.PutOption;
import com.jamf.regatta.core.options.TxnOption;

import java.util.stream.Stream;

public interface KV extends CloseableClient {

    /**
     * put a key-value pair into regatta.
     *
     * @param table table in ByteSequence
     * @param key   key in ByteSequence
     * @param value value in ByteSequence
     * @return PutResponse
     */
    PutResponse put(ByteSequence table, ByteSequence key, ByteSequence value);

    /**
     * put a key-value pair into regatta.
     *
     * @param table table in ByteSequence
     * @param key   key in ByteSequence
     * @param value value in ByteSequence
     * @return PutResponse
     */
    PutResponse put(ByteSequence table, ByteSequence key, ByteSequence value, PutOption option);

    /**
     * retrieve value for the given key.
     *
     * @param table table in ByteSequence
     * @param key   key in ByteSequence
     * @return GetResponse
     */
    GetResponse get(ByteSequence table, ByteSequence key);

    /**
     * retrieve value for the given key.
     *
     * @param table table in ByteSequence
     * @param key   key in ByteSequence
     * @return GetResponse
     */
    GetResponse get(ByteSequence table, ByteSequence key, GetOption option);

    /**
     * retrieve values for the given keys.
     *
     * @param table table in ByteSequence
     * @param key   key in ByteSequence
     * @return Stream of responses
     */
    Stream<GetResponse> iterate(ByteSequence table, ByteSequence key, GetOption option);

    /**
     * delete value with given key.
     *
     * @param table table in ByteSequence
     * @param key   key in ByteSequence
     * @return DeleteResponse
     */
    DeleteResponse delete(ByteSequence table, ByteSequence key);

    /**
     * delete value with given key.
     *
     * @param table table in ByteSequence
     * @param key   key in ByteSequence
     * @return DeleteResponse
     */
    DeleteResponse delete(ByteSequence table, ByteSequence key, DeleteOption option);

    /**
     * creates a transaction.
     *
     * @param table table in ByteSequence
     * @return a Txn
     */
    Txn txn(ByteSequence table);

    /**
     * creates a transaction.
     *
     * @param table table in ByteSequence
     * @return a Txn
     */
    Txn txn(ByteSequence table, TxnOption option);
}
