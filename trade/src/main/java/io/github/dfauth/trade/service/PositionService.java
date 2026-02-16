package io.github.dfauth.trade.service;

import io.github.dfauth.trade.model.Position;
import io.github.dfauth.trade.model.Side;
import io.github.dfauth.trade.model.Trade;
import io.github.dfauth.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
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
