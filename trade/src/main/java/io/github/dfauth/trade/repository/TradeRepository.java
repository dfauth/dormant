package io.github.dfauth.trade.repository;

import io.github.dfauth.trade.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    Optional<Trade> findByConfirmationId(String confirmationId);

    boolean existsByConfirmationId(String confirmationId);

    List<Trade> findByMarket(String market);

    List<Trade> findByMarketAndCodeOrderByDateAsc(String market, String code);

    List<Trade> findByMarketOrderByDateAsc(String market);

    @Query("SELECT DISTINCT t.market, t.code FROM Trade t")
    List<Object[]> findDistinctMarketAndCode();
}
