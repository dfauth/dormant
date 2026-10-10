package io.github.dfauth.ta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.dfauth.trycatch.OptionalFunction;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static io.github.dfauth.ta.RateOfChange.roc;

@RequiredArgsConstructor
@ToString
@EqualsAndHashCode
public class OnBalanceVolume implements Function<Candle, OnBalanceVolume>{

    @Getter
    private final int onBalanceVolume;
    @Getter
    @JsonIgnore
    private final Candle candle;

    public OnBalanceVolume(Candle current) {
        this(current.volume(), current);
    }

    public static Function<Candle, OnBalanceVolume> streamObv() {
        AtomicReference<OnBalanceVolume> previous = new AtomicReference<>();
        return c -> {
            if(previous.get() != null) {
                return previous.updateAndGet(p -> p.apply(c));
            } else {
                return previous.updateAndGet(_ -> new OnBalanceVolume(c));
            }
        };
    }

    public static Function<Candle, Optional<OnBalanceVolume>> streamObvRocEma(int period) {

        OptionalFunction<Double, Double> rocStream = OptionalFunction.of(RateOfChange.rocStream(2));
        OptionalFunction<Double, Double> emaStream = OptionalFunction.of(MovingAverage.emaStream(new double[period]));
        OptionalFunction<Double, Double> emaRocFn = rocStream.andNext(emaStream);

        Function<Candle, OnBalanceVolume> obvFn = OnBalanceVolume.streamObv();
        return c -> {
            OnBalanceVolume obv = obvFn.apply(c);
            Optional<Double> optEma = emaRocFn.apply(Double.valueOf(obv.getOnBalanceVolume()));
            return optEma.map(ema -> new OnBalanceVolume(obv.onBalanceVolume, c));
        };
    }

    public static int onBalanceVolume(Candle current, OnBalanceVolume onBalanceVolume) {
        return onBalanceVolume(current, onBalanceVolume.candle, onBalanceVolume.onBalanceVolume);
    }

    public static int onBalanceVolume(Candle current, Candle previous, int onBalanceVolume) {

        double diff = current.close() - previous.close();
        return diff > 0 ?
                onBalanceVolume + current.volume() :
                diff < 0 ?
                        onBalanceVolume - current.volume() :
                        onBalanceVolume;
    }

    @Override
    public OnBalanceVolume apply(Candle current) {
        return new OnBalanceVolume(onBalanceVolume(current, this), current);
    }

    public RateOfChange<OnBalanceVolume> rateOfChange(OnBalanceVolume next) {
        return new RateOfChange<>(roc(next.onBalanceVolume, onBalanceVolume), next);
    }

    public MovingAverage.EMA<RateOfChange<OnBalanceVolume>> ema(double multiplier, double previous, RateOfChange<OnBalanceVolume> roc) {
        return new MovingAverage.EMA<>(multiplier, previous, roc, r -> (double) r.getT().onBalanceVolume);
    }
}
