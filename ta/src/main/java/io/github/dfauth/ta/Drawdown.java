package io.github.dfauth.ta;

import io.github.dfauth.trycatch.Either;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static io.github.dfauth.ta.TrendCalculator.trendStream;
import static io.github.dfauth.trycatch.Either.left;
import static io.github.dfauth.trycatch.Either.right;
import static java.util.function.Function.identity;

@Slf4j
public record Drawdown<T>(Extractable<T, Double> value, List<Either<High<T>, Low<T>>> extremes) {

    public Optional<T> getRecentMax() {
        return extremes.reversed().stream()
                .filter(Either::isLeft)
                .findFirst()
                .map(e -> e.left())
                .map(High::value)
                .map(Extractable::payload);
    }

    public Optional<T> getRecentMin() {
        return extremes.reversed().stream()
                .filter(Either::isRight)
                .findFirst()
                .map(e -> e.right())
                .map(Low::value)
                .map(Extractable::payload);
    }

    public Optional<Double> getCurrentDrawdown() {
        return extremes.reversed().stream()
                .filter(Either::isLeft)
                .findFirst()
                .map(e -> e.left())
                .map(High::value)
                .map(hi -> (hi.value() - value.value())/hi.value());
    }

    public Optional<Double> getMaxDrawdown() {
        return extremes.reversed().stream()
                .filter(Either::isLeft)
                .findFirst()
                .map(e -> e.left())
                .map(High::value)
                .flatMap(hi -> extremes.reversed().stream()
                        .filter(Either::isRight)
                        .findFirst()
                        .map(e -> e.right()).map(Low::value).map(lo -> (hi.value() - lo.value())/hi.value()));
    }

    public static Function<Double, Optional<Drawdown<Double>>> drawdownStream() {
        return drawdownStream(identity());
    }

    public static <T> Function<T, Optional<Drawdown<T>>> drawdownStream(Function<T, Double> extractor) {
        List<Either<High<T>, Low<T>>> tmp = new ArrayList<>();
        AtomicReference<T> current = new AtomicReference<>();
        AtomicReference<Trend> previousTrend = new AtomicReference<>();
        Function<Double, Optional<Trend>> stream = trendStream();
        return t -> stream.apply(extractor.apply(t))
                .map(trend -> {
                    if (current.get() == null) {
                        current.set(t);
                    } else if (trend.trendState().isRising() && extractor.apply(t) > extractor.apply(current.get())) {
                        if (trend.duration() == 0 && previousTrend.get().trendState().isFalling()) {
                            tmp.add(right(new Low<>(new Extractable(extractor, current.get()))));
                        }
                        current.set(t);
                    } else if (trend.trendState().isFalling() && extractor.apply(t) < extractor.apply(current.get())) {
                        if (trend.duration() == 0 && previousTrend.get().trendState().isRising()) {
                            tmp.add(left(new High<>(new Extractable(extractor, current.get()))));
                        }
                        current.set(t);
                    } else {
                        if (trend.duration() == 0 && previousTrend.get().trendState().isRising() && trend.trendState().isFalling()) {
                            tmp.add(left(new High(new Extractable(extractor, current.get()))));
                            current.set(null);
                        } else if (trend.duration() == 0 && previousTrend.get().trendState().isFalling() && trend.trendState().isRising()) {
                            tmp.add(right(new Low(new Extractable(extractor, current.get()))));
                            current.set(null);
                        }
                    }
                    previousTrend.set(trend);
                    return new Drawdown(new Extractable(extractor, t), tmp);
                });

    }

    public T getCurrent() {
        return value.payload();
    }

    public record High<T>(Extractable<T, Double> value) {
    }

    public record Low<T>(Extractable<T, Double> value) {
    }

    public record Extractable<T, R>(Function<T, R> extractor, T t) {
        public R value() {
            return extractor.apply(t);
        }

        public T payload() {
            return t;
        }
    }
}
