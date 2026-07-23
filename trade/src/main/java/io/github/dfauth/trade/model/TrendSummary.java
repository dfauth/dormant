package io.github.dfauth.trade.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.dfauth.ta.Trend;
import lombok.Getter;

@Getter
public class TrendSummary {
    @JsonIgnore
    private final String market;
    private final String code;
    @JsonIgnore
    private final Double price;
    @JsonProperty("t")
    private final Trend trendState;
    @JsonIgnore
    private final double distanceFromEma;

    public TrendSummary(String market, String code, Double price, Trend trend) {
        this.market = market;
        this.code = code;
        this.price = price;
        this.trendState = trend;
        this.distanceFromEma = trend.distanceFromEma();
    }
}
