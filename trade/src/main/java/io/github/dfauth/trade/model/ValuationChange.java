package io.github.dfauth.trade.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ValuationChange(
        String market,
        String code,
        LocalDate date,
        LocalDate prevDate,
        BigDecimal target,
        BigDecimal prevTarget,
        BigDecimal targetChange,
        BigDecimal targetChangePct,
        BigDecimal price,
        BigDecimal potential,
        Consensus consensus
) {}
