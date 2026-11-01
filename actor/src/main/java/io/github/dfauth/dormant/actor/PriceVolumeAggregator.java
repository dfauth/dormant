package io.github.dfauth.dormant.actor;

import io.github.dfauth.dormant.actor.repository.PriceVolume;
import io.github.dfauth.dormant.model.Price;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@Slf4j
public class PriceVolumeAggregator implements Aggregator<Price, PriceVolume> {

    @Override
    public PriceVolume aggregate(Stream<Price> stream) {

        AtomicReference<Double> close = new AtomicReference<>((double) 0);
        AtomicReference<Double> volume = new AtomicReference<>((double) 0);
        AtomicInteger count = new AtomicInteger();

        stream.forEach(price -> {
            close.updateAndGet(d -> d + price.getClose().doubleValue());
            volume.updateAndGet(v -> v + price.getVolume());
            count.getAndIncrement();
        });
        return new PriceVolume(close.get() / count.get(),  volume.get() / count.get());
    }

}
