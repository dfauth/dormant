package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.DateRange;
import io.github.dfauth.trade.model.Trade;
import io.github.dfauth.trade.repository.TradeRepository;
import io.github.dfauth.trade.service.DuplicateTradeException;
import io.github.dfauth.trade.service.TradeService;
import io.github.dfauth.trade.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/trades")
@Tag(name = "Trades", description = "Record and retrieve trades")
public class TradeController extends BaseController {

    private final TradeRepository tradeRepository;
    private final TradeService tradeService;

    public TradeController(UserService userService, TradeRepository tradeRepository, TradeService tradeService) {
        super(userService);
        this.tradeRepository = tradeRepository;
        this.tradeService = tradeService;
    }

    @Operation(summary = "Create a trade", description = "Persists a single trade. Returns 409 if a trade with the same confirmation ID already exists.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trade created"),
            @ApiResponse(responseCode = "409", description = "Duplicate confirmation ID")
    })
    @PostMapping
    public ResponseEntity<?> createTrade(@RequestBody Trade trade) {
        return authorize(u -> {
            if (tradeRepository.existsByConfirmationId(trade.getConfirmationId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Trade with confirmation_id '" + trade.getConfirmationId() + "' already exists"));
            }
            trade.setUserId(u.getId());
            Trade saved = tradeRepository.save(trade);
            log.info("Persisted trade: market={}, confirmationId={}, code={}, side={}", saved.getMarket(), saved.getConfirmationId(), saved.getCode(), saved.getSide());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        });
    }

    @Operation(summary = "Create multiple trades", description = "Persists a batch of trades. Returns 409 if any trade has a duplicate confirmation ID; no trades in the batch are saved.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "All trades created"),
            @ApiResponse(responseCode = "409", description = "Duplicate confirmation ID in batch")
    })
    @PostMapping("/batch")
    public ResponseEntity<?> createTrades(@RequestBody List<Trade> trades) {
        return authorize(u -> {
            try {
                List<Trade> saved = tradeService.createBatch(trades, u.getId());
                log.info("Persisted {} trades", saved.size());
                return ResponseEntity.status(HttpStatus.CREATED).body(saved);
            } catch (DuplicateTradeException e) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", e.getMessage()));
            }
        });
    }

    @Operation(summary = "Get trades by market", description = "Returns the authenticated user's trades for a market, optionally filtered by date range or tenor.")
    @ApiResponse(responseCode = "200", description = "List of trades")
    @GetMapping
    public List<Trade> getTradesByMarket(
            @Parameter(description = "Market code (e.g. ASX). Defaults to the user's default market.") @RequestParam("market") Optional<String> market,
            @Parameter(description = "Tenor shorthand for date range (e.g. 6M, 1Y)") @RequestParam("tenor") Optional<String> tenors,
            @Parameter(description = "Start date in YYYYMMDD format") @RequestParam("startFrom") Optional<String> startFrom,
            @Parameter(description = "End date in YYYYMMDD format") @RequestParam("endAt") Optional<String> endAt) {
        return authorize(u -> {
            String mkt = market.orElseGet(u::getDefaultMarket);
            Optional<DateRange> dateRange = DateRange.resolve(tenors, startFrom, endAt);
            return dateRange
                    .map(dr -> tradeRepository.findByUserIdMarketAndDates(u.getId(), mkt, dr.start(), dr.end()))
                    .orElseGet(() -> tradeRepository.findByUserIdAndMarket(u.getId(), mkt));
        });
    }

    @Operation(summary = "Get trade by confirmation ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trade found"),
            @ApiResponse(responseCode = "404", description = "Trade not found")
    })
    @GetMapping("/confirmationId/{confirmationId}")
    public ResponseEntity<Trade> getByConfirmationId(
            @Parameter(description = "Unique confirmation ID of the trade") @PathVariable("confirmationId") String confirmationId) {
        return authorize(u -> tradeRepository.findByConfirmationId(confirmationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build()));
    }
}
