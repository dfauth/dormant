package io.github.dfauth.trade.service;

import io.github.dfauth.trade.model.PerformanceStats;
import io.github.dfauth.trade.model.Position;
import io.github.dfauth.trade.model.Side;
import io.github.dfauth.trade.model.Trade;
import io.github.dfauth.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final TradeRepository tradeRepository;

    public List<Position> getPositions(String market, String code) {
        List<Trade> trades = tradeRepository.findByMarketAndCodeOrderByDateAsc(market, code);
        return buildPositions(trades);
    }

    public List<Position> getOpenPositions() {
        List<Object[]> pairs = tradeRepository.findDistinctMarketAndCode();
        List<Position> open = new ArrayList<>();
        for (Object[] pair : pairs) {
            String market = (String) pair[0];
            String code = (String) pair[1];
            List<Position> positions = getPositions(market, code);
            positions.stream().filter(Position::isOpen).forEach(open::add);
        }
        return open;
    }

    public List<Position> getPositionsByMarket(String market) {
        List<Trade> allTrades = tradeRepository.findByMarketOrderByDateAsc(market);
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

    public PerformanceStats getPerformanceStats(String market) {
        List<Position> positions = getPositionsByMarket(market);
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
                    .winRate(BigDecimal.ZERO)
                    .averageWin(BigDecimal.ZERO)
                    .averageLoss(BigDecimal.ZERO)
                    .riskRewardRatio(BigDecimal.ZERO)
                    .expectancy(BigDecimal.ZERO)
                    .build();
        }

        List<Position> winners = closed.stream()
                .filter(p -> p.getRealisedPnl().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        List<Position> losers = closed.stream()
                .filter(p -> p.getRealisedPnl().compareTo(BigDecimal.ZERO) <= 0)
                .toList();

        int wins = winners.size();
        int losses = losers.size();

        BigDecimal winRate = new BigDecimal(wins)
                .divide(new BigDecimal(total), 10, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        BigDecimal averageWin = winners.isEmpty() ? BigDecimal.ZERO
                : winners.stream().map(Position::getRealisedPnl).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(wins), 10, RoundingMode.HALF_UP);

        BigDecimal averageLoss = losers.isEmpty() ? BigDecimal.ZERO
                : losers.stream().map(Position::getRealisedPnl).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(losses), 10, RoundingMode.HALF_UP);

        BigDecimal riskRewardRatio = averageLoss.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : averageWin.divide(averageLoss.abs(), 10, RoundingMode.HALF_UP);

        BigDecimal lossRate = new BigDecimal(losses)
                .divide(new BigDecimal(total), 10, RoundingMode.HALF_UP);
        BigDecimal winRateFraction = new BigDecimal(wins)
                .divide(new BigDecimal(total), 10, RoundingMode.HALF_UP);
        BigDecimal expectancy = winRateFraction.multiply(averageWin)
                .add(lossRate.multiply(averageLoss));

        return PerformanceStats.builder()
                .totalClosedPositions(total)
                .wins(wins)
                .losses(losses)
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
        BigDecimal runningSize = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO; // total cost basis for average price
        BigDecimal realisedPnl = BigDecimal.ZERO;

        for (Trade trade : trades) {
            if (current == null) {
                // Start a new position
                current = Position.builder()
                        .market(trade.getMarket())
                        .code(trade.getCode())
                        .side(trade.getSide())
                        .openDate(trade.getDate())
                        .trades(new ArrayList<>())
                        .build();
                runningSize = BigDecimal.ZERO;
                totalCost = BigDecimal.ZERO;
                realisedPnl = BigDecimal.ZERO;
            }

            current.getTrades().add(trade);
            boolean adding = trade.getSide() == current.getSide();

            if (adding) {
                // Adding to position
                totalCost = totalCost.add(trade.getSize().multiply(trade.getPrice()));
                runningSize = runningSize.add(trade.getSize());
            } else {
                // Reducing position
                BigDecimal avgPrice = totalCost.divide(runningSize, MathContext.DECIMAL128);
                BigDecimal pnl;
                if (current.getSide() == Side.BUY) {
                    pnl = trade.getPrice().subtract(avgPrice).multiply(trade.getSize());
                } else {
                    pnl = avgPrice.subtract(trade.getPrice()).multiply(trade.getSize());
                }
                realisedPnl = realisedPnl.add(pnl);
                runningSize = runningSize.subtract(trade.getSize());
                totalCost = runningSize.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : avgPrice.multiply(runningSize);
            }

            BigDecimal avgEntryPrice = runningSize.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : totalCost.divide(runningSize, MathContext.DECIMAL128);

            if (runningSize.compareTo(BigDecimal.ZERO) == 0) {
                // Position closed
                current.setSize(BigDecimal.ZERO);
                current.setAveragePrice(avgEntryPrice);
                current.setRealisedPnl(realisedPnl);
                current.setCloseDate(trade.getDate());
                current.setOpen(false);
                positions.add(current);
                current = null;
            } else {
                // Position still open
                current.setSize(runningSize);
                current.setAveragePrice(avgEntryPrice);
                current.setRealisedPnl(realisedPnl);
                current.setOpen(true);
            }
        }

        // If there's a remaining open position
        if (current != null) {
            positions.add(current);
        }

        return positions;
    }
}
