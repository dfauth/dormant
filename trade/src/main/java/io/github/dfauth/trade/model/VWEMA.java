package io.github.dfauth.trade.model;

import io.github.dfauth.ta.MovingAverage;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Function;

import static io.github.dfauth.ta.RateOfChange.roc;
import static java.lang.Math.abs;

@Slf4j
public record VWEMA(double value, double trendPrice, double trendVolume, Price price) {

    public static Function<Price, Optional<VWEMA>> create(int period, double volumeWeight) {
        Function<Double, Optional<Double>> priceStream = MovingAverage.emaStream(new double[period]);
        Function<Double, Optional<Double>> volStream = MovingAverage.emaStream(new double[period]);
        return p -> {
            Optional<Double> optPriceEma = priceStream.apply(p.close());
            Optional<Double> optVolEma = volStream.apply((double) p.volume());
            return optPriceEma.flatMap(c -> optVolEma.map(v -> new VWEMA(vwema(p, c, v, volumeWeight), c, v, p)));
        };
    }

    public static double vwema(Price price, double trendPrice, double trendVolume, double volumeWeight) {
        double priceDistanceFromTrend = roc(price.getClose().doubleValue(), trendPrice);
        double volumeDistanceFromTrend = roc(price.getVolume(), trendVolume);
        return priceDistanceFromTrend * (1.0 + abs(volumeWeight * volumeDistanceFromTrend));
    }

    public static Function<Price, Optional<VWEMA>> vwEmaStream(int period) {
        return vwEmaStream(period, 1.0);
    }

    public static Function<Price, Optional<VWEMA>> vwEmaStream(int period, double volumeWeight) {
        return create(period, volumeWeight);
    }
}
