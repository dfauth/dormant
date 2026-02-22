package io.github.dfauth.trade.service;

import io.github.dfauth.trade.model.PerformanceStats;
import io.github.dfauth.trade.model.Position;
import io.github.dfauth.trade.model.Side;
import io.github.dfauth.trade.model.Trade;
import io.github.dfauth.trade.repository.TradeRepository;
import io.github.dfauth.trade.utils.PositionCollector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static io.github.dfauth.trycatch.Utils.bd;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final TradeRepository tradeRepository;

    public List<Position> getPositions(Long userId, String market, String code) {
        List<Trade> trades = tradeRepository.findByUserIdAndMarketAndCodeOrderByDateAsc(userId, market, code);
        return buildPositions(trades);
    }

    public List<Position> getOpenPositions(Long userId) {
        return getPositions(userId, Position::isOpen);
    }

    public List<Position> getPositions(long userId, Predicate<Position> p) {
        return tradeRepository.findByUserId(userId).stream()
                .collect(new PositionCollector()).stream()
                .filter(_p -> p.test(_p))
                .toList();
    }

    public List<Position> getPositionsByMarket(Long userId, String market) {
        List<Trade> allTrades = tradeRepository.findByUserIdAndMarketOrderByDateAsc(userId, market);
        List<Position> result = new ArrayList<>();
        // Group trades by code while preserving order within each group
        allTrades.stream()
                .map(Trade::getCode)
                .distinct()
                .forEach(code -> {
                    List<Trade> tradesForCode = allTrades.stream()
                            .filter(t -> t.getCode().equals(code))
                            .toList();
                    result.addAll(buildPositions(tradesForCode));
                });
        return result;
    }

    public PerformanceStats getPerformanceStats(Long userId, String market) {
        List<Position> positions = getPositionsByMarket(userId, market);
        return computePerformanceStats(positions);
    }

    PerformanceStats computePerformanceStats(List<Position> positions) {
        List<Position> closed = positions.stream()
                .filter(p -> !p.isOpen())
                .toList();

        int total = closed.size();
        if (total == 0) {
            return PerformanceStats.builder()
                    .totalClosedPositions(0)
                    .wins(0)
                    .losses(0)
                    .winRate(0.0)
                    .averageWin(BigDecimal.ZERO)
                    .averageLoss(BigDecimal.ZERO)
                    .riskRewardRatio(0.0)
                    .expectancy(BigDecimal.ZERO)
                    .build();
        }

        List<Position> winners = closed.stream()
                .filter(p -> p.getRealisedPnl().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        List<Position> losers = closed.stream()
                .filter(p -> p.getRealisedPnl().compareTo(BigDecimal.ZERO) <= 0)
                .toList();

        double wins = winners.size();
        double losses = losers.size();

        double winRate = wins / total;

        BigDecimal averageWin = winners.isEmpty() ? BigDecimal.ZERO
                : winners.stream().map(Position::getRealisedPnl).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(wins), 10, RoundingMode.HALF_UP);

        BigDecimal averageLoss = losers.isEmpty() ? BigDecimal.ZERO
                : losers.stream().map(Position::getRealisedPnl).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(losses), 10, RoundingMode.HALF_UP);

        double riskRewardRatio = averageLoss.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : averageWin.divide(averageLoss.abs(), 10, RoundingMode.HALF_UP).doubleValue();

        BigDecimal lossRate = new BigDecimal(losses)
                .divide(new BigDecimal(total), 10, RoundingMode.HALF_UP);
        BigDecimal winRateFraction = new BigDecimal(wins)
                .divide(new BigDecimal(total), 10, RoundingMode.HALF_UP);
        BigDecimal expectancy = winRateFraction.multiply(averageWin)
                .add(lossRate.multiply(averageLoss));

        return PerformanceStats.builder()
                .totalClosedPositions(total)
                .wins((int)wins)
                .losses((int)losses)
                .winRate(winRate)
                .averageWin(averageWin)
                .averageLoss(averageLoss)
                .riskRewardRatio(riskRewardRatio)
                .expectancy(expectancy)
                .build();
    }

    List<Position> buildPositions(List<Trade> trades) {
        List<Position> positions = new ArrayList<>();
        if (trades.isEmpty()) {
            return positions;
        }

        Position current = null;
        int runningSize = 0;
        BigDecimal totalCost = BigDecimal.ZERO; // total cost basis for average price
        BigDecimal realisedPnl = BigDecimal.ZERO;

        for (Trade trade : trades) {
            if (current == null) {
                // Start a new position
                current = Position.builder()
                        .market(trade.getMarket())
                        .code(trade.getCode())
                        .trades(new ArrayList<>())
                        .build();
                runningSize = 0;
                totalCost = BigDecimal.ZERO;
                realisedPnl = BigDecimal.ZERO;
            }

            current.getTrades().add(trade);
            boolean adding = trade.getSide() == current.getSide();

            if (adding) {
                // Adding to position
                totalCost = totalCost.add(trade.getPrice()).multiply(bd(trade.getSize()));
                runningSize += trade.getSize();
            } else {
                // Reducing position
                BigDecimal avgPrice = totalCost.divide(bd(runningSize), MathContext.DECIMAL128);
                BigDecimal pnl;
                if (current.getSide() == Side.BUY) {
                    pnl = trade.getPrice().subtract(avgPrice).multiply(bd(trade.getSize()));
                } else {
                    pnl = avgPrice.subtract(trade.getPrice()).multiply(bd(trade.getSize()));
                }
                realisedPnl = realisedPnl.add(pnl);
                runningSize = runningSize - trade.getSize();
                totalCost = runningSize == 0
                        ? BigDecimal.ZERO
                        : avgPrice.multiply(bd(runningSize));
            }

            BigDecimal avgEntryPrice = runningSize == 0
                    ? BigDecimal.ZERO
                    : totalCost.divide(bd(runningSize), MathContext.DECIMAL128);

            if (runningSize == 0) {
                // Position closed
                positions.add(current);
                current = null;
            } else {
                // Position still open
            }
        }

        // If there's a remaining open position
        if (current != null) {
            positions.add(current);
        }

        return positions;
    }
}
