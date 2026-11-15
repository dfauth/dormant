package io.github.dfauth.dormant.actor.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface TAContainerRepository extends JpaRepository<TAContainer, Long> {

    ObjectMapper objectMapper = new ObjectMapper();


    Optional<TAContainer> findByMarketAndCodeAndDate(String market, String code, LocalDate date);

    @Modifying
    @Query(nativeQuery = true,
           value = "MERGE INTO ta KEY(market, code, date) " +
                   "VALUES (:market, :code, :date) " +
                   "INSERT (market, code, date, ema200, ema65, ema21, ema8) " +
                   "VALUES (:market, :code, :date, :ema200 AS FORMAT JSON, :ema65 AS FORMAT JSON, :ema21 FORMAT JSON, :ema8 FORMAT JSON) " +
                   "UPDATE SET ema200 = :ema200 AS FORMAT JSON, ema65 = :ema65 AS FORMAT JSON, ema21 = :ema21 AS FORMAT JSON, ema8 = :ema8 AS FORMAT JSON)")
    void upsert(@Param("market") String market, @Param("code") String code, @Param("date") LocalDate date,
               @Param("ema200") String ema200, @Param("ema65") String ema65,
               @Param("ema21") String ema21, @Param("ema8") String ema8);

    default void upsert(@Param("market") String market, @Param("code") String code, @Param("date") LocalDate date,
               @Param("ema200") PriceVolume ema200, @Param("ema65") PriceVolume ema65,
               @Param("ema21") PriceVolume ema21, @Param("ema8") PriceVolume ema8) {
        upsert(market, code, date, toJson(ema200), toJson(ema65), toJson(ema21), toJson(ema8));
    }

    private String toJson(PriceVolume pv) {
        try {
            return objectMapper.writeValueAsString(pv);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}
