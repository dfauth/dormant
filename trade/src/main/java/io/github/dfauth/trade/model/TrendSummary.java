package io.github.dfauth.trade.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.dfauth.ta.Trend;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TrendSummary {
    @JsonIgnore
    @Getter
    private final String market;
    @JsonIgnore
    @Getter
    private final String code;
    private final Double price;
    @JsonProperty("t")
    @Getter
    private final Trend trendState;
    private final double distanceFromEma;

    public TrendSummary(String market, String code, Double price, Trend trend) {
        this.market = market;
        this.code = code;
        this.price = price;
        this.trendState = trend;
        this.distanceFromEma = trend.distanceFromEma();
    }

    @JsonProperty("d")
    public BigDecimal getDistageFromEma() {
        return Double.isNaN(distanceFromEma) || Double.isInfinite(distanceFromEma) ? null : BigDecimal.valueOf(distanceFromEma).setScale(2, RoundingMode.HALF_UP);
    }
}
