package io.github.dfauth.ta;

import java.time.LocalDate;

public interface Candle {

    double open();

    double high();

    double low();

    double close();

    int volume();

    default double trueRange(Candle previous) {
        return AverageTrueRange.trueRange(high(), low(), previous.close());
    }

    static Candle candle(double open, double high, double low, double close, int volume) {
        return candle(LocalDate.now(), open, high, low, close, volume);
    }

    static Candle candle(LocalDate date, double open, double high, double low, double close, int volume) {
        return new CandleRecord(date, open, high, low, close, volume);
    }

}
record CandleRecord(LocalDate date, double open, double high, double low, double close, int volume) implements Candle, Dated<Candle> {
    @Override
    public Candle payload() {
        return this;
    }
}

