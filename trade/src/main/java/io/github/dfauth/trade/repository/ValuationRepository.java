package io.github.dfauth.trade.repository;

import io.github.dfauth.trade.model.Valuation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ValuationRepository extends JpaRepository<Valuation, Long> {

    boolean existsByMarketAndCodeAndDate(String market, String code, LocalDate date);

    Optional<Valuation> findByMarketAndCodeAndDate(String market, String code, LocalDate date);

    List<Valuation> findByMarketAndCodeOrderByDateAsc(String market, String code);

    List<Valuation> findByMarketAndCodeAndDateBetweenOrderByDateAsc(String market, String code, LocalDate start, LocalDate end);
}
