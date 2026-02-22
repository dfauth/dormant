package io.github.dfauth.trade.utils;

import io.github.dfauth.trade.model.Position;
import io.github.dfauth.trade.model.Side;
import io.github.dfauth.trade.model.Trade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PositionCollectorTest {

    private final PositionCollector collector = new PositionCollector();

    private Trade trade(String market, String code, LocalDate date, Side side, int size, String price) {
        BigDecimal p = new BigDecimal(price);
        BigDecimal cost = p.multiply(BigDecimal.valueOf(size));
        return Trade.builder()
                .market(market)
                .code(code)
                .date(date)
                .side(side)
                .size(size)
                .price(p)
                .cost(cost)
                .confirmationId("conf-" + market + "-" + code + "-" + date + "-" + side + "-" + size)
                .build();
    }

    private Trade asx(String code, LocalDate date, Side side, int size, String price) {
        return trade("ASX", code, date, side, size, price);
    }

    // --- Key.compareTo() ---

    @Test
    void key_equalCodeAndDate_returnsZero() {
        var k1 = new PositionCollector.Key("ASX:BHP", LocalDate.of(2024, 1, 1));
        var k2 = new PositionCollector.Key("ASX:BHP", LocalDate.of(2024, 1, 1));
        assertEquals(0, k1.compareTo(k2));
    }

    @Test
    void key_orderedByDateFirst() {
        var earlier = new PositionCollector.Key("ASX:BHP", LocalDate.of(2024, 1, 1));
        var later   = new PositionCollector.Key("ASX:BHP", LocalDate.of(2024, 2, 1));
        assertTrue(earlier.compareTo(later) < 0);
        assertTrue(later.compareTo(earlier) > 0);
    }

    @Test
    void key_sameDateOrderedAlphabeticallyByCode() {
        var anz = new PositionCollector.Key("ASX:ANZ", LocalDate.of(2024, 1, 1));
        var bhp = new PositionCollector.Key("ASX:BHP", LocalDate.of(2024, 1, 1));
        assertTrue(anz.compareTo(bhp) < 0);
        assertTrue(bhp.compareTo(anz) > 0);
    }

    // --- characteristics() ---

    @Test
    void characteristics_isEmpty() {
        assertTrue(collector.characteristics().isEmpty());
    }

    // --- Collector behaviour ---

    @Test
    void emptyStream_returnsEmptyCollection() {
        Collection<Position> result = Stream.<Trade>empty().collect(collector);
        assertTrue(result.isEmpty());
    }

    @Test
    void singleBuyTrade_returnsOneOpenPosition() {
        Trade t = asx("BHP", LocalDate.of(2024, 1, 1), Side.BUY, 100, "10.00");

        Collection<Position> result = Stream.of(t).collect(collector);

        assertEquals(1, result.size());
        Position p = result.iterator().next();
        assertTrue(p.isOpen());
        assertEquals(Side.BUY, p.getSide());
        assertEquals(100, p.getSize());
        assertEquals("ASX", p.getMarket());
        assertEquals("BHP", p.getCode());
        assertEquals(1, p.getTrades().size());
    }

    @Test
    void twoBuyTradesForSameCode_groupedIntoOneOpenPosition() {
        Collection<Position> result = Stream.of(
                asx("BHP", LocalDate.of(2024, 1, 1), Side.BUY, 100, "10.00"),
                asx("BHP", LocalDate.of(2024, 1, 5), Side.BUY,  50, "12.00")
        ).collect(collector);

        assertEquals(1, result.size());
        Position p = result.iterator().next();
        assertTrue(p.isOpen());
        assertEquals(150, p.getSize());
        assertEquals(2, p.getTrades().size());
    }

    @Test
    void buyThenFullSell_closesPosition() {
        Collection<Position> result = Stream.of(
                asx("BHP", LocalDate.of(2024, 1, 1), Side.BUY,  100, "10.00"),
                asx("BHP", LocalDate.of(2024, 2, 1), Side.SELL, 100, "15.00")
        ).collect(collector);

        assertEquals(1, result.size());
        Position p = result.iterator().next();
        assertFalse(p.isOpen());
        assertEquals(0, p.getSize());
        assertEquals(2, p.getTrades().size());
    }

    @Test
    void buyThenPartialSell_positionRemainsOpen() {
        Collection<Position> result = Stream.of(
                asx("BHP", LocalDate.of(2024, 1, 1), Side.BUY,  100, "10.00"),
                asx("BHP", LocalDate.of(2024, 2, 1), Side.SELL,  40, "15.00")
        ).collect(collector);

        assertEquals(1, result.size());
        Position p = result.iterator().next();
        assertTrue(p.isOpen());
        assertEquals(60, p.getSize());
    }

    @Test
    void buyThenCloseThenBuyAgain_createsTwoSeparatePositions() {
        Collection<Position> result = Stream.of(
                asx("BHP", LocalDate.of(2024, 1, 1), Side.BUY,  100, "10.00"),
                asx("BHP", LocalDate.of(2024, 2, 1), Side.SELL, 100, "15.00"),
                asx("BHP", LocalDate.of(2024, 3, 1), Side.BUY,   50, "20.00")
        ).collect(collector);

        assertEquals(2, result.size());
        List<Position> positions = new ArrayList<>(result);
        assertFalse(positions.get(0).isOpen());
        assertTrue(positions.get(1).isOpen());
        assertEquals(50, positions.get(1).getSize());
    }

    @Test
    void tradesForDifferentCodes_createSeparatePositions() {
        Collection<Position> result = Stream.of(
                asx("BHP", LocalDate.of(2024, 1, 1), Side.BUY, 100, "10.00"),
                asx("ANZ", LocalDate.of(2024, 1, 1), Side.BUY, 200, "25.00")
        ).collect(collector);

        assertEquals(2, result.size());
        Map<String, Position> byCode = result.stream()
                .collect(Collectors.toMap(Position::getCode, p -> p));
        assertEquals(100, byCode.get("BHP").getSize());
        assertEquals(200, byCode.get("ANZ").getSize());
    }

    @Test
    void tradesForDifferentMarkets_sameCode_createSeparatePositions() {
        Collection<Position> result = Stream.of(
                trade("ASX",  "BHP", LocalDate.of(2024, 1, 1), Side.BUY, 100, "10.00"),
                trade("NYSE", "BHP", LocalDate.of(2024, 1, 1), Side.BUY, 200, "10.00")
        ).collect(collector);

        assertEquals(2, result.size());
        Map<String, Position> byMarket = result.stream()
                .collect(Collectors.toMap(Position::getMarket, p -> p));
        assertEquals(100, byMarket.get("ASX").getSize());
        assertEquals(200, byMarket.get("NYSE").getSize());
    }

    @Test
    void positions_orderedByOpeningDate() {
        // Trades arrive out of date order; result should be sorted by open date
        Collection<Position> result = Stream.of(
                asx("BHP", LocalDate.of(2024, 3, 1), Side.BUY,  50, "20.00"),
                asx("ANZ", LocalDate.of(2024, 1, 1), Side.BUY, 100, "25.00"),
                asx("CBA", LocalDate.of(2024, 2, 1), Side.BUY,  75, "110.00")
        ).collect(collector);

        List<LocalDate> openDates = result.stream()
                .map(Position::getOpenDate)
                .collect(Collectors.toList());

        assertEquals(List.of(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 3, 1)
        ), openDates);
    }

    // --- combiner() ---

    @Test
    void combiner_mergesDisjointMaps() {
        Trade t1 = asx("BHP", LocalDate.of(2024, 1, 1), Side.BUY, 100, "10.00");
        Trade t2 = asx("ANZ", LocalDate.of(2024, 2, 1), Side.BUY, 200, "25.00");

        Map<PositionCollector.Key, Position> left = new TreeMap<>();
        left.put(new PositionCollector.Key("ASX:BHP", t1.getDate()), Position.of(t1));

        Map<PositionCollector.Key, Position> right = new TreeMap<>();
        right.put(new PositionCollector.Key("ASX:ANZ", t2.getDate()), Position.of(t2));

        Map<PositionCollector.Key, Position> merged = collector.combiner().apply(left, right);

        assertEquals(2, merged.size());
        assertTrue(merged.values().stream().anyMatch(p -> p.getCode().equals("BHP")));
        assertTrue(merged.values().stream().anyMatch(p -> p.getCode().equals("ANZ")));
    }
}
