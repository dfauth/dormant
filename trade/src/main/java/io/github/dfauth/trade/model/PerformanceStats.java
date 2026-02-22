package io.github.dfauth.trade.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceStats {

    private int totalClosedPositions;
    private int wins;
    private int losses;
    private double winRate;
    private BigDecimal averageWin;
    private BigDecimal averageLoss;
    private double riskRewardRatio;
    private BigDecimal expectancy;
}
