package io.github.dfauth.trade.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiPredicate;
import java.util.function.Function;

import static java.lang.Double.NaN;

@Slf4j
@AllArgsConstructor
public class Watermark<T> {

    private Direction direction;
    @Getter
    private T waterMark;
    @Getter
    private T current;
    @Getter
    private int intervalsSince;
    private Function<T, Double> extractor;

    public Watermark(Function<T, Double> extractor) {
        this(Direction.HIGH, null, null, 0, extractor);
    }

    public Watermark(Direction direction, Function<T, Double> extractor) {
        this(direction, null, null, 0, extractor);
    }

    public Watermark<T> update(T t) {
        if(waterMark == null) {
            waterMark = t;
            intervalsSince = 0;
        } else if(direction.test(extractor.apply(t), extractor.apply(waterMark))) {
            waterMark = t;
            intervalsSince = 0;
        } else {
            intervalsSince++;
        }
        this.current = t;
        return this;
    }

    public double getDistance() {
        if(waterMark == null) {
            return NaN;
        }
        double ref = extractor.apply(waterMark);
        return (extractor.apply(current) - ref) / ref;
    }

    public static Watermark<Price> priceWatermarker() {
        return new Watermark<>(p -> p.getClose().doubleValue());
    }

    public enum Direction implements BiPredicate<Double, Double> {
        HIGH((l, r) -> l.doubleValue() > r.doubleValue()),
        LOW((l, r) -> l.doubleValue() < r.doubleValue());

        private BiPredicate<Double, Double> p2;

        Direction(BiPredicate<Double, Double> p2) {
            this.p2 = p2;
        }

        @Override
        public boolean test(Double l, Double r) {
            return p2.test(l, r);
        }
    }

}
