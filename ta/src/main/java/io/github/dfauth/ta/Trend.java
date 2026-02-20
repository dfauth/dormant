package io.github.dfauth.ta;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class Trend {
    private final Double price;
    private final List<Double> fast;
    private final List<Double> slow;
    private final List<Double> lng;
    private final TrendState trendState;

    public boolean isDiverging() {
        return false;
    }
}
