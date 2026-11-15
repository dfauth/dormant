package io.github.dfauth.dormant.actor;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Gatherer;

public class SimpleAverage<T, R extends Foldable<T, R> & Scalable<R>> implements PartialFunction<T, R> {

    public static final SimpleAverage<Double, Foldable.FoldableScalableDouble> sma(int period) {
        return new SimpleAverage<>(period, Foldable.FoldableScalableDouble::new, new Foldable.FoldableScalableDouble());
    }

    private final int period;
    private final Function<T, R> extractor;
    private R r;
    private int cnt = 0;

    public SimpleAverage(int period, Function<T, R> extractor, R r) {
        this.period = period;
        this.extractor = extractor;
        this.r = r;
    }

    @Override
    public Optional<R> apply(T t) {
        this.r = this.r.aggregate(t);
        this.cnt++;
        return Optional.ofNullable(this.r)
                .filter(_ -> cnt >= period)
                .map(_r -> _r.scale(1.0 / this.cnt));
    }

    public Gatherer<T, R, R> gatherer(R r) {
        return gatherer(r, Optional::of);
    }
}
