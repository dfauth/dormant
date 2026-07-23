package io.github.dfauth.trade.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static io.github.dfauth.trade.model.PositionPredicate.*;
import static org.junit.jupiter.api.Assertions.*;

class PositionPredicateTest {

    private static final LocalDate DATE = LocalDate.of(2024, 1, 1);

    private Trade buyTrade(int size) {
        return Trade.builder()
                .date(DATE).market("ASX").code("BHP")
                .side(Side.BUY).size(size)
                .price(BigDecimal.TEN).cost(BigDecimal.TEN.multiply(BigDecimal.valueOf(size)))
                .build();
    }

    private Trade sellTrade(int size) {
        return Trade.builder()
                .date(DATE.plusDays(1)).market("ASX").code("BHP")
                .side(Side.SELL).size(size)
                .price(BigDecimal.TEN).cost(BigDecimal.TEN.multiply(BigDecimal.valueOf(size)))
                .build();
    }

    private Position openLong() {
        return Position.of(buyTrade(100));
    }

    private Position closedLong() {
        return Position.of(buyTrade(100)).addTrade(sellTrade(100));
    }

    private Position openShort() {
        return Position.of(sellTrade(100));
    }

    private Position closedShort() {
        return Position.of(sellTrade(100)).addTrade(buyTrade(100));
    }

    // --- OPEN ---

    @Test
    void open_matchesOpenLongPosition() {
        assertTrue(OPEN.get().test(openLong()));
    }

    @Test
    void open_matchesOpenShortPosition() {
        assertTrue(OPEN.get().test(openShort()));
    }

    @Test
    void open_doesNotMatchClosedLongPosition() {
        assertFalse(OPEN.get().test(closedLong()));
    }

    @Test
    void open_doesNotMatchClosedShortPosition() {
        assertFalse(OPEN.get().test(closedShort()));
    }

    // --- CLOSED ---

    @Test
    void closed_matchesClosedLongPosition() {
        assertTrue(CLOSED.get().test(closedLong()));
    }

    @Test
    void closed_matchesClosedShortPosition() {
        assertTrue(CLOSED.get().test(closedShort()));
    }

    @Test
    void closed_doesNotMatchOpenLongPosition() {
        assertFalse(CLOSED.get().test(openLong()));
    }

    @Test
    void closed_doesNotMatchOpenShortPosition() {
        assertFalse(CLOSED.get().test(openShort()));
    }

    // --- SHORT ---

    @Test
    void short_matchesOpenShortPosition() {
        assertTrue(SHORT.get().test(openShort()));
    }

    @Test
    void short_matchesClosedShortPosition() {
        assertTrue(SHORT.get().test(closedShort()));
    }

    @Test
    void short_doesNotMatchLongPosition() {
        assertFalse(SHORT.get().test(openLong()));
    }

    // --- LONG ---

    @Test
    void long_matchesOpenLongPosition() {
        assertTrue(LONG.get().test(openLong()));
    }

    @Test
    void long_matchesClosedLongPosition() {
        assertTrue(LONG.get().test(closedLong()));
    }

    @Test
    void long_doesNotMatchShortPosition() {
        assertFalse(LONG.get().test(openShort()));
    }

    // --- OPEN and CLOSED are complements ---

    @Test
    void open_andClosed_areComplements_forLong() {
        Position p = openLong();
        assertNotEquals(OPEN.get().test(p), CLOSED.get().test(p));
    }

    @Test
    void open_andClosed_areComplements_forShort() {
        Position p = closedShort();
        assertNotEquals(OPEN.get().test(p), CLOSED.get().test(p));
    }

    // --- SHORT and LONG are complements ---

    @Test
    void short_andLong_areComplements() {
        Position longPos = openLong();
        Position shortPos = openShort();
        assertNotEquals(SHORT.get().test(longPos), LONG.get().test(longPos));
        assertNotEquals(SHORT.get().test(shortPos), LONG.get().test(shortPos));
    }

    // --- implements Predicate (can compose) ---

    @Test
    void predicates_canBeComposed_openAndLong() {
        java.util.function.Predicate<Position> openLongPredicate = OPEN.and(LONG);
        assertTrue(openLongPredicate.test(openLong()));
        assertFalse(openLongPredicate.test(closedLong()));
        assertFalse(openLongPredicate.test(openShort()));
    }

    @Test
    void predicates_canBeComposed_closedOrShort() {
        java.util.function.Predicate<Position> closedOrShort = CLOSED.or(SHORT);
        assertTrue(closedOrShort.test(closedLong()));
        assertTrue(closedOrShort.test(openShort()));
        assertFalse(closedOrShort.test(openLong()));
    }
}
