package io.github.dfauth.ta;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.Math.abs;

@Slf4j
@NoArgsConstructor
@ToString
public class PivotPoints implements Function<Candle, PivotPoints> {

    public static PivotPoints apply(PivotPoints p, Candle c) {
        return p.apply(c);
    }

    @Getter
    private WithOffSet<Candle> swingHigh;
    @Getter
    private WithOffSet<Candle> swingLow;

    @Override
    public PivotPoints apply(Candle candle) {
        Optional.ofNullable(swingHigh).ifPresentOrElse(
                hi -> swingHigh = swingHigh.compare(candle),
                () -> swingHigh = new WithOffSet<>(candle, (l, r) -> {
                    double diff = r.close() - l.close();
                    return (int) (diff / abs(diff));
                })
        );
        Optional.ofNullable(swingLow).ifPresentOrElse(
                hi -> swingLow = swingLow.compare(candle),
                () -> swingLow = new WithOffSet<>(candle, (l, r) -> {
                    double diff = l.close() - r.close();
                    return (int) (diff / abs(diff));
                })
        );
        return this;
    }

    @AllArgsConstructor
    @ToString
    private static class WithOffSet<T> {

        @Getter
        private int offset;
        @Getter
        private final T t;
        private final Comparator<T> comparator;

        public WithOffSet(T t, Comparator<T> comparator) {
            this(0, t, comparator);
        }

        public WithOffSet<T> compare(T t) {
            return comparator.compare(this.t, t) > 0 ? new WithOffSet<>(t, comparator) : this.increment();
        }

        private WithOffSet<T> increment() {
            offset++;
            return this;
        }
    }
}
