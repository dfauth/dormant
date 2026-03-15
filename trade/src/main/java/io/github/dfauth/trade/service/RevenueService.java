package io.github.dfauth.trade.service;

import io.github.dfauth.trade.model.Revenue;
import io.github.dfauth.trade.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final RevenueRepository revenueRepository;

    @Transactional
    public List<Revenue> upsertAll(List<Revenue> revenues) {
        return revenues.stream()
                .map(this::upsert)
                .toList();
    }

    @Transactional
    public Revenue upsert(Revenue revenue) {
        return revenueRepository.findByMarketAndCodeAndDate(revenue.getMarket(), revenue.getCode(), revenue.getDate())
                .map(existing -> {
                    existing.setAmount(revenue.getAmount());
                    return revenueRepository.save(existing);
                })
                .orElseGet(() -> revenueRepository.save(revenue));
    }

    public List<Revenue> findByMarketAndCode(String market, String code) {
        return revenueRepository.findByMarketAndCodeOrderByDateAsc(market, code);
    }
}
