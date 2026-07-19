package io.github.dfauth.trade.model;

import io.github.dfauth.ta.RingBuffer;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Function;

@Slf4j
public record VWEMA(double value, double trendPrice, double trendVolume, Price price) {

    public static EMA.EMACalculator<Price, VWEMA> create(int period, double volumeWeight) {
        RingBuffer<VWEMA> ringBuffer = RingBuffer.create(new VWEMA[period]);
        return new EMA.EMACalculator<>(ringBuffer,
          c -> (p, previous) -> new VWEMA(
                  c.calculate(vwema(p, previous.trendPrice, previous.trendVolume, volumeWeight), previous.value()),
                  c.calculate(p.close(), previous.price.close()),
                  c.calculate(p.getVolume(), previous.price.getVolume()), p),
         p -> new VWEMA(0, 0, 0, p)
        );
    }

    public static double vwema(Price price, double trendPrice, double trendVolume, double volumeWeight) {
        double priceDistanceFromTrend = (price.getClose().doubleValue() - trendPrice) / price.getClose().doubleValue();
        double volumeDistanceFromTrend = (price.getVolume() - trendVolume) / (double) price.getVolume();
        return 100.0 * priceDistanceFromTrend * (1.0 + (volumeWeight * volumeDistanceFromTrend));
    }

    public static Function<Price, Optional<VWEMA>> vwEmaStream(int period) {
        return vwEmaStream(period, 0.5);
    }

    public static Function<Price, Optional<VWEMA>> vwEmaStream(int period, double volumeWeight) {
        return create(period, volumeWeight);
    }
}
