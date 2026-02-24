package io.github.dfauth.trade.service;

import io.github.dfauth.trade.model.*;
import io.github.dfauth.trade.repository.PaymentRepository;
import io.github.dfauth.trade.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionServiceRepositoryTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PositionService service;

    private static final Long USER_ID = 1L;
    private static final String MARKET = "ASX";

    private static final LocalDate D1 = LocalDate.of(2024, 1, 1);
    private static final LocalDate D2 = LocalDate.of(2024, 2, 1);
    private static final LocalDate D3 = LocalDate.of(2024, 3, 1);

    @BeforeEach
    void stubDividendsEmpty() {
        // Default: no dividends — keeps existing tests passing without modification.
        // Individual tests override these stubs as needed.
        lenient().when(paymentRepository.findByUserIdAndCodeAndTransactionTypeOrderByDateAsc(
                any(), any(), eq(TransactionType.DIV))).thenReturn(List.of());
        lenient().when(paymentRepository.findByUserIdAndMarketAndTransactionTypeOrderByDateAsc(
                any(), any(), eq(TransactionType.DIV))).thenReturn(List.of());
        lenient().when(paymentRepository.findByUserIdAndTransactionTypeOrderByDateAsc(
                any(), eq(TransactionType.DIV))).thenReturn(List.of());
    }

    private Trade trade(String code, LocalDate date, Side side, int size, String price) {
        BigDecimal p = new BigDecimal(price);
        return Trade.builder()
                .market(MARKET)
                .code(code)
                .date(date)
                .side(side)
                .size(size)
                .price(p)
                .cost(p.multiply(BigDecimal.valueOf(size)))
                .confirmationId("conf-" + code + "-" + date + "-" + side)
                .userId(USER_ID)
                .build();
    }

    // --- getPositions(userId, market, code) ---

    @Test
    void getPositions_noTrades_returnsEmpty() {
        when(tradeRepository.findByUserIdAndMarketAndCodeOrderByDateAsc(USER_ID, MARKET, "BHP"))
                .thenReturn(List.of());

        List<Position> result = service.getPositions(USER_ID, MARKET, "BHP");

        assertTrue(result.isEmpty());
        verify(tradeRepository).findByUserIdAndMarketAndCodeOrderByDateAsc(USER_ID, MARKET, "BHP");
    }

    @Test
    void getPositions_closedPosition_returnsClosed() {
        when(tradeRepository.findByUserIdAndMarketAndCodeOrderByDateAsc(USER_ID, MARKET, "BHP"))
                .thenReturn(List.of(
                        trade("BHP", D1, Side.BUY,  100, "10.00"),
                        trade("BHP", D2, Side.SELL, 100, "15.00")
                ));

        List<Position> result = service.getPositions(USER_ID, MARKET, "BHP");

        assertEquals(1, result.size());
        assertFalse(result.get(0).isOpen());
    }

    @Test
    void getPositions_sequentialPositions_returnsAll() {
        when(tradeRepository.findByUserIdAndMarketAndCodeOrderByDateAsc(USER_ID, MARKET, "BHP"))
                .thenReturn(List.of(
                        trade("BHP", D1, Side.BUY,  100, "10.00"),
                        trade("BHP", D2, Side.SELL, 100, "15.00"),
                        trade("BHP", D3, Side.BUY,   50, "20.00")
                ));

        List<Position> result = service.getPositions(USER_ID, MARKET, "BHP");

        assertEquals(2, result.size());
        assertFalse(result.get(0).isOpen());
        assertTrue(result.get(1).isOpen());
    }

    // --- getOpenPositions(userId) / getPositions(userId, predicate) ---

    @Test
    void getOpenPositions_noTrades_returnsEmpty() {
        when(tradeRepository.findByUserId(USER_ID)).thenReturn(List.of());

        assertTrue(service.getOpenPositions(USER_ID).isEmpty());
    }

    @Test
    void getOpenPositions_allOpen_returnsAll() {
        when(tradeRepository.findByUserId(USER_ID)).thenReturn(List.of(
                trade("BHP", D1, Side.BUY, 100, "10.00"),
                trade("ANZ", D1, Side.BUY, 200, "25.00")
        ));

        List<Position> result = service.getOpenPositions(USER_ID);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(Position::isOpen));
    }

    @Test
    void getOpenPositions_closedPositionsExcluded() {
        // BHP fully closed, ANZ open
        when(tradeRepository.findByUserId(USER_ID)).thenReturn(List.of(
                trade("BHP", D1, Side.BUY,  100, "10.00"),
                trade("BHP", D2, Side.SELL, 100, "15.00"),
                trade("ANZ", D1, Side.BUY,  200, "25.00")
        ));

        List<Position> result = service.getOpenPositions(USER_ID);

        assertEquals(1, result.size());
        assertEquals("ANZ", result.get(0).getCode());
    }

    @Test
    void getPositions_customPredicate_filtersCorrectly() {
        // BHP is long (BUY-first), ANZ is short (SELL-first)
        when(tradeRepository.findByUserId(USER_ID)).thenReturn(List.of(
                trade("BHP", D1, Side.BUY,  100, "10.00"),
                trade("ANZ", D1, Side.SELL, 200, "50.00")
        ));

        List<Position> result = service.getPositions(USER_ID.longValue(), Position::isShort);

        assertEquals(1, result.size());
        assertEquals("ANZ", result.get(0).getCode());
        assertTrue(result.get(0).isShort());
    }

    // --- getPositionsByMarket(userId, market) ---

    @Test
    void getPositionsByMarket_noTrades_returnsEmpty() {
        when(tradeRepository.findByUserIdAndMarketOrderByDateAsc(USER_ID, MARKET))
                .thenReturn(List.of());

        assertTrue(service.getPositionsByMarket(USER_ID, MARKET).isEmpty());
    }

    @Test
    void getPositionsByMarket_singleCode_buildsPositions() {
        when(tradeRepository.findByUserIdAndMarketOrderByDateAsc(USER_ID, MARKET))
                .thenReturn(List.of(
                        trade("BHP", D1, Side.BUY,  100, "10.00"),
                        trade("BHP", D2, Side.SELL, 100, "15.00")
                ));

        List<Position> result = service.getPositionsByMarket(USER_ID, MARKET);

        assertEquals(1, result.size());
        assertEquals("BHP", result.get(0).getCode());
        assertFalse(result.get(0).isOpen());
    }

    @Test
    void getPositionsByMarket_multipleCodes_eachCodeBuiltSeparately() {
        when(tradeRepository.findByUserIdAndMarketOrderByDateAsc(USER_ID, MARKET))
                .thenReturn(List.of(
                        trade("BHP", D1, Side.BUY, 100, "10.00"),
                        trade("ANZ", D1, Side.BUY, 200, "25.00")
                ));

        List<Position> result = service.getPositionsByMarket(USER_ID, MARKET);

        assertEquals(2, result.size());
        List<String> codes = result.stream().map(Position::getCode).toList();
        assertTrue(codes.contains("BHP"));
        assertTrue(codes.contains("ANZ"));
    }

    @Test
    void getPositionsByMarket_codeOrderFollowsFirstTradeAppearance() {
        // distinct() preserves encounter order: BHP, CBA, ANZ
        when(tradeRepository.findByUserIdAndMarketOrderByDateAsc(USER_ID, MARKET))
                .thenReturn(List.of(
                        trade("BHP", D1, Side.BUY,  100, "10.00"),
                        trade("CBA", D1, Side.BUY,   50, "100.00"),
                        trade("ANZ", D2, Side.BUY,  200, "25.00")
                ));

        List<Position> result = service.getPositionsByMarket(USER_ID, MARKET);

        assertEquals(3, result.size());
        assertEquals("BHP", result.get(0).getCode());
        assertEquals("CBA", result.get(1).getCode());
        assertEquals("ANZ", result.get(2).getCode());
    }

    @Test
    void getPositionsByMarket_sequentialPositionsSameCode() {
        when(tradeRepository.findByUserIdAndMarketOrderByDateAsc(USER_ID, MARKET))
                .thenReturn(List.of(
                        trade("BHP", D1, Side.BUY,  100, "10.00"),
                        trade("BHP", D2, Side.SELL, 100, "15.00"),
                        trade("BHP", D3, Side.BUY,   50, "20.00")
                ));

        List<Position> result = service.getPositionsByMarket(USER_ID, MARKET);

        assertEquals(2, result.size());
        assertFalse(result.get(0).isOpen()); // closed
        assertTrue(result.get(1).isOpen());  // open
    }

    // --- dividend assignment (assignDividends) ---

    private Payment div(String code, LocalDate paymentDate, LocalDate exDivDate, String value) {
        return Payment.builder()
                .transactionType(TransactionType.DIV)
                .market(MARKET)
                .code(code)
                .date(paymentDate)
                .exDividendDate(exDivDate)
                .value(new BigDecimal(value))
                .balance(BigDecimal.ZERO)
                .detail("Dividend")
                .userId(USER_ID)
                .build();
    }

    @Test
    void dividends_paymentDateWithinClosedPosition_accumulated() {
        // Position: BUY Jan 1, SELL Mar 1. Dividend payment date Feb 1 (within range).
        when(tradeRepository.findByUserIdAndMarketAndCodeOrderByDateAsc(USER_ID, MARKET, "BHP"))
                .thenReturn(List.of(
                        trade("BHP", D1, Side.BUY,  100, "10.00"),
                        trade("BHP", D3, Side.SELL, 100, "15.00")
                ));
        when(paymentRepository.findByUserIdAndCodeAndTransactionTypeOrderByDateAsc(
                USER_ID,"ASX:BHP", TransactionType.DIV))
                .thenReturn(List.of(div("BHP", D2, null, "120.00")));

        List<Position> result = service.getPositions(USER_ID, MARKET, "BHP");

        assertEquals(1, result.size());
        assertEquals(120.0, result.get(0).getDividends().doubleValue(), 1e-6);
    }

    @Test
    void dividends_exDivDateWithinPosition_paymentDateAfterClose_assignedToClosedPosition() {
        // Position closes Mar 1. Ex-div Feb 1 (within), payment Apr 1 (after close).
        // Ex-div date takes precedence → dividend belongs to closed position.
        LocalDate D4 = LocalDate.of(2024, 4, 1);
        when(tradeRepository.findByUserIdAndMarketAndCodeOrderByDateAsc(USER_ID, MARKET, "BHP"))
                .thenReturn(List.of(
                        trade("BHP", D1, Side.BUY,  100, "10.00"),
                        trade("BHP", D3, Side.SELL, 100, "15.00")
                ));
        when(paymentRepository.findByUserIdAndCodeAndTransactionTypeOrderByDateAsc(
                USER_ID,"ASX:BHP", TransactionType.DIV))
                .thenReturn(List.of(div("BHP", D4, D2, "75.00")));

        List<Position> result = service.getPositions(USER_ID, MARKET, "BHP");

        assertEquals(1, result.size());
        assertFalse(result.get(0).isOpen());
        assertEquals(75.0, result.get(0).getDividends().doubleValue(), 1e-6);
    }

    @Test
    void dividends_paymentDateAfterClose_noExDiv_notAssigned() {
        // Position closes Mar 1. Payment Apr 1, no ex-div date → not assigned.
        LocalDate D4 = LocalDate.of(2024, 4, 1);
        when(tradeRepository.findByUserIdAndMarketAndCodeOrderByDateAsc(USER_ID, MARKET, "BHP"))
                .thenReturn(List.of(
                        trade("BHP", D1, Side.BUY,  100, "10.00"),
                        trade("BHP", D3, Side.SELL, 100, "15.00")
                ));
        when(paymentRepository.findByUserIdAndCodeAndTransactionTypeOrderByDateAsc(
                USER_ID, "ASX:BHP", TransactionType.DIV))
                .thenReturn(List.of(div("BHP", D4, null, "75.00")));

        List<Position> result = service.getPositions(USER_ID, MARKET, "BHP");

        assertEquals(1, result.size());
        assertEquals(0.0, result.get(0).getDividends().doubleValue(), 1e-6);
    }

    @Test
    void dividends_openPosition_accumulatesMultipleDividends() {
        LocalDate D4 = LocalDate.of(2024, 4, 1);
        when(tradeRepository.findByUserIdAndMarketAndCodeOrderByDateAsc(USER_ID, MARKET, "BHP"))
                .thenReturn(List.of(trade("BHP", D1, Side.BUY, 100, "10.00")));
        when(paymentRepository.findByUserIdAndCodeAndTransactionTypeOrderByDateAsc(
                USER_ID, "ASX:BHP", TransactionType.DIV))
                .thenReturn(List.of(
                        div("BHP", D2, null, "50.00"),
                        div("BHP", D4, null, "60.00")
                ));

        List<Position> result = service.getPositions(USER_ID, MARKET, "BHP");

        assertEquals(1, result.size());
        assertTrue(result.get(0).isOpen());
        assertEquals(110.0, result.get(0).getDividends().doubleValue(), 1e-6);
    }

    @Test
    void dividends_routedToCorrectPositionOfTwo() {
        // Two sequential positions. Dividend Feb 1 → first (closed). Dividend Apr 1 → second (open).
        LocalDate D4 = LocalDate.of(2024, 4, 1);
        when(tradeRepository.findByUserIdAndMarketAndCodeOrderByDateAsc(USER_ID, MARKET, "BHP"))
                .thenReturn(List.of(
                        trade("BHP", D1, Side.BUY,  100, "10.00"),
                        trade("BHP", D3, Side.SELL, 100, "15.00"),
                        trade("BHP", D3, Side.BUY,   50, "20.00")
                ));
        when(paymentRepository.findByUserIdAndCodeAndTransactionTypeOrderByDateAsc(
                USER_ID, "ASX:BHP", TransactionType.DIV))
                .thenReturn(List.of(
                        div("BHP", D2, null, "40.00"),   // Feb 1 → first position (Jan–Mar)
                        div("BHP", D4, null, "30.00")    // Apr 1 → second position (Mar–open)
                ));

        List<Position> result = service.getPositions(USER_ID, MARKET, "BHP");

        assertEquals(2, result.size());
        assertEquals(40.0, result.get(0).getDividends().doubleValue(), 1e-6); // closed
        assertEquals(30.0, result.get(1).getDividends().doubleValue(), 1e-6); // open
    }

    @Test
    void dividends_noDividends_remainZero() {
        when(tradeRepository.findByUserIdAndMarketAndCodeOrderByDateAsc(USER_ID, MARKET, "BHP"))
                .thenReturn(List.of(
                        trade("BHP", D1, Side.BUY,  100, "10.00"),
                        trade("BHP", D2, Side.SELL, 100, "15.00")
                ));
        // paymentRepository stub already returns List.of() from @BeforeEach

        List<Position> result = service.getPositions(USER_ID, MARKET, "BHP");

        assertEquals(1, result.size());
        assertEquals(0.0, result.get(0).getDividends().doubleValue(), 1e-6);
    }

    // --- getPerformanceStats(userId, market) ---

    @Test
    void getPerformanceStats_noTrades_returnsZeroStats() {
        when(tradeRepository.findByUserIdAndMarketOrderByDateAsc(USER_ID, MARKET))
                .thenReturn(List.of());

        PerformanceStats stats = service.getPerformanceStats(USER_ID, MARKET);

        assertEquals(0, stats.getTotalClosedPositions());
        assertEquals(0, stats.getWins());
        assertEquals(0, stats.getLosses());
        assertEquals(0.0, stats.getWinRate(), 1e-9);
    }

    @Test
    void getPerformanceStats_openPositionsExcluded() {
        // BHP: closed win (+500); ANZ: open (excluded)
        when(tradeRepository.findByUserIdAndMarketOrderByDateAsc(USER_ID, MARKET))
                .thenReturn(List.of(
                        trade("BHP", D1, Side.BUY,  100, "10.00"),
                        trade("BHP", D2, Side.SELL, 100, "15.00"),
                        trade("ANZ", D3, Side.BUY,   50, "20.00")
                ));

        PerformanceStats stats = service.getPerformanceStats(USER_ID, MARKET);

        assertEquals(1, stats.getTotalClosedPositions());
        assertEquals(1, stats.getWins());
        assertEquals(0, stats.getLosses());
        assertEquals(1.0, stats.getWinRate(), 1e-9);
    }

    @Test
    void getPerformanceStats_delegatesToGetPositionsByMarket() {
        when(tradeRepository.findByUserIdAndMarketOrderByDateAsc(USER_ID, MARKET))
                .thenReturn(List.of(
                        trade("BHP", D1, Side.BUY,  100, "10.00"),
                        trade("BHP", D2, Side.SELL, 100, "15.00"),
                        trade("ANZ", D1, Side.BUY,  200, "30.00"),
                        trade("ANZ", D2, Side.SELL, 200, "25.00")
                ));

        PerformanceStats stats = service.getPerformanceStats(USER_ID, MARKET);

        assertEquals(2, stats.getTotalClosedPositions());
        assertEquals(1, stats.getWins());   // BHP: +500
        assertEquals(1, stats.getLosses()); // ANZ: -1000
        verify(tradeRepository).findByUserIdAndMarketOrderByDateAsc(USER_ID, MARKET);
    }
}
