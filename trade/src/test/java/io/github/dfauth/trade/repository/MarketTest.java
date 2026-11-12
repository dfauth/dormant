package io.github.dfauth.trade.repository;

import io.github.dfauth.dormant.model.BusinessDayCalendar;
import io.github.dfauth.dormant.model.MarketEnum;
import io.github.dfauth.dormant.model.repository.MarketHolidayRepository;
import io.github.dfauth.trade.config.LiquibaseConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@DataJpaTest(
        properties = {
                "spring.datasource.url=jdbc:h2:mem:testdb",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        },
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {MarketHolidayRepository.class, LiquibaseConfig.class}
        )
)
@ActiveProfiles("test")
public class MarketTest {

    // @MockitoBean
    @Autowired
    private MarketHolidayRepository marketHolidayRepository;

    // @Disabled
    @Test
    public void testIt() {
        LocalDate today = LocalDate.parse("20261109", BASIC_ISO_DATE);
        BusinessDayCalendar cal = marketHolidayRepository.findBusinessCalendarByMarket(MarketEnum.ASX);
        log.info("cal: {}", cal);
        log.info("nextOpen: {}", cal.nextOpen());
        LocalDate m400 = cal.of(today).minusDays(400);
        log.info("400 trading dates before today : {}", m400);
        assertEquals(LocalDate.parse("20250414", BASIC_ISO_DATE), m400);
    }

}
