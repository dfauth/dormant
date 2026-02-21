package io.github.dfauth.trade.model;

import java.time.LocalDate;

public record MarketDepthSummary(
        LocalDate date,
        String code,
        int buyers,
        int buyerShares,
        int sellers,
        int sellerShares,
        double ratio
) {}
