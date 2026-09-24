package io.github.dfauth.trade.repository;

import io.github.dfauth.trade.model.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceRepository extends JpaRepository<Price, Long> {

    List<Price> findByMarketOrderByDateAsc(String market);

    List<Price> findByMarketAndDateBetweenOrderByDateAsc(String market, LocalDate start, LocalDate end);

    List<Price> findByMarketAndCodeOrderByDateAsc(String market, String code);

    List<Price> findByMarketAndCodeAndDateBetweenOrderByDateAsc(String market, String code, LocalDate start, LocalDate end);

    boolean existsByMarketAndCodeAndDate(String market, String code, LocalDate date);

    Optional<Price> findTopByMarketAndCodeOrderByDateDesc(String market, String code);

    @Query("SELECT DISTINCT p.code FROM Price p WHERE p.market = :market ORDER BY p.code")
    List<String> findDistinctCodesByMarket(@Param("market") String market);

    @Query("SELECT p FROM Price p WHERE p.market = :market and p.code = :code AND p.date > :date ORDER BY p.date asc")
    List<Price> findByCodeOrderByDateAsc(@Param("market") String market, @Param("code") String code, @Param("date") LocalDate date);

    default List<Price> findByCodeOrderByDateAsc(String marketCodeString) {
        String[] arr = marketCodeString.split(":");
        String mkt = arr[0];
        String code = arr[1];
        LocalDate date = LocalDate.now().minusYears(1);
        return findByCodeOrderByDateAsc(mkt, code, date);
    }
}
