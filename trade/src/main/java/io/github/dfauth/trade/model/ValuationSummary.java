package io.github.dfauth.trade.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public record ValuationSummary(
        Long id,
        String market,
        String code,
        LocalDate date,
        Consensus consensus,
        Integer buy,
        Integer hold,
        Integer sell,
        BigDecimal target,
        BigDecimal price,
        BigDecimal potential
) {
    public static ValuationSummary of(Valuation v, BigDecimal price) {
        BigDecimal potential = null;
        if (price != null && price.compareTo(BigDecimal.ZERO) != 0) {
            potential = v.getTarget().subtract(price)
                    .divide(price, 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return new ValuationSummary(
                v.getId(), v.getMarket(), v.getCode(), v.getDate(),
                v.getConsensus(), v.getBuy(), v.getHold(), v.getSell(),
                v.getTarget(), price, potential
        );
    }
}
