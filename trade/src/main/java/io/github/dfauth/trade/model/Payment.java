package io.github.dfauth.trade.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = true)
    private String market;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(name = "payment_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = Integer.MAX_VALUE)
    private String detail;

    @Column(name = "payment_value", nullable = false, precision = 19, scale = 6)
    private BigDecimal value;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal balance;

    @Column(nullable = true)
    private String side;

    @Column(nullable = true)
    private String code;

    @Column(name = "contract_number", nullable = true)
    private String contractNumber;

    @Column(name = "ex_dividend_date", nullable = true)
    private LocalDate exDividendDate;
}
