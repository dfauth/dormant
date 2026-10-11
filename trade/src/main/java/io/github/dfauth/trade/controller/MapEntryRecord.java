package io.github.dfauth.trade.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public record MapEntryRecord<K, V>(K key, V value) implements Map.Entry<K, V> {

    public MapEntryRecord(Map.Entry<K, V> entry) {
        this(entry.getKey(),  entry.getValue());
    }

    @JsonProperty("k")
    @Override
    public K getKey() {
        return key;
    }

    @JsonProperty("v")
    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }
}
