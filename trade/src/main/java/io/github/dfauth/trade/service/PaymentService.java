package io.github.dfauth.trade.service;

import io.github.dfauth.trade.model.DateRange;
import io.github.dfauth.trade.model.Payment;
import io.github.dfauth.trade.model.TransactionType;
import io.github.dfauth.trade.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment create(Payment payment, Long userId) {
        payment.setUserId(userId);
        return paymentRepository.save(payment);
    }

    @Transactional
    public List<Payment> createBatch(List<Payment> payments, Long userId) {
        payments.forEach(p -> p.setUserId(userId));
        return paymentRepository.saveAll(payments);
    }

    public Optional<Payment> findById(Long id, Long userId) {
        return paymentRepository.findByIdAndUserId(id, userId);
    }

    public List<Payment> getPayments(Long userId, Optional<String> code, Optional<TransactionType> transactionType, Optional<DateRange> dateRange) {
        if (code.isPresent() && dateRange.isPresent()) {
            return paymentRepository.findByUserIdAndCodeAndDateRange(userId, code.get(), dateRange.get().start(), dateRange.get().end());
        }
        if (code.isPresent()) {
            return paymentRepository.findByUserIdAndCodeOrderByDateAsc(userId, code.get());
        }
        if (transactionType.isPresent()) {
            return paymentRepository.findByUserIdAndTransactionTypeOrderByDateAsc(userId, transactionType.get());
        }
        if (dateRange.isPresent()) {
            return paymentRepository.findByUserIdAndDateRange(userId, dateRange.get().start(), dateRange.get().end());
        }
        return paymentRepository.findByUserIdOrderByDateAsc(userId);
    }
}
