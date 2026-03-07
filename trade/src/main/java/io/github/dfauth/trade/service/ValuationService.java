package io.github.dfauth.trade.service;

import io.github.dfauth.trade.model.Direction;
import io.github.dfauth.trade.model.Valuation;
import io.github.dfauth.trade.repository.ValuationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public List<List<Valuation>> findTargetIncreases() {
        return findTargetChanges(Direction.RISING);
    }

    public List<List<Valuation>> findTargetChanges(Direction direction) {
        List<Valuation> all = valuationRepository.findAllByOrderByMarketAscCodeAscDateDesc();
        Map<String, List<Valuation>> grouped = new LinkedHashMap<>();
        for (Valuation v : all) {
            grouped.computeIfAbsent(v.getMarket() + ":" + v.getCode(), k -> new ArrayList<>()).add(v);
        }
        return grouped.values().stream()
                .filter(list -> list.size() >= 2 && direction.<BigDecimal>getBiPredicate().test(list.get(0).getTarget(), list.get(1).getTarget()))
//                .filter(list -> list.size() >= 2 && list.get(0).getTarget().compareTo(list.get(1).getTarget()) > 0)
                .map(list -> List.of(list.get(0), list.get(1)))
                .toList();
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
