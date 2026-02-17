package io.github.dfauth.trade.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.TemporalAccessor;

import static java.time.temporal.ChronoField.*;
import static org.junit.jupiter.api.Assertions.*;

class DatetimeFormatsTest {

    @Test
    void parseYYYYMMDD() {
        TemporalAccessor result = DatetimeFormats.YYYYMMDD.parse("20250615");
        assertEquals(2025, result.get(YEAR));
        assertEquals(6, result.get(MONTH_OF_YEAR));
        assertEquals(15, result.get(DAY_OF_MONTH));
    }

    @Test
    void parseToLocalDate() {
        TemporalAccessor result = DatetimeFormats.YYYYMMDD.parse("20231201");
        LocalDate date = LocalDate.from(result);
        assertEquals(LocalDate.of(2023, 12, 1), date);
    }

    @Test
    void toLocalDate() {
        LocalDate date = DatetimeFormats.YYYYMMDD.toLocalDate("20250615");
        assertEquals(LocalDate.of(2025, 6, 15), date);
    }

    @Test
    void parseLeapYearDate() {
        TemporalAccessor result = DatetimeFormats.YYYYMMDD.parse("20240229");
        LocalDate date = LocalDate.from(result);
        assertEquals(LocalDate.of(2024, 2, 29), date);
    }

    @Test
    void parseFirstDayOfYear() {
        TemporalAccessor result = DatetimeFormats.YYYYMMDD.parse("20250101");
        LocalDate date = LocalDate.from(result);
        assertEquals(LocalDate.of(2025, 1, 1), date);
    }

    @Test
    void parseLastDayOfYear() {
        TemporalAccessor result = DatetimeFormats.YYYYMMDD.parse("20251231");
        LocalDate date = LocalDate.from(result);
        assertEquals(LocalDate.of(2025, 12, 31), date);
    }

    @Test
    void usedAsTenorRangeAnchorWithStartFrom() {
        LocalDate anchor = DatetimeFormats.YYYYMMDD.toLocalDate("20250615");
        TenorRange range = TenorRange.parse("2Y").startFrom(anchor);
        assertEquals(anchor, range.start());
        assertEquals(LocalDate.of(2027, 6, 15), range.end());
    }

    @Test
    void usedAsTenorRangeAnchorWithEndAt() {
        LocalDate anchor = DatetimeFormats.YYYYMMDD.toLocalDate("20250615");
        TenorRange range = TenorRange.parse("6M").endAt(anchor);
        assertEquals(LocalDate.of(2024, 12, 15), range.start());
        assertEquals(anchor, range.end());
    }

    @Test
    void invalidInputThrows() {
        assertThrows(Exception.class, () -> DatetimeFormats.YYYYMMDD.parse("not-a-date"));
    }
}
