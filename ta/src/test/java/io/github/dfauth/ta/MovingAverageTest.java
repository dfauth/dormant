package io.github.dfauth.ta;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static io.github.dfauth.ta.MovingAverage.ema;
import static io.github.dfauth.ta.TestData.CSL;
import static io.github.dfauth.trycatch.Collectors.immutableCollector;
import static java.util.Arrays.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MovingAverageTest {

    @Test
    void testEmaCalculation() {
        double[] prices = {10, 20, 30, 50, 70};

        Function<Double, Optional<Double>> f = MovingAverage.emaStream(new double[3]);

        List<Double> result = stream(prices).mapToObj(f::apply).flatMap(Optional::stream).toList();

        assertEquals(3, result.size());
        Iterator<Double> it = result.iterator();
        assertEquals(20.0, it.next(), 1e-9);            // SMA seed: (10+20+30)/3 = 20.0
        assertEquals(35.0, it.next(), 1e-9);             // (50-20)*0.5 + 20 = 35.0
        assertEquals(52.5, it.next(), 1e-9);             // (70-35)*0.5 + 35
    }

    @Test
    void testSmaCalculation() {
        double[] prices = {10, 20, 30, 50, 70};

        Function<Double, Optional<Double>> f = MovingAverage.smaStream(new Double[3]);

        List<Double> result = stream(prices).mapToObj(f::apply).flatMap(Optional::stream).toList();

        assertEquals(3, result.size());
        Iterator<Double> it = result.iterator();
        assertEquals(20.0, it.next(), 1e-9);            // SMA seed: (10+20+30)/3 = 20.0
        assertEquals(33.333333333, it.next(), 1e-9);    // (20+30+50)/3 = 100/3 = 33.333333333
        assertEquals(50.0, it.next(), 1e-9);            // (30+50+70)/3 = 150/3 = 50.0
    }

    @Test
    void testSmaPriceCalculation() {
        int period = 21;
        List<Candle> prices = TestData.unzipCandles(CSL);

        BinaryOperator<Candle> accumulator =  (a, c) -> new CandleRecord(
                a.open() + c.open(),
                a.high() + c.high(),
                a.low() + c.low(),
                a.close() + c.close(),
                a.volume() + c.volume()
                );
        Function<Integer, UnaryOperator<Candle>> finisher =  n -> a -> new CandleRecord(
                a.open()/n,
                a.high()/n,
                a.low()/n,
                a.close()/n,
                a.volume()/n
                );
        Function<Candle, Optional<Candle>> stream = MovingAverage.smaStream(new Candle[period], immutableCollector(
                CandleRecord::new,
                accumulator,
                accumulator,
                finisher.apply(period)
        ));

        List<Candle> result = prices.stream().map(stream).flatMap(Optional::stream).toList();

        assertEquals(prices.size()-period+1, result.size());
        // test last 3 values
        Iterator<Candle> it = result.subList(result.size()-3, result.size()).iterator();
        assertEquals(115.80428571428571, it.next().close(), 1e-9);
        assertEquals(116.46285714285716, it.next().close(), 1e-9);
        assertEquals(117.29285714285714, it.next().close(), 1e-9);
        assertFalse(it.hasNext());
    }

    @Test
    void testEmaPriceCalculation() {
        int period = 21;
        List<Candle> prices = TestData.unzipCandles(CSL);

        BinaryOperator<Candle> accumulator =  (a, c) -> new CandleRecord(
                a.open() + c.open(),
                a.high() + c.high(),
                a.low() + c.low(),
                a.close() + c.close(),
                a.volume() + c.volume()
                );
        Function<Integer, UnaryOperator<Candle>> finisher =  n -> a -> new CandleRecord(
                a.open()/n,
                a.high()/n,
                a.low()/n,
                a.close()/n,
                a.volume()/n
                );
        Function<Candle, Optional<Candle>> stream = MovingAverage.emaStream(new Candle[period], immutableCollector(
                CandleRecord::new,
                accumulator,
                accumulator,
                finisher.apply(period)
        ), w -> (t, p) -> new CandleRecord(
                ema(w, t.open(), p.open()),
                ema(w, t.close(), p.close()),
                ema(w, t.high(), p.high()),
                ema(w, t.low(), p.low()),
                (int) ema(w, t.volume(), p.volume())
        ));

        List<Candle> result = prices.stream().map(stream).flatMap(Optional::stream).toList();

        assertEquals(prices.size()-period+1, result.size());
        // test last 3 values
        Iterator<Candle> it = result.subList(result.size()-3, result.size()).iterator();
        assertEquals(122.55703613029488, it.next().close(), 1e-9);
        assertEquals(120.59151414755989, it.next().close(), 1e-9);
        assertEquals(121.79685370374213, it.next().close(), 1e-9);
        assertFalse(it.hasNext());
    }

}
