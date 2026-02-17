package io.github.dfauth.trade.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static io.github.dfauth.trade.model.DatetimeFormats.YYYYMMDD;
import static io.github.dfauth.trade.model.TenorRange.Direction.BACKWARD;
import static io.github.dfauth.trade.model.TenorRange.Direction.FORWARD;
import static java.util.Optional.empty;
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
        TenorRange range = new TenorRange(2, Tenor.Y, ANCHOR, FORWARD);
        assertEquals(ANCHOR, range.start());
        assertEquals(LocalDate.of(2027, 6, 15), range.end());
    }

    @Test
    void forwardMonths() {
        TenorRange range = new TenorRange(6, Tenor.M, ANCHOR, FORWARD);
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

    static Stream<Arguments> tenorCombinations() {
        var start = LocalDate.of(2025, 1, 1);
        var end = LocalDate.of(2026, 1, 1);
        Optional<String>[] tenors = options("6M");
        Optional<String>[] starts = options(YYYYMMDD.format(start));
        Optional<String>[] ends = options(YYYYMMDD.format(end));
        Optional<DateRange>[] expected = new Optional[]{
                empty(),
                empty(),
                empty(),
                Optional.of(DateRange.of(start,end)),
                Optional.of(new TenorRange(6, Tenor.M, LocalDate.now(), BACKWARD)),
                Optional.of(new TenorRange(6, Tenor.M, end, BACKWARD)),
                Optional.of(new TenorRange(6, Tenor.M, start, FORWARD)),
                Optional.of(DateRange.of(start, end)),
        };
        int[] i = new int[]{0};
        return Stream.of(tenors)
                .flatMap(t -> Stream.of(starts)
                        .flatMap(s -> Stream.of(ends)
                                .map(e -> Arguments.of(t, s, e, expected[i[0]++]))));
    }

    private static Optional<String>[] options(String value) {
        return new Optional[]{empty(), Optional.of(value)};
    }

    @ParameterizedTest
    @MethodSource("tenorCombinations")
    void testPriority(Optional<String> tenor, Optional<String> startFrom, Optional<String> endAt, Optional<DateRange> expected) {
        assertEquals(expected, DateRange.resolve(tenor, startFrom, endAt));
    }
}
