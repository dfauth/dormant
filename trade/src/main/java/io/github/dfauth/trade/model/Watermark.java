package io.github.dfauth.trade.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

import static java.lang.Double.NaN;

@Slf4j
@RequiredArgsConstructor
public class Watermark<T> {

    private final Direction direction;
    @Getter
    private T waterMark;
    @Getter
    private T current;
    @Getter
    private int intervalsSince;
    private final Function<T, Double> extractor;
    private final double threshold;
    private ThresholdState touches = new WithinThreshold(0);

    public Watermark(Function<T, Double> extractor) {
        this(Direction.RISING, extractor);
    }

    public Watermark(Direction direction, Function<T, Double> extractor) {
        this(direction, extractor, 0.05);
    }

    public Watermark<T> update(T t) {
        if(waterMark == null) {
            waterMark = t;
            intervalsSince = 0;
        } else if(direction.<Double>getBiPredicate().test(extractor.apply(t), extractor.apply(waterMark))) {
            waterMark = t;
            intervalsSince = 0;
        } else {
            intervalsSince++;
        }
        this.current = t;

        // look for support / resistance
        if(direction.<Double>getBiPredicate().test(extractor.apply(t), extractor.apply(waterMark) * (1.0 + direction.signed(threshold)))) {
            touches = touches.inside();
        } else {
            touches = touches.outside();
        }
        return this;
    }

    public double getDistance() {
        if(waterMark == null) {
            return NaN;
        }
        double ref = extractor.apply(waterMark);
        return (extractor.apply(current) - ref) / ref;
    }

    public int getTouches() {
        return touches.getCount();
    }

    public static Watermark<Price> priceWatermarker() {
        return new Watermark<>(p -> p.getClose().doubleValue());
    }

    @AllArgsConstructor
    static abstract class ThresholdState {
        @Getter()
        protected int count;
        abstract ThresholdState inside();
        abstract ThresholdState outside();
    }

    static class WithinThreshold extends ThresholdState {

        public WithinThreshold(int cnt) {
            super(cnt);
        }

        @Override
        public ThresholdState inside() {
            return this;
        }

        @Override
        ThresholdState outside() {
            return new OutsideThreshold(count);
        }
    }

    static class OutsideThreshold extends ThresholdState {

        public OutsideThreshold(int cnt) {
            super(cnt);
        }

        @Override
        ThresholdState inside() {
            return new WithinThreshold(count+1);
        }

        @Override
        ThresholdState outside() {
            return this;
        }
    }

}
