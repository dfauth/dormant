package io.github.dfauth.trade.model;

import io.github.dfauth.trycatch.Maps;
import lombok.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

import static io.github.dfauth.trycatch.Tuple2.tuple2;
import static io.github.dfauth.trycatch.Utils.bd;
import static java.lang.Math.abs;
import static java.math.BigDecimal.ZERO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position {

    private String market;
    private String code;
    @Builder.Default
    private List<Trade> trades = new ArrayList<>();

    public static Position of(Trade t) {
        return Position.builder().market(t.getMarket()).code(t.getCode()).build().addTrade(t);
    }

    public Position addTrade(Trade trade) {
        trades.add(trade);
        return this;
    }

    public boolean isOpen() {
        return !isClosed();
    }

    public boolean isClosed() {
        return getSize() == 0;
    }

    public int getSize() {
        return abs(trades.stream().mapToInt(t -> t.getSide().signed(t.getSize())).sum());
    }

    public Side getSide() {
        return trades.getFirst().getSide();
    }

    public boolean isShort() {
        return getSide().isSell();
    }

    public BigDecimal getRealisedPnl() {
        if(isClosed()) {
            return Optional.of(trades)
                    .flatMap(t -> t.stream().map(_t -> _t.getSide().signed(_t.getCost())).reduce(BigDecimal::add))
                    .orElse(ZERO);
        } else {
            Map<Side, SideSizeCost> tmp = trades.stream()
                    .map(SideSizeCost::new)
                    .reduce(new HashMap<>(), (acc, ssc) -> {
                        acc.computeIfPresent(ssc.side, (k, v) -> v.add(ssc));
                        acc.computeIfAbsent(ssc.side, k -> ssc);
                        return acc;
                    }, Maps.merge(SideSizeCost::merge));
            if(tmp.size() == 1) {
                return ZERO;
            } else {
                return tmp.values().stream().reduce(SideSizeCost::merge)
                    .map(SideSizeCost::cost)
                    .orElse(ZERO);
            }
        }
    }

    public BigDecimal getAveragePrice() {
        return trades.stream()
                .filter(t -> t.getSide() == getSide())
                .reduce(tuple2(ZERO, 0), (t2, t) -> {
                    return tuple2(t2._1().add(t.getCost()), t2._2() + t.getSize());
                }, (l, r) -> tuple2(l._1().add(r._1()), l._2() + r._2()))
                .map((cost, size) -> cost.divide(bd(size), MathContext.DECIMAL128).setScale(cost.scale(), RoundingMode.HALF_UP));
    }

    public LocalDate getOpenDate() {
        return trades.getFirst().getDate();
    }

    public Optional<LocalDate> getCloseDate() {
        return Optional.of(trades.getLast()).filter(t -> isClosed()).map(Trade::getDate);
    }

    private record SideSizeCost(Side side, int size, BigDecimal cost) {
        public SideSizeCost(Trade trade) {
            this(trade.getSide(), trade.getSize(), trade.getCost());
        }

        public SideSizeCost add(SideSizeCost other) {
            assert (other.side == side);
            return new SideSizeCost(side, size + other.size, cost.add(other.cost));
        }

        public SideSizeCost merge(SideSizeCost other) {
            if (size < other.size) {
                return calculate(this, other);
            } else {
                return calculate(other, this);
            }
        }

        private static SideSizeCost calculate(SideSizeCost l, SideSizeCost r) {
            return new SideSizeCost(l.side, l.size,
                    l.side.signed(l.cost).add(bd((double) l.size / (double) r.size).multiply(r.side.signed(r.cost)))
                            .setScale(l.cost.scale(), RoundingMode.HALF_UP));
        }
    }
}
