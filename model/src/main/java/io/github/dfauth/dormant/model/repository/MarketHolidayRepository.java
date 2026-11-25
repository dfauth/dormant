package io.github.dfauth.dormant.model.repository;

import io.github.dfauth.dormant.model.BusinessDayCalendar;
import io.github.dfauth.dormant.model.Market;
import io.github.dfauth.dormant.model.MarketHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.TreeMap;

import static io.github.dfauth.trycatch.Utils.oops;

@Repository
public interface MarketHolidayRepository extends JpaRepository<MarketHoliday, MarketHolidayId> {

    @Query("SELECT mh FROM MarketHoliday mh WHERE mh.id.mic = :mic")
    List<MarketHoliday> findByMarketCode(@Param("mic") String mic);

    default BusinessDayCalendar findBusinessCalendarByMarket(Market market) {
        return new BusinessDayCalendar(market, findByMarketCode(market.getMic()).stream()
                .reduce(
                        new TreeMap<>(),
                        (m, mh) -> {
                            m.put(mh.getId().getDate(), mh);
                            return m;
                            },
                        oops()
                ));
    }
}
