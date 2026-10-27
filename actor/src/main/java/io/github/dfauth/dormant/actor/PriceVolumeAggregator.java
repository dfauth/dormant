package io.github.dfauth.dormant.actor;

import io.github.dfauth.dormant.actor.repository.PriceVolume;
import io.github.dfauth.dormant.model.Price;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PriceVolumeAggregator implements Aggregator<Price, PriceVolumeAggregator, PriceVolume> {

    private double close;
    private int volume;
    private int cnt;

    @Override
    public PriceVolume get() {
        return new PriceVolume(close/cnt, ((double)volume)/cnt);
    }

    @Override
    public PriceVolumeAggregator aggregate(Price p) {
        this.close += p.close();
        this.volume += p.volume();
        this.cnt++;
        return this;
    }

    @Override
    public PriceVolumeAggregator combine(PriceVolumeAggregator aggregate) {
        this.close += aggregate.close;
        this.volume += aggregate.volume;
        this.cnt += aggregate.cnt;
        return this;
    }
}
