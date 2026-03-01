package io.github.dfauth.trade.model;

import io.github.dfauth.ta.RingBuffer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.util.Optional.empty;

@AllArgsConstructor
@Getter
public class EMA {

    public static final int[] DEFAULT_PERIODS = new int[]{8,21,200};

    private final double price;
    private final double volume;

    public static Function<Price, Optional<EMA>[]> createN(int... periods) {
        List<EMACalculator<Price, EMA>> calculators = IntStream.of(periods).mapToObj(EMA::create).toList();
        return p -> calculators.stream().map(c -> c.apply(p)).toArray(Optional[]::new);
    }

    public static EMACalculator<Price, EMA> create(int period) {
        RingBuffer<EMA> ringBuffer = RingBuffer.create(new EMA[period]);
        return new EMACalculator<>(ringBuffer, c -> (price, previous) -> new EMA(
                c.calculate(price.getClose().doubleValue(), previous.price),
                c.calculate(price.getVolume(), previous.volume)
        ), p -> new EMA(p.getClose().doubleValue(), p.getVolume()));
    }

    @RequiredArgsConstructor
    static class EMACalculator<T,R> implements Function<T, Optional<R>> {

        private final double multiplier;
        private final RingBuffer<R> ringBuffer;
        private final Function<EMACalculator<T,R>, BiFunction<T,R,R>> callback;
        private final Function<T,R> initalValueCalculator;

        public EMACalculator(RingBuffer<R> ringBuffer, Function<EMACalculator<T,R>, BiFunction<T,R,R>> callback, Function<T,R> initalValueCalculator) {
            this(2.0/(ringBuffer.capacity() + 1.0), ringBuffer, callback, initalValueCalculator);
        }

        public Optional<R> apply(T current) {
            R prev = ringBuffer.read();
            R next = Optional.ofNullable(prev).map(p -> calculate(current, p)).orElse(initalValueCalculator.apply(current));
            ringBuffer.write(next);
            return ringBuffer.isFull() ? Optional.of(next) : empty();
        }

        public R calculate(T current, R prev) {
            return callback.apply(this).apply(current, prev);
        }

        public double calculate(double current, double prev) {
            return calculate(multiplier, current, prev);
        }

        public static double calculate(double multiplier, double current, double prev) {
            return (current * multiplier) + (prev * (1 - multiplier));
        }
    }
}
