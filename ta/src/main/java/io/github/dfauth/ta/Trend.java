package io.github.dfauth.ta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Trend(@JsonProperty("n") int duration,
                    @JsonIgnore double price,
                    @JsonIgnore double fast,
                    @JsonIgnore double slow,
                    @JsonIgnore double lng,
                    @JsonProperty("s") TrendState trendState) {

    @JsonIgnore
    public boolean isDiverging() {
        return false;
    }

    public double distanceFromEma() {
        return (price - fast) / price;
    }
}
