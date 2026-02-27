package io.github.dfauth.trade.model;

import io.github.dfauth.ta.Trend;
import lombok.Getter;

@Getter
public class TrendSummary {
    private final String market;
    private final String code;
    private final Double price;
    private final Trend trendState;
    private final double distanceFromEma;

    public TrendSummary(String market, String code, Double price, Trend trend) {
        this.market = market;
        this.code = code;
        this.price = price;
        this.trendState = trend;
        this.distanceFromEma = trend.distanceFromEma();
    }
}
