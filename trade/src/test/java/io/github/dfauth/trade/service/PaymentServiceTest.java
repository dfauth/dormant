package io.github.dfauth.trade.service;

import io.github.dfauth.trade.model.DateRange;
import io.github.dfauth.trade.model.Payment;
import io.github.dfauth.trade.model.TransactionType;
import io.github.dfauth.trade.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService service;

    private static final Long USER_ID = 1L;
    private static final LocalDate D1 = LocalDate.of(2024, 1, 1);
    private static final LocalDate D2 = LocalDate.of(2024, 6, 30);

    private Payment payment(TransactionType type, String code) {
        return Payment.builder()
                .transactionType(type)
                .date(D1)
                .detail("Test payment")
                .value(new BigDecimal("250.00"))
                .balance(new BigDecimal("10000.00"))
                .side("CREDIT")
                .code(code)
                .userId(USER_ID)
                .build();
    }

    // --- create ---

    @Test
    void create_setsUserIdAndDelegatesToRepository() {
        Payment input = payment(TransactionType.DIV, "BHP");
        input.setUserId(null);
        when(paymentRepository.save(input)).thenReturn(input);

        service.create(input, USER_ID);

        assertEquals(USER_ID, input.getUserId());
        verify(paymentRepository).save(input);
    }

    // --- createBatch ---

    @Test
    void createBatch_setsUserIdOnAllBeforeSaving() {
        Payment p1 = payment(TransactionType.DIV, "BHP");
        Payment p2 = payment(TransactionType.INT, null);
        p1.setUserId(null);
        p2.setUserId(null);
        when(paymentRepository.saveAll(List.of(p1, p2))).thenReturn(List.of(p1, p2));

        List<Payment> result = service.createBatch(List.of(p1, p2), USER_ID);

        assertEquals(USER_ID, p1.getUserId());
        assertEquals(USER_ID, p2.getUserId());
        assertEquals(2, result.size());
    }

    // --- findById ---

    @Test
    void findById_delegatesToRepository() {
        Payment p = payment(TransactionType.DIV, "BHP");
        when(paymentRepository.findByIdAndUserId(42L, USER_ID)).thenReturn(Optional.of(p));

        Optional<Payment> result = service.findById(42L, USER_ID);

        assertTrue(result.isPresent());
        verify(paymentRepository).findByIdAndUserId(42L, USER_ID);
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(paymentRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertTrue(service.findById(99L, USER_ID).isEmpty());
    }

    // --- getPayments — no filters ---

    @Test
    void getPayments_noFilters_fetchesAll() {
        List<Payment> all = List.of(payment(TransactionType.DIV, "BHP"), payment(TransactionType.INT, null));
        when(paymentRepository.findByUserIdOrderByDateAsc(USER_ID)).thenReturn(all);

        List<Payment> result = service.getPayments(USER_ID, Optional.empty(), Optional.empty(), Optional.empty());

        assertEquals(2, result.size());
        verify(paymentRepository).findByUserIdOrderByDateAsc(USER_ID);
    }

    @Test
    void getPayments_noFilters_returnsEmptyWhenNoPayments() {
        when(paymentRepository.findByUserIdOrderByDateAsc(USER_ID)).thenReturn(List.of());

        assertTrue(service.getPayments(USER_ID, Optional.empty(), Optional.empty(), Optional.empty()).isEmpty());
    }

    // --- getPayments — code filter ---

    @Test
    void getPayments_codeOnly_filtersByCode() {
        List<Payment> divs = List.of(payment(TransactionType.DIV, "BHP"));
        when(paymentRepository.findByUserIdAndCodeOrderByDateAsc(USER_ID, "BHP")).thenReturn(divs);

        List<Payment> result = service.getPayments(USER_ID, Optional.of("BHP"), Optional.empty(), Optional.empty());

        assertEquals(1, result.size());
        verify(paymentRepository).findByUserIdAndCodeOrderByDateAsc(USER_ID, "BHP");
        verify(paymentRepository, never()).findByUserIdOrderByDateAsc(any());
    }

    // --- getPayments — transactionType filter ---

    @Test
    void getPayments_typeOnly_filtersByTransactionType() {
        List<Payment> divs = List.of(payment(TransactionType.DIV, "BHP"));
        when(paymentRepository.findByUserIdAndTransactionTypeOrderByDateAsc(USER_ID, TransactionType.DIV)).thenReturn(divs);

        List<Payment> result = service.getPayments(USER_ID, Optional.empty(), Optional.of(TransactionType.DIV), Optional.empty());

        assertEquals(1, result.size());
        verify(paymentRepository).findByUserIdAndTransactionTypeOrderByDateAsc(USER_ID, TransactionType.DIV);
        verify(paymentRepository, never()).findByUserIdOrderByDateAsc(any());
    }

    // --- getPayments — date range filter ---

    @Test
    void getPayments_dateRangeOnly_filtersByDateRange() {
        DateRange range = DateRange.of(D1, D2);
        List<Payment> payments = List.of(payment(TransactionType.INT, null));
        when(paymentRepository.findByUserIdAndDateRange(USER_ID, D1, D2)).thenReturn(payments);

        List<Payment> result = service.getPayments(USER_ID, Optional.empty(), Optional.empty(), Optional.of(range));

        assertEquals(1, result.size());
        verify(paymentRepository).findByUserIdAndDateRange(USER_ID, D1, D2);
        verify(paymentRepository, never()).findByUserIdOrderByDateAsc(any());
    }

    // --- getPayments — code + date range (highest priority) ---

    @Test
    void getPayments_codeAndDateRange_usesCodeAndDateRangeQuery() {
        DateRange range = DateRange.of(D1, D2);
        List<Payment> payments = List.of(payment(TransactionType.DIV, "BHP"));
        when(paymentRepository.findByUserIdAndCodeAndDateRange(USER_ID, "BHP", D1, D2)).thenReturn(payments);

        List<Payment> result = service.getPayments(USER_ID, Optional.of("BHP"), Optional.empty(), Optional.of(range));

        assertEquals(1, result.size());
        verify(paymentRepository).findByUserIdAndCodeAndDateRange(USER_ID, "BHP", D1, D2);
        verify(paymentRepository, never()).findByUserIdAndCodeOrderByDateAsc(eq(USER_ID), any());
        verify(paymentRepository, never()).findByUserIdOrderByDateAsc(any());
    }

    @Test
    void getPayments_codeAndDateRange_returnsEmptyWhenNoMatch() {
        DateRange range = DateRange.of(D1, D2);
        when(paymentRepository.findByUserIdAndCodeAndDateRange(USER_ID, "ANZ", D1, D2)).thenReturn(List.of());

        assertTrue(service.getPayments(USER_ID, Optional.of("ANZ"), Optional.empty(), Optional.of(range)).isEmpty());
    }
}
