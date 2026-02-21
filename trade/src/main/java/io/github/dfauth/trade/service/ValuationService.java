package io.github.dfauth.trade.service;

import io.github.dfauth.trade.model.Valuation;
import io.github.dfauth.trade.repository.ValuationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ValuationService {

    private final ValuationRepository valuationRepository;

    @Transactional
    public List<Valuation> createBatch(List<Valuation> valuations) {
        List<Valuation> newValuations = valuations.stream()
                .filter(v -> !valuationRepository.existsByMarketAndCodeAndDate(v.getMarket(), v.getCode(), v.getDate()))
                .toList();
        return valuationRepository.saveAll(newValuations);
    }

    @Transactional
    public Valuation upsert(Valuation valuation) {
        return valuationRepository.findByMarketAndCodeAndDate(valuation.getMarket(), valuation.getCode(), valuation.getDate())
                .map(existing -> {
                    existing.setConsensus(valuation.getConsensus());
                    existing.setBuy(valuation.getBuy());
                    existing.setHold(valuation.getHold());
                    existing.setSell(valuation.getSell());
                    existing.setTarget(valuation.getTarget());
                    return valuationRepository.save(existing);
                })
                .orElseGet(() -> valuationRepository.save(valuation));
    }
}
