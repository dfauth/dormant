package io.github.dfauth.trade.repository;

import io.github.dfauth.trade.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    boolean existsByConfirmationIdAndUserId(String confirmationId, Long userId);

    List<Trade> findByUserIdAndMarket(Long userId, String market);

    List<Trade> findByUserIdAndMarketAndCodeOrderByDateAsc(Long userId, String market, String code);

    List<Trade> findByUserIdAndMarketOrderByDateAsc(Long userId, String market);

    @Query("SELECT DISTINCT t.market, t.code FROM Trade t WHERE t.userId = :userId")
    List<Object[]> findDistinctMarketAndCodeByUserId(@Param("userId") Long userId);

    @Query("SELECT t from Trade t WHERE t.userId = :userId")
    List<Trade> findByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM Trade t WHERE t.userId = :userId AND t.market = :market AND t.date >= :start AND t.date <= :end")
    List<Trade> findByUserIdMarketAndDates(@Param("userId") Long userId, @Param("market") String market, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
