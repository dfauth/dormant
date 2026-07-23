package io.github.dfauth.ta;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record Trend(int duration,
                    @JsonIgnore double price,
                    @JsonIgnore double fast,
                    @JsonIgnore double slow,
                    @JsonIgnore double lng,
                    TrendState trendState) {

    @JsonIgnore
    public boolean isDiverging() {
        return false;
    }

    public double distanceFromEma() {
        return (price - fast) / price;
    }
}
