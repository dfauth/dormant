package io.github.dfauth.trade.service;

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
}
