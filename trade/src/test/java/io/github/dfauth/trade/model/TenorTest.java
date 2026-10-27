package io.github.dfauth.trade.model;

import io.github.dfauth.dormant.model.Tenor;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.github.dfauth.dormant.model.Tenor.TenorUnit.*;
import static io.github.dfauth.dormant.model.Tenor._1Y;
import static io.github.dfauth.trade.model.DateRange.of;
import static org.junit.jupiter.api.Assertions.*;

class TenorTest {

    private static final LocalDate ANCHOR = LocalDate.of(2025, 6, 15);

    @Test
    void parseYears() {
        Tenor tenor = Tenor.parse("2Y");
        assertNotNull(tenor);
        assertEquals(2, tenor.n());
        assertEquals(Y, tenor.u());
    }

    @Test
    void parseMonths() {
        Tenor tenor = Tenor.parse("6M");
        assertEquals(6, tenor.n());
        assertEquals(M, tenor.u());
    }

    @Test
    void parseDays() {
        Tenor tenor = Tenor.parse("30D");
        assertEquals(30, tenor.n());
        assertEquals(D, tenor.u());
    }

    @Test
    void parseMultiDigitNumber() {
        Tenor tenor = Tenor.parse("12M");
        assertEquals(12, tenor.n());
        assertEquals(M, tenor.u());
    }

    @Test
    void backwardYears() {
        DateRange dateRange = of(new Tenor(2, Y).endAt(ANCHOR), ANCHOR);
        assertEquals(LocalDate.of(2023, 6, 15), dateRange.start());
        assertEquals(ANCHOR, dateRange.end());
    }

    @Test
    void backwardMonths() {
        DateRange dateRange = of(new Tenor(3, M).endAt(ANCHOR), ANCHOR);
        assertEquals(LocalDate.of(2025, 3, 15), dateRange.start());
        assertEquals(ANCHOR, dateRange.end());
    }

    @Test
    void backwardDays() {
        DateRange dateRange = of(new Tenor(10, D).endAt(ANCHOR), ANCHOR);
        assertEquals(LocalDate.of(2025, 6, 5), dateRange.start());
        assertEquals(ANCHOR, dateRange.end());
    }

    @Test
    void forwardYears() {
        DateRange dateRange = of(ANCHOR, new Tenor(2, Y).startFrom(ANCHOR));
        assertEquals(ANCHOR, dateRange.start());
        assertEquals(LocalDate.of(2027, 6, 15), dateRange.end());
    }

    @Test
    void forwardMonths() {
        DateRange dateRange = of(ANCHOR, new Tenor(6, M).startFrom(ANCHOR));
        assertEquals(ANCHOR, dateRange.start());
        assertEquals(LocalDate.of(2025, 12, 15), dateRange.end());
    }

    @Test
    void toLocalDates() {
        DateRange dateRange = of(_1Y.endAt(ANCHOR), ANCHOR);
        assertEquals(LocalDate.of(2024, 6, 15), dateRange.start());
        assertEquals(ANCHOR, dateRange.end());
    }

    @Test
    void defaultDirectionIsBackward() {
        DateRange dateRange = of(_1Y.endAt(ANCHOR), ANCHOR);
        assertEquals(LocalDate.of(2024, 6, 15), dateRange.start());
        assertEquals(ANCHOR, dateRange.end());
    }

    @Test
    void parseInvalidInputThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Tenor.parse("abc"));
        assertTrue(ex.getMessage().contains("abc"));
    }

    @Test
    void parseEmptyInputThrows() {
        assertThrows(IllegalArgumentException.class, () -> Tenor.parse(""));
    }

}
