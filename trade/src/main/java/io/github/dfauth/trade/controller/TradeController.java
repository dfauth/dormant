package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.DateRange;
import io.github.dfauth.trade.model.Trade;
import io.github.dfauth.trade.model.User;
import io.github.dfauth.trade.repository.TradeRepository;
import io.github.dfauth.trade.service.DuplicateTradeException;
import io.github.dfauth.trade.service.TradeService;
import io.github.dfauth.trade.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
@Tag(name = "Trades", description = "Record and retrieve trades")
public class TradeController {

    private final TradeRepository tradeRepository;
    private final TradeService tradeService;
    private final UserService userService;

    private Long resolveUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return userService.findOrCreateUser(oidcUser).getId();
        } else if (principal instanceof Jwt jwt) {
            return userService.findOrCreateUser(jwt).getId();
        }
        throw new IllegalStateException("Unsupported principal type: " + principal.getClass());
    }

    @Operation(summary = "Create a trade", description = "Persists a single trade. Returns 409 if a trade with the same confirmation ID already exists.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trade created"),
            @ApiResponse(responseCode = "409", description = "Duplicate confirmation ID")
    })
    @PostMapping
    public ResponseEntity<?> createTrade(@RequestBody Trade trade, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        if (tradeRepository.existsByConfirmationId(trade.getConfirmationId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Trade with confirmation_id '" + trade.getConfirmationId() + "' already exists"));
        }
        trade.setUserId(userId);
        Trade saved = tradeRepository.save(trade);
        log.info("Persisted trade: market={}, confirmationId={}, code={}, side={}", saved.getMarket(), saved.getConfirmationId(), saved.getCode(), saved.getSide());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Create multiple trades", description = "Persists a batch of trades. Returns 409 if any trade has a duplicate confirmation ID; no trades in the batch are saved.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "All trades created"),
            @ApiResponse(responseCode = "409", description = "Duplicate confirmation ID in batch")
    })
    @PostMapping("/batch")
    public ResponseEntity<?> createTrades(@RequestBody List<Trade> trades, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        try {
            List<Trade> saved = tradeService.createBatch(trades, userId);
            log.info("Persisted {} trades", saved.size());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (DuplicateTradeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Get trades by market", description = "Returns the authenticated user's trades for a market, optionally filtered by date range or tenor.")
    @ApiResponse(responseCode = "200", description = "List of trades")
    @GetMapping()
    public List<Trade> getTradesByMarket(Authentication authentication,
                                         @Parameter(description = "Market code (e.g. ASX). Defaults to the user's default market.") @RequestParam("market") Optional<String> market,
                                         @Parameter(description = "Tenor shorthand for date range (e.g. 6M, 1Y)") @RequestParam("tenor") Optional<String> tenors,
                                         @Parameter(description = "Start date in YYYYMMDD format") @RequestParam("startFrom") Optional<String> startFrom,
                                         @Parameter(description = "End date in YYYYMMDD format") @RequestParam("endAt") Optional<String> endAt
    ) {
        Optional<DateRange> dateRange = DateRange.resolve(tenors, startFrom, endAt);
        Long userId = resolveUserId(authentication);
        String mkt = market.orElseGet(() ->
            switch(authentication.getPrincipal()) {
                case DefaultOidcUser p ->
                    userService.findById(p.getSubject()).map(User::getDefaultMarket).orElseThrow();

                default -> throw new IllegalStateException("Unexpected value: " + authentication.getPrincipal());
            }
        );
        return dateRange
                .map(dr -> tradeRepository.findByUserIdMarketAndDates(userId, mkt, dr.start(), dr.end()))
                .orElseGet(() -> tradeRepository.findByUserIdAndMarket(userId, mkt));
    }

    @Operation(summary = "Get trade by confirmation ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trade found"),
            @ApiResponse(responseCode = "404", description = "Trade not found")
    })
    @GetMapping("/confirmationId/{confirmationId}")
    public ResponseEntity<Trade> getByConfirmationId(
            @Parameter(description = "Unique confirmation ID of the trade") @PathVariable("confirmationId") String confirmationId,
            Authentication authentication) {
        return tradeRepository.findByConfirmationId(confirmationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
