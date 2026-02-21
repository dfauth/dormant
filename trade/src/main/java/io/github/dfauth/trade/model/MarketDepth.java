package io.github.dfauth.trade.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minidev.json.annotate.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_depth")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDepth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @Column(nullable = false)
    private String market;

    @JsonIgnore
    @Column(nullable = false)
    private String code;

    @JsonIgnore
    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @Column(nullable = false)
    private Integer buyers;

    @Column(nullable = false)
    private Integer buyerShares;

    @Column(nullable = false)
    private Integer sellers;

    @Column(nullable = false)
    private Integer sellerShares;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal price;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal change;

    @Column(nullable = false)
    private Integer volume;
}
