package io.github.dfauth.trade.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static java.lang.Math.abs;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceStats {

    private int wins;
    private int losses;
    private BigDecimal averageWin;
    private BigDecimal averageLoss;
    private BigDecimal expectancy;

    public int getTotalPositions() {
        return wins + losses;
    }

    public double getWinRate() {
        return getTotalPositions() == 0 ? 0.0 : (double) wins / (double) getTotalPositions();
    }

    public double getLossRate() {
        return 1.0 - getWinRate();
    }

    public double getRiskRewardRatio() {
        return averageLoss.doubleValue() == 0 ? 0.0 : averageWin.doubleValue() / abs(averageLoss.doubleValue());
    }

    public double getExpectancyMultiple() {
        return (getWinRate() * getRiskRewardRatio()) - getLossRate();
    }
}
