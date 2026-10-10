package io.github.dfauth.ta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import static io.github.dfauth.ta.RateOfChange.roc;
import static java.lang.Math.abs;

public record Trend(@JsonProperty("n") int duration,
                    @JsonIgnore double price,
                    @JsonIgnore double fast,
                    @JsonIgnore double slow,
                    @JsonIgnore double lng,
                    @JsonProperty("s") TrendState trendState,
                    @JsonIgnore double rocFast,
                    @JsonIgnore double rocSlow,
                    @JsonIgnore double rocLng
                    ) {

    public Trend(Trend previous, double price, double fast, double slow, double lng, TrendState trendState) {
        this(previous.duration + 1, price, fast, slow, lng, trendState, roc(fast, previous.fast()), roc(slow, previous.slow()), roc(lng, previous.lng()));
    }

    public boolean isDiverging() {
        return trendState.isDiverging(rocFast, rocSlow, rocLng);
    }

    public double acceleration() {
        return (rocFast - rocSlow) / abs(rocSlow);
    }

    public double distanceFromEma() {
        return (price - fast) / price;
    }
}
