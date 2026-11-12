package io.github.dfauth.dormant.actor;

import io.github.dfauth.dormant.actor.repository.PriceVolume;
import io.github.dfauth.dormant.model.Price;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Optional.empty;

@RequiredArgsConstructor
public class PriceVolumeAggregator implements Aggregator<Price, PriceVolume, PriceVolumeAggregator> {

    private final AtomicReference<Double> close = new AtomicReference<>((double) 0);
    private final AtomicReference<Double> volume = new AtomicReference<>((double) 0);
    private final AtomicInteger count = new AtomicInteger();
    private final int period;

    public PriceVolumeAggregator() {
        this(0);
    }

    @Override
    public PriceVolumeAggregator aggregate(Price price) {
        close.updateAndGet(d -> d + price.getClose().doubleValue());
        volume.updateAndGet(v -> v + price.getVolume());
        count.getAndIncrement();
        return this;
    }

    @Override
    public Optional<PriceVolume> finish() {
        return count.get() <= period ? empty() : Optional.of(new PriceVolume(close.get() / count.get(),  volume.get() / count.get()));
    }
}
