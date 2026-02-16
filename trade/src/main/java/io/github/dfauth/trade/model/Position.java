package io.github.dfauth.trade.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position {

    private String market;
    private String code;
    private Side side;
    private BigDecimal size;
    private BigDecimal averagePrice;
    private BigDecimal realisedPnl;
    private LocalDate openDate;
    private LocalDate closeDate;
    @Builder.Default
    private List<Trade> trades = new ArrayList<>();
    private boolean open;
}
