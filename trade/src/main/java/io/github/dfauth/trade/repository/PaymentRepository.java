package io.github.dfauth.trade.repository;

import io.github.dfauth.trade.model.Payment;
import io.github.dfauth.trade.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserIdOrderByDateAsc(Long userId);

    List<Payment> findByUserIdAndTransactionTypeOrderByDateAsc(Long userId, TransactionType transactionType);

    List<Payment> findByUserIdAndCodeOrderByDateAsc(Long userId, String code);

    @Query("SELECT p FROM Payment p WHERE p.userId = :userId AND p.date >= :start AND p.date <= :end ORDER BY p.date ASC")
    List<Payment> findByUserIdAndDateRange(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT p FROM Payment p WHERE p.userId = :userId AND p.code = :code AND p.date >= :start AND p.date <= :end ORDER BY p.date ASC")
    List<Payment> findByUserIdAndCodeAndDateRange(@Param("userId") Long userId, @Param("code") String code, @Param("start") LocalDate start, @Param("end") LocalDate end);

    Optional<Payment> findByIdAndUserId(Long id, Long userId);
}
