package io.github.dfauth.trade.repository;

import io.github.dfauth.trade.model.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceRepository extends JpaRepository<Price, Long> {

    List<Price> findByMarketAndCodeOrderByDateAsc(String market, String code);

    List<Price> findByMarketAndCodeAndDateBetweenOrderByDateAsc(String market, String code, LocalDate start, LocalDate end);

    boolean existsByMarketAndCodeAndDate(String market, String code, LocalDate date);

    Optional<Price> findTopByMarketAndCodeOrderByDateDesc(String market, String code);

    @Query("SELECT DISTINCT p.code FROM Price p WHERE p.market = :market ORDER BY p.code")
    List<String> findDistinctCodesByMarket(@Param("market") String market);
}
