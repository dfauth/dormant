package io.github.dfauth.ta;

import io.github.dfauth.trycatch.OptionalFunction;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static io.github.dfauth.ta.TestData.CAT;
import static io.github.dfauth.trycatch.Utils.roundTo;

@Slf4j
class OnBalanceVolumeTest {

    List<Candle> prices = TestData.unzipCandles(CAT);
    int period = 21;

    @Test
    void testOnBalanceVolume() {

        Function<Candle, OnBalanceVolume> stream = OnBalanceVolume.streamObv();

        List<Integer> result = prices.stream()
                .map(stream)
                .map(OnBalanceVolume::getOnBalanceVolume)
                .toList();
        log.info("result: {}", result);
    }


    @Test
    void testOnBalanceVolumeRocCalculation() {

        Function<Candle, OnBalanceVolume> stream = OnBalanceVolume.streamObv();

        OptionalFunction<Double, Double> rocStream = OptionalFunction.of(RateOfChange.rocStream(2));

        Optional<Double> result = prices.stream()
                .map(stream)
                .map(OnBalanceVolume::getOnBalanceVolume)
                .map(Double::valueOf)
                .reduce(RateOfChange::roc);

        log.info("result: {}", result.get());
    }

    @Test
    void testOnBalanceVolumeEmaRocCalculation() {

        Function<Candle, OnBalanceVolume> stream = OnBalanceVolume.streamObv();

        OptionalFunction<Double, Double> rocStream = OptionalFunction.of(RateOfChange.rocStream(2));
        OptionalFunction<Double, Double> emaStream = OptionalFunction.of(MovingAverage.emaStream(new double[period]));

        List<Double> result = prices.stream()
                .map(stream)
                .map(OnBalanceVolume::getOnBalanceVolume)
                .map(Double::valueOf)
                .map(rocStream.andNext(emaStream))
                .flatMap(Optional::stream)
                .map(roundTo(4))
                .toList();
        log.info("result: {}", result);
    }

//    @Test
//    void testOnBalanceVolumeEmaRocCalculation2() {
//
//        Function<Candle, OnBalanceVolume> stream = OnBalanceVolume.streamObv();
//
//        BiFunction<OnBalanceVolume, OnBalanceVolume, RateOfChange<OnBalanceVolume>> f = OnBalanceVolume::rateOfChange;
//
//        Object f2 = OnBalanceVolume::ema;
//
//        Function<OnBalanceVolume, Optional<RateOfChange<OnBalanceVolume>>> curriedF = Function2.curry(f);
//
//        List<Double> result = prices.stream()
//                .map(stream)
//                .map(curriedF)
//                .flatMap(Optional::stream)
//                .map()
//                .map(roundTo(4))
//                .toList();
//        log.info("result: {}", result);
//    }

}
