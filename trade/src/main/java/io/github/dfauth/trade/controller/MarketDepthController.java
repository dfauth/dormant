package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.MarketDepth;
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

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/depth")
@RequiredArgsConstructor
@Tag(name = "Market Depth", description = "Record and query market depth snapshots")
public class MarketDepthController {

    private final MarketDepthRepository marketDepthRepository;

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
