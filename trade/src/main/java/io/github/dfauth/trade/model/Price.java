package io.github.dfauth.trade.model;

import io.github.dfauth.ta.Candle;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "prices", uniqueConstraints = @UniqueConstraint(columnNames = {"market", "code", "date"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Price implements Candle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String market;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal open;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal high;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal low;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal close;

    @Column(nullable = false)
    private int volume;

    @Override
    public double open() {
        return getOpen().doubleValue();
    }

    @Override
    public double high() {
        return getHigh().doubleValue();
    }

    @Override
    public double low() {
        return getLow().doubleValue();
    }

    @Override
    public double close() {
        return getClose().doubleValue();
    }
}
