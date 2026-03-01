package io.github.dfauth.ta;

public interface Candle {

    double open();

    double high();

    double low();

    double close();

    default double trueRange(Candle previous) {
        return AverageTrueRange.trueRange(high(), low(), previous.close());
    }

    static Candle candle(double open, double high, double low, double close) {
        return new CandleRecord(open, high, low, close);
    }

}
record CandleRecord(double open, double high, double low, double close) implements Candle {
}
