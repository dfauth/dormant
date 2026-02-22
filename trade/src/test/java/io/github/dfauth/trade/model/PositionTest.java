package io.github.dfauth.trade.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PositionTest {

    private static final LocalDate D1 = LocalDate.of(2024, 1, 1);
    private static final LocalDate D2 = LocalDate.of(2024, 2, 1);
    private static final LocalDate D3 = LocalDate.of(2024, 3, 1);

    private Trade trade(String code, LocalDate date, Side side, int size, String price) {
        BigDecimal p = new BigDecimal(price);
        BigDecimal cost = p.multiply(BigDecimal.valueOf(size));
        return Trade.builder()
                .market("ASX")
                .code(code)
                .date(date)
                .side(side)
                .size(size)
                .price(p)
                .cost(cost)
                .confirmationId("conf-" + code + "-" + date + "-" + side + "-" + size)
                .build();
    }

    private Trade bhp(LocalDate date, Side side, int size, String price) {
        return trade("BHP", date, side, size, price);
    }

    // --- Position.of() ---

    @Test
    void of_setsMarketAndCodeFromTrade() {
        Trade t = bhp(D1, Side.BUY, 100, "10.00");
        Position p = Position.of(t);
        assertEquals("ASX", p.getMarket());
        assertEquals("BHP", p.getCode());
    }

    @Test
    void of_addsSingleTradeToPosition() {
        Trade t = bhp(D1, Side.BUY, 100, "10.00");
        Position p = Position.of(t);
        assertEquals(1, p.getTrades().size());
        assertSame(t, p.getTrades().get(0));
    }

    @Test
    void of_createsOpenPosition() {
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        assertTrue(p.isOpen());
    }

    // --- addTrade() ---

    @Test
    void addTrade_appendsTradeToList() {
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        Trade second = bhp(D2, Side.BUY, 50, "12.00");
        p.addTrade(second);
        assertEquals(2, p.getTrades().size());
        assertSame(second, p.getTrades().get(1));
    }

    @Test
    void addTrade_returnsThis_forFluentChaining() {
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        Position returned = p.addTrade(bhp(D2, Side.BUY, 50, "12.00"));
        assertSame(p, returned);
    }

    // --- isShort() ---

    @Test
    void isShort_falseForLongPosition() {
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        assertFalse(p.isShort());
    }

    @Test
    void isShort_trueForShortPosition() {
        Position p = Position.of(bhp(D1, Side.SELL, 100, "50.00"));
        assertTrue(p.isShort());
    }

    // --- getSide() ---

    @Test
    void getSide_returnsFirstTradeSide() {
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        p.addTrade(bhp(D2, Side.SELL, 50, "15.00")); // partial close — first trade is still BUY
        assertEquals(Side.BUY, p.getSide());
    }

    // --- getOpenDate() ---

    @Test
    void getOpenDate_returnsFirstTradeDate() {
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        p.addTrade(bhp(D2, Side.BUY, 50, "12.00"));
        assertEquals(D1, p.getOpenDate());
    }

    // --- getCloseDate() ---

    @Test
    void getCloseDate_emptyWhenPositionIsOpen() {
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        assertTrue(p.getCloseDate().isEmpty());
    }

    @Test
    void getCloseDate_presentWithLastTradeDateWhenClosed() {
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        p.addTrade(bhp(D2, Side.SELL, 100, "15.00"));
        assertEquals(D2, p.getCloseDate().get());
    }

    // --- isClosed() / isOpen() ---

    @Test
    void isClosed_trueWhenNetSizeIsZero() {
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        p.addTrade(bhp(D2, Side.SELL, 100, "15.00"));
        assertTrue(p.isClosed());
        assertFalse(p.isOpen());
    }

    @Test
    void isOpen_trueWhenNetSizeIsNonZero() {
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        p.addTrade(bhp(D2, Side.SELL, 40, "15.00"));
        assertTrue(p.isOpen());
        assertFalse(p.isClosed());
    }

    // --- getSize() ---

    @Test
    void getSize_withBuyTradesOnly() {
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        p.addTrade(bhp(D2, Side.BUY, 50, "12.00"));
        assertEquals(150, p.getSize());
    }

    @Test
    void getSize_reducedByPartialSell() {
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        p.addTrade(bhp(D2, Side.SELL, 40, "15.00"));
        assertEquals(60, p.getSize());
    }

    @Test
    void getSize_zeroAfterFullClose() {
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        p.addTrade(bhp(D2, Side.SELL, 100, "15.00"));
        assertEquals(0, p.getSize());
    }

    // --- getAveragePrice() ---

    @Test
    void getAveragePrice_singleTrade_returnsTradePrice() {
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        assertEquals(0, new BigDecimal("10.00").compareTo(p.getAveragePrice()));
    }

    @Test
    void getAveragePrice_multipleBuys_returnsWeightedAverage() {
        // (100*10 + 50*12) / 150 = 1600/150 ≈ 10.67
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        p.addTrade(bhp(D2, Side.BUY, 50, "12.00"));
        assertEquals(1600.0 / 150.0, p.getAveragePrice().doubleValue(), 1e-2);
    }

    @Test
    void getAveragePrice_ignoresOppositeSideTrades() {
        // Partial close: sell trades should not affect average buy price
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        p.addTrade(bhp(D2, Side.SELL, 40, "15.00"));
        assertEquals(0, new BigDecimal("10.00").compareTo(p.getAveragePrice()));
    }

    // --- getRealisedPnl() — also exercises SideSizeCost.add() / merge() / calculate() ---

    @Test
    void realisedPnl_openPositionBuyOnly_isZero() {
        // Only one side in the map → tmp.size() == 1 → returns ZERO
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        p.addTrade(bhp(D2, Side.BUY, 50, "12.00"));
        assertEquals(0, BigDecimal.ZERO.compareTo(p.getRealisedPnl()));
    }

    @Test
    void realisedPnl_openPositionSellOnly_isZero() {
        Position p = Position.of(bhp(D1, Side.SELL, 100, "50.00"));
        assertEquals(0, BigDecimal.ZERO.compareTo(p.getRealisedPnl()));
    }

    @Test
    void realisedPnl_closedLong_correctPnl() {
        // PnL = sum of signed costs: BUY.signed(1000) + SELL.signed(1500) = -1000 + 1500 = 500
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        p.addTrade(bhp(D2, Side.SELL, 100, "15.00"));
        assertEquals(0, new BigDecimal("500.00").compareTo(p.getRealisedPnl()));
    }

    @Test
    void realisedPnl_closedShort_correctPnl() {
        // Sell 100 @ 50 (cost=5000), buy 100 @ 45 (cost=4500)
        // PnL = SELL.signed(5000) + BUY.signed(4500) = 5000 - 4500 = 500
        Position p = Position.of(bhp(D1, Side.SELL, 100, "50.00"));
        p.addTrade(bhp(D2, Side.BUY, 100, "45.00"));
        assertEquals(0, new BigDecimal("500.00").compareTo(p.getRealisedPnl()));
    }

    @Test
    void realisedPnl_partialCloseLong_exercisesMergeAndCalculate() {
        // Buy 100 @ 10 (cost=1000), sell 40 @ 15 (cost=600) — still open (size=60)
        // calculate(l=SELL(40,600), r=BUY(100,1000)):
        //   cost = SELL.signed(600) + (40/100)*BUY.signed(1000) = 600 + 0.4*(-1000) = 200
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        p.addTrade(bhp(D2, Side.SELL, 40, "15.00"));
        assertTrue(p.isOpen());
        assertEquals(200.0, p.getRealisedPnl().doubleValue(), 1e-2);
    }

    @Test
    void realisedPnl_partialCloseShort_exercisesMergeAndCalculate() {
        // Sell 100 @ 50 (cost=5000), buy 30 @ 45 (cost=1350) — still open (size=70)
        // calculate(l=BUY(30,1350), r=SELL(100,5000)):
        //   cost = BUY.signed(1350) + (30/100)*SELL.signed(5000) = -1350 + 0.3*5000 = 150
        Position p = Position.of(bhp(D1, Side.SELL, 100, "50.00"));
        p.addTrade(bhp(D2, Side.BUY, 30, "45.00"));
        assertTrue(p.isOpen());
        assertEquals(150.0, p.getRealisedPnl().doubleValue(), 1e-2);
    }

    @Test
    void realisedPnl_multipleBuysThenPartialSell_exercisesAddAndMerge() {
        // Buy 100 @ 10 (cost=1000), buy 50 @ 12 (cost=600), sell 60 @ 15 (cost=900) — open (size=90)
        // add(): BUY 100+50 @ 1000+600 = BUY(150, 1600)
        // calculate(l=SELL(60,900), r=BUY(150,1600)):
        //   cost = SELL.signed(900) + (60/150)*BUY.signed(1600) = 900 + 0.4*(-1600) = 260
        // Manual: avg buy = 1600/150, PnL = (15 - 1600/150) * 60 = 260
        Position p = Position.of(bhp(D1, Side.BUY, 100, "10.00"));
        p.addTrade(bhp(D2, Side.BUY, 50, "12.00"));
        p.addTrade(bhp(D3, Side.SELL, 60, "15.00"));
        assertTrue(p.isOpen());
        assertEquals(260.0, p.getRealisedPnl().doubleValue(), 1e-2);
    }
}
