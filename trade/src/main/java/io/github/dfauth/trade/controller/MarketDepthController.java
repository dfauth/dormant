package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.MarketDepth;
import io.github.dfauth.trade.model.MarketDepthSummary;
import io.github.dfauth.trade.repository.MarketDepthRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/depth")
@RequiredArgsConstructor
@Tag(name = "Market Depth", description = "Record and query market depth snapshots")
public class MarketDepthController {

    private final MarketDepthRepository marketDepthRepository;

    @Operation(summary = "Get daily-averaged market depth per code for the last 3 days")
    @ApiResponse(responseCode = "200", description = "Daily averages per security")
    @GetMapping("/recent")
    public List<MarketDepthSummary> getRecentDepth() {
        LocalDateTime cutoff = LocalDate.now().minusDays(3).atStartOfDay();
        return aggregateByDay(marketDepthRepository.findByRecordedAtGreaterThanEqual(cutoff));
    }

    @Operation(summary = "Get all daily-averaged market depth history for a code")
    @ApiResponse(responseCode = "200", description = "Daily averages for the given code, all history")
    @GetMapping("/{code}")
    public List<MarketDepthSummary> getDepthByCode(
            @Parameter(description = "Security code (e.g. CBA)") @PathVariable("code") String code) {
        return aggregateByDay(marketDepthRepository.findByCodeOrderByRecordedAtDesc(code.toUpperCase()));
    }

    private List<MarketDepthSummary> aggregateByDay(List<MarketDepth> entries) {
        return entries.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getCode() + "|" + d.getRecordedAt().toLocalDate(),
                        Collectors.toList()
                ))
                .entrySet().stream()
                .map(e -> {
                    List<MarketDepth> days = e.getValue();
                    String code = days.get(0).getCode();
                    LocalDate date = days.get(0).getRecordedAt().toLocalDate();
                    int buyers       = (int) Math.round(days.stream().mapToInt(MarketDepth::getBuyers).average().orElse(0));
                    int buyerShares  = (int) Math.round(days.stream().mapToInt(MarketDepth::getBuyerShares).average().orElse(0));
                    int sellers      = (int) Math.round(days.stream().mapToInt(MarketDepth::getSellers).average().orElse(0));
                    int sellerShares = (int) Math.round(days.stream().mapToInt(MarketDepth::getSellerShares).average().orElse(0));
                    double ratio = sellerShares > 0
                            ? (double) buyerShares / sellerShares * 100
                            : 0;
                    return new MarketDepthSummary(date, code, buyers, buyerShares, sellers, sellerShares,
                            Math.round(ratio * 10.0) / 10.0);
                })
                .sorted(Comparator.comparing(MarketDepthSummary::date).reversed()
                        .thenComparing(MarketDepthSummary::code))
                .toList();
    }

    @Operation(
            summary = "Record a market depth snapshot",
            description = "Persists a single market depth snapshot for the given security. " +
                    "The path parameter must be in the form MARKET:CODE (e.g. ASX:CBA).")
    @ApiResponse(responseCode = "201", description = "Snapshot recorded")
    @ApiResponse(responseCode = "400", description = "Invalid market:code format")
    @PostMapping("/{marketCode}")
    @CrossOrigin("https://sharetrading.westpac.com.au")
    public ResponseEntity<MarketDepth> record(
            @Parameter(description = "Market and code separated by colon, e.g. ASX:CBA")
            @PathVariable("marketCode") String marketCode,
            @RequestBody MarketDepth depth) {

        String[] parts = marketCode.split(":", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        depth.setMarket(parts[0].toUpperCase());
        depth.setCode(parts[1].toUpperCase());
        depth.setRecordedAt(LocalDateTime.now());

        MarketDepth saved = marketDepthRepository.save(depth);
        log.info("Recorded market depth: {}/{} price={} volume={}", saved.getMarket(), saved.getCode(), saved.getPrice(), saved.getVolume());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
