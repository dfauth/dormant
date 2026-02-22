package io.github.dfauth.trade.utils;

import io.github.dfauth.trade.model.Position;
import io.github.dfauth.trade.model.Trade;
import io.github.dfauth.trycatch.Maps;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

@Slf4j
public class PositionCollector implements Collector<Trade, Map<PositionCollector.Key, Position>, Collection<Position>> {

    @Override
    public Supplier<Map<PositionCollector.Key, Position>> supplier() {
        return TreeMap::new;
    }

    @Override
    public BiConsumer<Map<PositionCollector.Key, Position>, Trade> accumulator() {
        return (acc, t) -> {
            acc.values().stream()
                    .filter(p -> p.getMarket().equals(t.getMarket()) && p.getCode().equals(t.getCode()))
                    .filter(Position::isOpen)
                    .findFirst()
                    .ifPresentOrElse(
                            p -> p.addTrade(t),
                            () -> acc.put(new Key(t.getMarket()+":"+t.getCode(), t.getDate()), Position.of(t))
                    );
        };
    }

    @Override
    public BinaryOperator<Map<PositionCollector.Key, Position>> combiner() {
        return Maps.merge();
    }

    @Override
    public Function<Map<PositionCollector.Key, Position>, Collection<Position>> finisher() {
        return Map::values;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
    
    public record Key(String code, LocalDate date) implements Comparable<Key> {

        @Override
        public int compareTo(Key other) {
            int dateComparison = date.compareTo(other.date);
            return dateComparison == 0 ?
                    code.compareTo(other.code) :         
                    dateComparison;
        }
    }
}
