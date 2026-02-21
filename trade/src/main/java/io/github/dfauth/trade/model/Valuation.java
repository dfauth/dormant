package io.github.dfauth.trade.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "valuations", uniqueConstraints = @UniqueConstraint(columnNames = {"market", "code", "date"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Valuation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String market;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Consensus consensus;

    @Column(nullable = false)
    private Integer buy;

    @Column(nullable = false)
    private Integer hold;

    @Column(nullable = false)
    private Integer sell;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal target;

    @Column(nullable = false)
    private LocalDate date;
}
