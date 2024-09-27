/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.data.core;

import com.jamf.regatta.core.Client;
import com.jamf.regatta.core.KV;
import com.jamf.regatta.core.api.ByteSequence;
import com.jamf.regatta.core.api.GetResponse;
import com.jamf.regatta.core.api.KeyValue;
import com.jamf.regatta.core.api.Txn;
import com.jamf.regatta.core.api.op.Cmp;
import com.jamf.regatta.core.api.op.CmpTarget;
import com.jamf.regatta.core.api.op.Op;
import com.jamf.regatta.core.options.DeleteOption;
import com.jamf.regatta.core.options.GetOption;
import com.jamf.regatta.core.options.PutOption;
import com.jamf.regatta.data.convert.IndexEntry;
import com.jamf.regatta.data.convert.SecondaryIndexProvider;
import com.jamf.regatta.data.convert.RegattaConverter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.keyvalue.core.AbstractKeyValueAdapter;
import org.springframework.data.keyvalue.core.ForwardingCloseableIterator;
import org.springframework.data.util.CloseableIterator;

import java.util.Map;
import java.util.stream.Collectors;

public class RegattaKeyValueAdapter extends AbstractKeyValueAdapter implements InitializingBean, ApplicationContextAware {

    private static final ByteSequence KEY_ALL = ByteSequence.from(new byte[]{0});

    private final RegattaConverter converter;
    private final KV kv;

    public RegattaKeyValueAdapter(Client regattaClient, RegattaConverter converter) {
        super(new RegattaQueryEngine());
        this.kv = regattaClient.getKVClient();
        this.converter = converter;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    }

    @Override
    public Object put(Object id, Object item, String keyspace) {
        var table = converter.write(keyspace);
        var value = converter.write(item);

        if (item instanceof SecondaryIndexProvider entity) {
            Txn transaction = kv.txn(table);
            var primaryKey = converter.write(entity.primaryKey().indexPrefix() + "/" + entity.primaryKey().value())
            transaction.Then(Op.PutOp.put(primaryKey, value, PutOption.DEFAULT));
            for (IndexEntry entry : entity.secondaryIndexes()) {
                var secondaryIndexKey = converter.write(entry.indexPrefix() + "/" + entry.value());
                transaction.Then(Op.PutOp.put(secondaryIndexKey, primaryKey, PutOption.DEFAULT));
            }
            transaction.commit();
        } else {
            var key = converter.write(id);
            kv.put(table, key, value, PutOption.DEFAULT);
        }

        return null;
    }

    @Override
    public boolean contains(Object id, String keyspace) {
        var table = converter.write(keyspace);
        var key = converter.write(id);
        var response = kv.get(table, key, GetOption.builder().withCountOnly(true).build());
        return response.count() > 0;
    }

    @Override
    public Object get(Object id, String keyspace) {
        return this.get(id, keyspace, Object.class);
    }

    @Override
    public <T> T get(Object id, String keyspace, Class<T> type) {
        var table = converter.write(keyspace);
        var key = converter.write(id);

        GetResponse response = kv.get(table, key);
        if (response.kvs().isEmpty()) {
            return null;
        }

        return converter.read(response.kvs().get(0).value(), type);
    }

    @Override
    public Object delete(Object id, String keyspace) {
        return delete(id, keyspace, Object.class);

    }

    @Override
    public <T> T delete(Object id, String keyspace, Class<T> type) {
        var table = converter.write(keyspace);
        T entityToDelete = this.get(id, keyspace, type);

        if (entityToDelete instanceof SecondaryIndexProvider entity) {
            Txn transaction = kv.txn(table);
            for (IndexEntry entry : entity.secondaryIndexes()) {
                var secondaryIndexKey = converter.write(entry.indexPrefix() + "/" + entry.value());
                transaction.Then(Op.DeleteOp.delete(secondaryIndexKey, DeleteOption.DEFAULT));
            }
            var prefixedKey = converter.write(entity.primaryKey().indexPrefix() + "/" + id);
            Cmp primaryKeyComparator = new Cmp(converter.write(entity.primaryKey().indexPrefix() + "/" + entity.primaryKey().value()),
                    Cmp.Op.EQUAL, CmpTarget.value(prefixedKey));
            transaction.If(primaryKeyComparator)
                    .Then(Op.DeleteOp.delete(prefixedKey, DeleteOption.builder().withPrevKV(true).build()));
            transaction.commit();
        } else {
            var key = converter.write(id);
            var resp = kv.delete(table, key, DeleteOption.builder().withPrevKV(true).build());
            if (resp.deleted() > 0) {
                return entityToDelete;
            }
        }
        return null;

    }

    @Override
    public Iterable<?> getAllOf(String keyspace) {
        return getAllOf(keyspace, Object.class);
    }

    @Override
    public <T> Iterable<T> getAllOf(String keyspace, Class<T> type) {
        var table = converter.write(keyspace);
        return () -> kv.iterate(table, KEY_ALL, GetOption.builder().withRange(KEY_ALL).build())
                .flatMap(getResponse -> getResponse.kvs().stream())
                .map(keyValue -> converter.read(keyValue.value(), type))
                .iterator();
    }

    @Override
    public CloseableIterator<Map.Entry<Object, Object>> entries(String keyspace) {
        return entries(keyspace, Object.class);
    }

    @Override
    public <T> CloseableIterator<Map.Entry<Object, T>> entries(String keyspace, Class<T> type) {
        var table = converter.write(keyspace);
        Map<Object, T> collect = kv.iterate(table, KEY_ALL, GetOption.builder().withRange(KEY_ALL).build())
                .flatMap(getResponse -> getResponse.kvs().stream())
                .collect(Collectors.toMap(KeyValue::key, keyValue -> converter.read(keyValue.value(), type)));
        return new ForwardingCloseableIterator<>(collect.entrySet().iterator());
    }

    @Override
    public void deleteAllOf(String keyspace) {
        var table = converter.write(keyspace);
        kv.delete(table, KEY_ALL, DeleteOption.builder().withRange(KEY_ALL).build());
    }

    @Override
    public void clear() {

    }

    @Override
    public long count(String keyspace) {
        var table = converter.write(keyspace);
        var response = kv.get(table, KEY_ALL, GetOption.builder().withRange(KEY_ALL).withCountOnly(true).build());
        return response.count();
    }

    @Override
    public void destroy() throws Exception {

    }
}
