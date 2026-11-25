package io.github.dfauth.dormant.model.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketHolidayId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "EXCHANGE_MIC", nullable = false)
    private String mic;

    @Column(name = "HOLIDAY_DATE", nullable = false)
    private LocalDate date;
}
