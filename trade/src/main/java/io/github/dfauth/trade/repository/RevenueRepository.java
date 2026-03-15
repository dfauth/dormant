package io.github.dfauth.trade.repository;

import io.github.dfauth.trade.model.Revenue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RevenueRepository extends JpaRepository<Revenue, Long> {

    boolean existsByMarketAndCodeAndDate(String market, String code, LocalDate date);

    Optional<Revenue> findByMarketAndCodeAndDate(String market, String code, LocalDate date);

    List<Revenue> findByMarketAndCodeOrderByDateAsc(String market, String code);
}
