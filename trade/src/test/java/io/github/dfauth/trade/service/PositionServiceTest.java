package io.github.dfauth.trade.service;

import io.github.dfauth.trade.model.PerformanceStats;
import io.github.dfauth.trade.model.Position;
import io.github.dfauth.trade.model.Side;
import io.github.dfauth.trade.model.Trade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PositionServiceTest {

    private final PositionService service = new PositionService(null);

    private Trade trade(LocalDate date, Side side, String size, String price) {
        return Trade.builder()
                .date(date)
                .market("ASX")
                .code("BHP")
                .side(side)
                .size(new BigDecimal(size))
                .price(new BigDecimal(price))
                .cost(BigDecimal.ZERO)
                .confirmationId("conf-" + date + "-" + side + "-" + size)
                .build();
    }

    @Test
    void singleLongPosition_buyTradesOnly() {
        List<Trade> trades = List.of(
                trade(LocalDate.of(2024, 1, 1), Side.BUY, "100", "10.00"),
                trade(LocalDate.of(2024, 1, 2), Side.BUY, "50", "12.00")
        );

        List<Position> positions = service.buildPositions(trades);

        assertEquals(1, positions.size());
        Position p = positions.get(0);
        assertTrue(p.isOpen());
        assertEquals(Side.BUY, p.getSide());
        assertEquals(0, new BigDecimal("150").compareTo(p.getSize()));
        // avg price = (100*10 + 50*12) / 150 = 1600/150 ≈ 10.6667
        BigDecimal expectedAvg = new BigDecimal("1600").divide(new BigDecimal("150"), java.math.MathContext.DECIMAL128);
        assertEquals(0, expectedAvg.compareTo(p.getAveragePrice()));
        assertEquals(0, BigDecimal.ZERO.compareTo(p.getRealisedPnl()));
        assertEquals(LocalDate.of(2024, 1, 1), p.getOpenDate());
        assertNull(p.getCloseDate());
        assertEquals(2, p.getTrades().size());
    }

    @Test
    void longPositionOpenedAndClosed() {
        List<Trade> trades = List.of(
                trade(LocalDate.of(2024, 1, 1), Side.BUY, "100", "10.00"),
                trade(LocalDate.of(2024, 2, 1), Side.SELL, "100", "15.00")
        );

        List<Position> positions = service.buildPositions(trades);

        assertEquals(1, positions.size());
        Position p = positions.get(0);
        assertFalse(p.isOpen());
        assertEquals(Side.BUY, p.getSide());
        assertEquals(0, BigDecimal.ZERO.compareTo(p.getSize()));
        // PnL = (15 - 10) * 100 = 500
        assertEquals(0, new BigDecimal("500").compareTo(p.getRealisedPnl()));
        assertEquals(LocalDate.of(2024, 1, 1), p.getOpenDate());
        assertEquals(LocalDate.of(2024, 2, 1), p.getCloseDate());
    }

    @Test
    void shortPosition_sellFirstThenBuyToClose() {
        List<Trade> trades = List.of(
                trade(LocalDate.of(2024, 1, 1), Side.SELL, "200", "50.00"),
                trade(LocalDate.of(2024, 3, 1), Side.BUY, "200", "45.00")
        );

        List<Position> positions = service.buildPositions(trades);

        assertEquals(1, positions.size());
        Position p = positions.get(0);
        assertFalse(p.isOpen());
        assertEquals(Side.SELL, p.getSide());
        // Short PnL = (50 - 45) * 200 = 1000
        assertEquals(0, new BigDecimal("1000").compareTo(p.getRealisedPnl()));
    }

    @Test
    void multipleSequentialPositions() {
        List<Trade> trades = List.of(
                // First position: buy 100 @ 10, sell 100 @ 15
                trade(LocalDate.of(2024, 1, 1), Side.BUY, "100", "10.00"),
                trade(LocalDate.of(2024, 2, 1), Side.SELL, "100", "15.00"),
                // Second position: buy 50 @ 20 (still open)
                trade(LocalDate.of(2024, 3, 1), Side.BUY, "50", "20.00")
        );

        List<Position> positions = service.buildPositions(trades);

        assertEquals(2, positions.size());

        Position first = positions.get(0);
        assertFalse(first.isOpen());
        assertEquals(0, new BigDecimal("500").compareTo(first.getRealisedPnl()));
        assertEquals(LocalDate.of(2024, 2, 1), first.getCloseDate());

        Position second = positions.get(1);
        assertTrue(second.isOpen());
        assertEquals(0, new BigDecimal("50").compareTo(second.getSize()));
        assertEquals(0, new BigDecimal("20.00").compareTo(second.getAveragePrice()));
        assertNull(second.getCloseDate());
    }

    @Test
    void openPositionDetection() {
        List<Trade> trades = List.of(
                trade(LocalDate.of(2024, 1, 1), Side.BUY, "100", "10.00"),
                trade(LocalDate.of(2024, 1, 5), Side.SELL, "60", "12.00")
        );

        List<Position> positions = service.buildPositions(trades);

        assertEquals(1, positions.size());
        Position p = positions.get(0);
        assertTrue(p.isOpen());
        assertEquals(0, new BigDecimal("40").compareTo(p.getSize()));
        // Realised PnL from partial close: (12 - 10) * 60 = 120
        assertEquals(0, new BigDecimal("120").compareTo(p.getRealisedPnl()));
        assertEquals(0, new BigDecimal("10.00").compareTo(p.getAveragePrice()));
    }

    @Test
    void emptyTradeList() {
        List<Position> positions = service.buildPositions(List.of());
        assertTrue(positions.isEmpty());
    }

    // --- Performance stats tests ---

    @Test
    void performanceStats_allWinningPositions() {
        List<Trade> trades = List.of(
                // Position 1: buy 100 @ 10, sell 100 @ 15 → PnL = 500
                trade(LocalDate.of(2024, 1, 1), Side.BUY, "100", "10.00"),
                trade(LocalDate.of(2024, 2, 1), Side.SELL, "100", "15.00"),
                // Position 2: buy 200 @ 20, sell 200 @ 25 → PnL = 1000
                trade(LocalDate.of(2024, 3, 1), Side.BUY, "200", "20.00"),
                trade(LocalDate.of(2024, 4, 1), Side.SELL, "200", "25.00")
        );

        List<Position> positions = service.buildPositions(trades);
        PerformanceStats stats = service.computePerformanceStats(positions);

        assertEquals(2, stats.getTotalClosedPositions());
        assertEquals(2, stats.getWins());
        assertEquals(0, stats.getLosses());
        assertEquals(0, new BigDecimal("100").compareTo(stats.getWinRate()));
        // averageWin = (500 + 1000) / 2 = 750
        assertEquals(0, new BigDecimal("750").compareTo(stats.getAverageWin()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getAverageLoss()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getRiskRewardRatio()));
        // expectancy = 1.0 * 750 + 0.0 * 0 = 750
        assertEquals(0, new BigDecimal("750").compareTo(stats.getExpectancy()));
    }

    @Test
    void performanceStats_mixedWinsAndLosses() {
        List<Trade> trades = List.of(
                // Position 1 (win): buy 100 @ 10, sell 100 @ 20 → PnL = 1000
                trade(LocalDate.of(2024, 1, 1), Side.BUY, "100", "10.00"),
                trade(LocalDate.of(2024, 2, 1), Side.SELL, "100", "20.00"),
                // Position 2 (loss): buy 100 @ 30, sell 100 @ 25 → PnL = -500
                trade(LocalDate.of(2024, 3, 1), Side.BUY, "100", "30.00"),
                trade(LocalDate.of(2024, 4, 1), Side.SELL, "100", "25.00")
        );

        List<Position> positions = service.buildPositions(trades);
        PerformanceStats stats = service.computePerformanceStats(positions);

        assertEquals(2, stats.getTotalClosedPositions());
        assertEquals(1, stats.getWins());
        assertEquals(1, stats.getLosses());
        // winRate = 50%
        assertEquals(0, new BigDecimal("50").compareTo(stats.getWinRate()));
        // averageWin = 1000
        assertEquals(0, new BigDecimal("1000").compareTo(stats.getAverageWin()));
        // averageLoss = -500
        assertEquals(0, new BigDecimal("-500").compareTo(stats.getAverageLoss()));
        // riskRewardRatio = 1000 / 500 = 2
        assertEquals(0, new BigDecimal("2").compareTo(stats.getRiskRewardRatio()));
        // expectancy = 0.5 * 1000 + 0.5 * (-500) = 500 - 250 = 250
        assertEquals(0, new BigDecimal("250").compareTo(stats.getExpectancy()));
    }

    @Test
    void performanceStats_noClosedPositions() {
        List<Trade> trades = List.of(
                trade(LocalDate.of(2024, 1, 1), Side.BUY, "100", "10.00")
        );

        List<Position> positions = service.buildPositions(trades);
        PerformanceStats stats = service.computePerformanceStats(positions);

        assertEquals(0, stats.getTotalClosedPositions());
        assertEquals(0, stats.getWins());
        assertEquals(0, stats.getLosses());
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getWinRate()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getAverageWin()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getAverageLoss()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getRiskRewardRatio()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getExpectancy()));
    }

    @Test
    void performanceStats_allLosingPositions() {
        List<Trade> trades = List.of(
                // Position 1: buy 100 @ 20, sell 100 @ 15 → PnL = -500
                trade(LocalDate.of(2024, 1, 1), Side.BUY, "100", "20.00"),
                trade(LocalDate.of(2024, 2, 1), Side.SELL, "100", "15.00"),
                // Position 2: buy 100 @ 30, sell 100 @ 22 → PnL = -800
                trade(LocalDate.of(2024, 3, 1), Side.BUY, "100", "30.00"),
                trade(LocalDate.of(2024, 4, 1), Side.SELL, "100", "22.00")
        );

        List<Position> positions = service.buildPositions(trades);
        PerformanceStats stats = service.computePerformanceStats(positions);

        assertEquals(2, stats.getTotalClosedPositions());
        assertEquals(0, stats.getWins());
        assertEquals(2, stats.getLosses());
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getWinRate()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getAverageWin()));
        // averageLoss = (-500 + -800) / 2 = -650
        assertEquals(0, new BigDecimal("-650").compareTo(stats.getAverageLoss()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getRiskRewardRatio()));
        // expectancy = 0 * 0 + 1.0 * (-650) = -650
        assertEquals(0, new BigDecimal("-650").compareTo(stats.getExpectancy()));
    }

    @Test
    void performanceStats_breakevenPositionCountsAsLoss() {
        List<Trade> trades = List.of(
                // Breakeven: buy 100 @ 10, sell 100 @ 10 → PnL = 0
                trade(LocalDate.of(2024, 1, 1), Side.BUY, "100", "10.00"),
                trade(LocalDate.of(2024, 2, 1), Side.SELL, "100", "10.00")
        );

        List<Position> positions = service.buildPositions(trades);
        PerformanceStats stats = service.computePerformanceStats(positions);

        assertEquals(1, stats.getTotalClosedPositions());
        assertEquals(0, stats.getWins());
        assertEquals(1, stats.getLosses());
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getWinRate()));
    }

    @Test
    void performanceStats_openPositionsExcluded() {
        List<Trade> trades = List.of(
                // Closed position (win): buy 100 @ 10, sell 100 @ 20 → PnL = 1000
                trade(LocalDate.of(2024, 1, 1), Side.BUY, "100", "10.00"),
                trade(LocalDate.of(2024, 2, 1), Side.SELL, "100", "20.00"),
                // Open position: buy 50 @ 30 (no close)
                trade(LocalDate.of(2024, 3, 1), Side.BUY, "50", "30.00")
        );

        List<Position> positions = service.buildPositions(trades);
        assertEquals(2, positions.size());

        PerformanceStats stats = service.computePerformanceStats(positions);

        assertEquals(1, stats.getTotalClosedPositions());
        assertEquals(1, stats.getWins());
        assertEquals(0, stats.getLosses());
    }

    @Test
    void performanceStats_shortPositionWinAndLoss() {
        List<Trade> trades = List.of(
                // Short win: sell 100 @ 50, buy 100 @ 40 → PnL = 1000
                trade(LocalDate.of(2024, 1, 1), Side.SELL, "100", "50.00"),
                trade(LocalDate.of(2024, 2, 1), Side.BUY, "100", "40.00"),
                // Short loss: sell 100 @ 30, buy 100 @ 38 → PnL = -800
                trade(LocalDate.of(2024, 3, 1), Side.SELL, "100", "30.00"),
                trade(LocalDate.of(2024, 4, 1), Side.BUY, "100", "38.00")
        );

        List<Position> positions = service.buildPositions(trades);
        PerformanceStats stats = service.computePerformanceStats(positions);

        assertEquals(2, stats.getTotalClosedPositions());
        assertEquals(1, stats.getWins());
        assertEquals(1, stats.getLosses());
        assertEquals(0, new BigDecimal("50").compareTo(stats.getWinRate()));
        assertEquals(0, new BigDecimal("1000").compareTo(stats.getAverageWin()));
        assertEquals(0, new BigDecimal("-800").compareTo(stats.getAverageLoss()));
        // riskRewardRatio = 1000 / 800 = 1.25
        assertEquals(0, new BigDecimal("1.25").compareTo(stats.getRiskRewardRatio()));
        // expectancy = 0.5 * 1000 + 0.5 * (-800) = 100
        assertEquals(0, new BigDecimal("100").compareTo(stats.getExpectancy()));
    }

    @Test
    void performanceStats_manyPositionsWinRate() {
        List<Trade> trades = List.of(
                // Win 1: buy 100 @ 10, sell 100 @ 12 → PnL = 200
                trade(LocalDate.of(2024, 1, 1), Side.BUY, "100", "10.00"),
                trade(LocalDate.of(2024, 1, 10), Side.SELL, "100", "12.00"),
                // Win 2: buy 100 @ 10, sell 100 @ 13 → PnL = 300
                trade(LocalDate.of(2024, 2, 1), Side.BUY, "100", "10.00"),
                trade(LocalDate.of(2024, 2, 10), Side.SELL, "100", "13.00"),
                // Loss 1: buy 100 @ 10, sell 100 @ 8 → PnL = -200
                trade(LocalDate.of(2024, 3, 1), Side.BUY, "100", "10.00"),
                trade(LocalDate.of(2024, 3, 10), Side.SELL, "100", "8.00")
        );

        List<Position> positions = service.buildPositions(trades);
        PerformanceStats stats = service.computePerformanceStats(positions);

        assertEquals(3, stats.getTotalClosedPositions());
        assertEquals(2, stats.getWins());
        assertEquals(1, stats.getLosses());
        // winRate = 2/3 * 100 ≈ 66.6666666667
        assertTrue(stats.getWinRate().compareTo(new BigDecimal("66")) > 0);
        assertTrue(stats.getWinRate().compareTo(new BigDecimal("67")) < 0);
        // averageWin = (200 + 300) / 2 = 250
        assertEquals(0, new BigDecimal("250").compareTo(stats.getAverageWin()));
        // averageLoss = -200
        assertEquals(0, new BigDecimal("-200").compareTo(stats.getAverageLoss()));
        // riskRewardRatio = 250 / 200 = 1.25
        assertEquals(0, new BigDecimal("1.25").compareTo(stats.getRiskRewardRatio()));
    }

    @Test
    void performanceStats_emptyPositionsList() {
        PerformanceStats stats = service.computePerformanceStats(List.of());

        assertEquals(0, stats.getTotalClosedPositions());
        assertEquals(0, stats.getWins());
        assertEquals(0, stats.getLosses());
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getWinRate()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getExpectancy()));
    }
}
