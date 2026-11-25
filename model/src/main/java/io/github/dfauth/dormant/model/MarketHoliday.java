package io.github.dfauth.dormant.model;

import io.github.dfauth.dormant.model.repository.MarketHolidayId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "MARKET_HOLIDAY_CALENDAR") //, uniqueConstraints = @UniqueConstraint(columnNames = {"EXCHANGE_MIC", "HOLIDAY_DATE"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketHoliday {

    @EmbeddedId
    private MarketHolidayId id;

    @Column(nullable = true)
    private String holidayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MarketStatus marketStatus;

    @Column(nullable = true)
    private LocalTime earlyCloseTimeLocal;

}
