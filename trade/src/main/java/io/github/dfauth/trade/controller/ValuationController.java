package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.DateRange;
import io.github.dfauth.trade.model.Valuation;
import io.github.dfauth.trade.model.ValuationSummary;
import io.github.dfauth.trade.repository.PriceRepository;
import io.github.dfauth.trade.repository.ValuationRepository;
import io.github.dfauth.trade.service.ValuationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/valuations")
@RequiredArgsConstructor
@Tag(name = "Valuations", description = "Manage analyst consensus valuations")
public class ValuationController {

    private final ValuationRepository valuationRepository;
    private final ValuationService valuationService;
    private final PriceRepository priceRepository;

    @Operation(summary = "Create or update a valuation", description = "Upserts a valuation for a given market, code, and date. If a record already exists for that combination, it is updated.")
    @ApiResponse(responseCode = "201", description = "Valuation created or updated")
    @PostMapping
    public ResponseEntity<Valuation> createValuation(@RequestBody Valuation valuation) {
        Valuation saved = valuationService.upsert(valuation);
        log.info("Upserted valuation: market={}, code={}, date={}", saved.getMarket(), saved.getCode(), saved.getDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Batch insert valuations", description = "Persists a list of valuations, silently skipping any that already exist for the same market, code, and date.")
    @ApiResponse(responseCode = "201", description = "Number of new valuations persisted")
    @PostMapping("/batch")
    public ResponseEntity<Integer> createValuations(@RequestBody List<Valuation> valuations) {
        List<Valuation> saved = valuationService.createBatch(valuations);
        log.info("Persisted {} of {} valuations ({} duplicates skipped)", saved.size(), valuations.size(), valuations.size() - saved.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.size());
    }

    @Operation(summary = "Get the most recent valuation per code, updated within the last 3 months")
    @ApiResponse(responseCode = "200", description = "Latest valuation per security with current price and upside potential, dated within 3 months")
    @GetMapping("/recent")
    public List<ValuationSummary> getRecentValuations() {
        return valuationRepository.findLatestPerCodeSince(LocalDate.now().minusMonths(3))
                .stream()
                .map(v -> ValuationSummary.of(v,
                        priceRepository.findTopByMarketAndCodeOrderByDateDesc(v.getMarket(), v.getCode())
                                .map(p -> p.getClose())
                                .orElse(null)))
                .toList();
    }

    @Operation(summary = "Get valuations for a security", description = "Returns analyst valuations ordered by date ascending, optionally filtered by date range or tenor. Price and potential are enriched from the most recent closing price.")
    @ApiResponse(responseCode = "200", description = "List of valuations with current price and upside potential")
    @GetMapping("/{code}")
    public List<ValuationSummary> getValuations(
            @Parameter(description = "Market code (e.g. ASX)") @RequestParam("market") String market,
            @Parameter(description = "Security code (e.g. CBA)") @PathVariable("code") String code,
            @Parameter(description = "Tenor shorthand for date range (e.g. 6M, 1Y)") @RequestParam("tenor") Optional<String> tenor,
            @Parameter(description = "Start date in YYYYMMDD format") @RequestParam("startFrom") Optional<String> startFrom,
            @Parameter(description = "End date in YYYYMMDD format") @RequestParam("endAt") Optional<String> endAt) {
        Optional<DateRange> dateRange = DateRange.resolve(tenor, startFrom, endAt);
        List<Valuation> valuations = dateRange
                .map(dr -> valuationRepository.findByMarketAndCodeAndDateBetweenOrderByDateAsc(market, code, dr.start(), dr.end()))
                .orElseGet(() -> valuationRepository.findByMarketAndCodeOrderByDateAsc(market, code));
        java.math.BigDecimal price = priceRepository.findTopByMarketAndCodeOrderByDateDesc(market, code)
                .map(p -> p.getClose())
                .orElse(null);
        return valuations.stream().map(v -> ValuationSummary.of(v, price)).toList();
    }

    @Operation(summary = "Get the most recent valuation for a security")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Most recent valuation"),
            @ApiResponse(responseCode = "404", description = "No valuations found for this security")
    })
    @GetMapping("/{code}/latest")
    public ResponseEntity<Valuation> getLatestValuation(
            @Parameter(description = "Market code (e.g. ASX)") @RequestParam("market") String market,
            @Parameter(description = "Security code (e.g. CBA)") @PathVariable("code") String code) {
        return valuationRepository.findByMarketAndCodeOrderByDateAsc(market, code)
                .stream()
                .reduce((first, second) -> second)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
