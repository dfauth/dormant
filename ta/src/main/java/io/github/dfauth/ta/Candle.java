package io.github.dfauth.ta;

public record Candle(double high, double low, double close) {

    public static double trueRange(double high, double low, double prevClose) {
        return Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
    }

    public double trueRange(double prevClose) {
        return trueRange(high, low, prevClose);
    }
}
