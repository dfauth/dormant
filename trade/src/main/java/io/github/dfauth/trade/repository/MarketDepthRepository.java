package io.github.dfauth.trade.repository;

import io.github.dfauth.trade.model.MarketDepth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketDepthRepository extends JpaRepository<MarketDepth, Long> {

    List<MarketDepth> findByMarketAndCodeOrderByRecordedAtDesc(String market, String code);
}
