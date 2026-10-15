package io.github.dfauth.trade.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.dfauth.trycatch.Tuple2;

import java.util.Map;

public record MapEntry<K, V>(K k, V v) implements Tuple2<K, V> {

    public MapEntry(Map.Entry<K, V> entry) {
        this(entry.getKey(),  entry.getValue());
    }

    @JsonProperty("k")
    @Override
    public K _1() {
        return k;
    }

    @JsonProperty("v")
    @Override
    public V _2() {
        return v;
    }
}
