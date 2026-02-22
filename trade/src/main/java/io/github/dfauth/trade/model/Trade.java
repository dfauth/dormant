package io.github.dfauth.trade.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

import static io.github.dfauth.trycatch.Utils.bd;

@Entity
@Table(name = "trades")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String market;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Side side;

    @Column(nullable = false)
    private int size;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal price;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal cost;

    @Column(name = "confirmation_id", nullable = false, unique = true)
    private String confirmationId;

    @Column(nullable = true, length = Integer.MAX_VALUE)
    private String notes;

    public BigDecimal getCommission() {
        return cost.subtract(price.multiply(bd(size)));
    }
}
