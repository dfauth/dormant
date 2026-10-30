package io.github.dfauth.dormant.actor.repository;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "ta", uniqueConstraints = @UniqueConstraint(columnNames = {"market", "code", "date"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TAContainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String market;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private LocalDate date;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = true)
    private PriceVolume ema200;

    public TAContainer(String market, String code, LocalDate date, PriceVolume ema200) {
        this(null, market, code, date, ema200);
    }
}
