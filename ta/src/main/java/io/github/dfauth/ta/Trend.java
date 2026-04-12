package io.github.dfauth.ta;

public record Trend(int duration, double price, double fast, double slow, double lng, TrendState trendState) {

    public boolean isDiverging() {
        return false;
    }

    public double distanceFromEma() {
        return (price - fast) / price;
    }
}
