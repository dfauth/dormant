package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.DateRange;
import io.github.dfauth.trade.model.Price;
import io.github.dfauth.trade.repository.PriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceRepository priceRepository;

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Integer>> createPrices(@RequestBody List<Price> prices) {
        List<Price> newPrices = prices.stream()
                .filter(p -> !priceRepository.existsByMarketAndCodeAndDate(p.getMarket(), p.getCode(), p.getDate()))
                .toList();
        priceRepository.saveAll(newPrices);
        log.info("Persisted {} of {} prices ({} duplicates skipped)", newPrices.size(), prices.size(), prices.size() - newPrices.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("persisted", newPrices.size(), "skipped", prices.size() - newPrices.size()));
    }

    @GetMapping
    public List<Price> getPrices(@RequestParam("market") String market,
                                 @RequestParam("code") String code,
                                 @RequestParam("tenor") Optional<String> tenor,
                                 @RequestParam("startFrom") Optional<String> startFrom,
                                 @RequestParam("endAt") Optional<String> endAt) {
        Optional<DateRange> dateRange = DateRange.resolve(tenor, startFrom, endAt);
        return dateRange
                .map(dr -> priceRepository.findByMarketAndCodeAndDateBetweenOrderByDateAsc(market, code, dr.start(), dr.end()))
                .orElseGet(() -> priceRepository.findByMarketAndCodeOrderByDateAsc(market, code));
    }
}
