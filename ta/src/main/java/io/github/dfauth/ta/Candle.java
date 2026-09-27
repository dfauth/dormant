package io.github.dfauth.ta;

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
        return new CandleRecord(open, high, low, close, volume);
    }

}
record CandleRecord(double open, double high, double low, double close, int volume) implements Candle {
    public CandleRecord() {
        this(0.0, 0.0, 0.0, 0.0, 0);
    }
}

