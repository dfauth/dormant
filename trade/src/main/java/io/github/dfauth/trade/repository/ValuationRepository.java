package io.github.dfauth.trade.repository;

import io.github.dfauth.trade.model.Valuation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ValuationRepository extends JpaRepository<Valuation, Long> {

    boolean existsByMarketAndCodeAndDate(String market, String code, LocalDate date);

    Optional<Valuation> findByMarketAndCodeAndDate(String market, String code, LocalDate date);

    List<Valuation> findByMarketAndCodeOrderByDateAsc(String market, String code);

    List<Valuation> findByMarketAndCodeAndDateBetweenOrderByDateAsc(String market, String code, LocalDate start, LocalDate end);

    @Query("SELECT v FROM Valuation v WHERE v.date = (SELECT MAX(v2.date) FROM Valuation v2 WHERE v2.market = v.market AND v2.code = v.code) AND v.date >= :cutoff ORDER BY v.market, v.code")
    List<Valuation> findLatestPerCodeSince(@Param("cutoff") LocalDate cutoff);
}
