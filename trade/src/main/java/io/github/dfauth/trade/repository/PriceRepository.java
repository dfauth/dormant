package io.github.dfauth.trade.repository;

import io.github.dfauth.trade.model.Price;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceRepository extends JpaRepository<Price, Long> {

    List<Price> findByMarketAndCodeOrderByDateAsc(String market, String code);

    List<Price> findByMarketAndCodeAndDateBetweenOrderByDateAsc(String market, String code, LocalDate start, LocalDate end);

    boolean existsByMarketAndCodeAndDate(String market, String code, LocalDate date);

    Optional<Price> findTopByMarketAndCodeOrderByDateDesc(String market, String code);
}
