package io.github.dfauth.trade.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TenorRangeTest {

    private static final LocalDate ANCHOR = LocalDate.of(2025, 6, 15);

    @Test
    void parseYears() {
        TenorRange range = TenorRange.parse("2Y");
        assertNotNull(range);
        assertEquals(2, range.amount());
        assertEquals(Tenor.Y, range.unit());
    }

    @Test
    void parseMonths() {
        TenorRange range = TenorRange.parse("6M");
        assertEquals(6, range.amount());
        assertEquals(Tenor.M, range.unit());
    }

    @Test
    void parseDays() {
        TenorRange range = TenorRange.parse("30D");
        assertEquals(30, range.amount());
        assertEquals(Tenor.D, range.unit());
    }

    @Test
    void parseMultiDigitNumber() {
        TenorRange range = TenorRange.parse("12M");
        assertEquals(12, range.amount());
        assertEquals(Tenor.M, range.unit());
    }

    @Test
    void backwardYears() {
        TenorRange range = new TenorRange(2, Tenor.Y, ANCHOR, TenorRange.Direction.BACKWARD);
        assertEquals(LocalDate.of(2023, 6, 15), range.start());
        assertEquals(ANCHOR, range.end());
    }

    @Test
    void backwardMonths() {
        TenorRange range = new TenorRange(3, Tenor.M, ANCHOR, TenorRange.Direction.BACKWARD);
        assertEquals(LocalDate.of(2025, 3, 15), range.start());
        assertEquals(ANCHOR, range.end());
    }

    @Test
    void backwardDays() {
        TenorRange range = new TenorRange(10, Tenor.D, ANCHOR, TenorRange.Direction.BACKWARD);
        assertEquals(LocalDate.of(2025, 6, 5), range.start());
        assertEquals(ANCHOR, range.end());
    }

    @Test
    void forwardYears() {
        TenorRange range = new TenorRange(2, Tenor.Y, ANCHOR, TenorRange.Direction.FORWARD);
        assertEquals(ANCHOR, range.start());
        assertEquals(LocalDate.of(2027, 6, 15), range.end());
    }

    @Test
    void forwardMonths() {
        TenorRange range = new TenorRange(6, Tenor.M, ANCHOR, TenorRange.Direction.FORWARD);
        assertEquals(ANCHOR, range.start());
        assertEquals(LocalDate.of(2025, 12, 15), range.end());
    }

    @Test
    void toLocalDates() {
        TenorRange range = new TenorRange(1, Tenor.Y, ANCHOR, TenorRange.Direction.BACKWARD);
        LocalDate[] dates = range.toLocalDates();
        assertEquals(2, dates.length);
        assertEquals(LocalDate.of(2024, 6, 15), dates[0]);
        assertEquals(ANCHOR, dates[1]);
    }

    @Test
    void defaultDirectionIsBackward() {
        TenorRange range = new TenorRange(1, Tenor.Y, ANCHOR);
        assertEquals(TenorRange.Direction.BACKWARD, range.direction());
        assertEquals(LocalDate.of(2024, 6, 15), range.start());
        assertEquals(ANCHOR, range.end());
    }

    @Test
    void parseInvalidInputThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> TenorRange.parse("abc"));
        assertTrue(ex.getMessage().contains("abc"));
    }

    @Test
    void parseEmptyInputThrows() {
        assertThrows(IllegalArgumentException.class, () -> TenorRange.parse(""));
    }
}
