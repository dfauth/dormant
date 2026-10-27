package io.github.dfauth.dormant.actor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DoubleAggregator implements Aggregator<Double, DoubleAggregator, Double> {

    private double d;
    private int cnt;

    @Override
    public Double get() {
        return d / cnt;
    }

    @Override
    public DoubleAggregator aggregate(Double d) {
        this.d += d;
        this.cnt++;
        return this;
    }

    @Override
    public DoubleAggregator combine(DoubleAggregator aggregate) {
        this.d += aggregate.d;
        this.cnt += aggregate.cnt;
        return this;
    }
}
