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
        Function<Double, Optional<Trend>> stream = trendStream();
        return t -> stream.apply(extractor.apply(t))
                .map(trend -> {
                    return switch (trend.trendState()) {
                        case BULL -> {
                            if (current.get() == null || extractor.apply(t) > extractor.apply(current.get())) {
                                current.set(t);
                            }
                            yield new Drawdown(new Extractable(extractor, t), tmp);
                        }
                        case LATE_BULL -> {
                            if (current.get() != null && trend.duration() == 0) {
                                tmp.add(left(new High(new Extractable(extractor, current.get()))));
                                current.set(null);
                            }
                            yield new Drawdown(new Extractable(extractor, t), tmp);
                        }
                        case BEAR -> {
                            if (current.get() == null || extractor.apply(t) < extractor.apply(current.get())) {
                                current.set(t);
                            }
                            yield new Drawdown(new Extractable(extractor, t), tmp);
                        }
                        case LATE_BEAR -> {
                            if (current.get() != null && trend.duration() == 0) {
                                tmp.add(right(new Low(new Extractable(extractor, current.get()))));
                                current.set(null);
                            }
                            yield new Drawdown(new Extractable(extractor, t), tmp);
                        }
                        default -> {
                            yield new Drawdown(new Extractable(extractor, t), tmp);
                        }
                    };
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
