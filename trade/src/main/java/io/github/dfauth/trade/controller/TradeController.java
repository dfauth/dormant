package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.TenorRange;
import io.github.dfauth.trade.model.Trade;
import io.github.dfauth.trade.model.User;
import io.github.dfauth.trade.repository.TradeRepository;
import io.github.dfauth.trade.service.DuplicateTradeException;
import io.github.dfauth.trade.service.TradeService;
import io.github.dfauth.trade.service.UserService;
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

import static io.github.dfauth.trade.model.DatetimeFormats.YYYYMMDD;

@Slf4j
@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
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

    @GetMapping()
    public List<Trade> getTradesByMarket(Authentication authentication,
                                         @RequestParam("market") Optional<String> market,
                                         @RequestParam("tenor") Optional<String> tenors,
                                         @RequestParam("startFrom") Optional<String> startFrom,
                                         @RequestParam("endAt") Optional<String> endAt
    ) {
        Optional<TenorRange> tenorRange = tenors.map(TenorRange::parse)
                .map(t -> startFrom.map(YYYYMMDD::toLocalDate).map(t::startFrom).orElse(t))
                .map(t -> endAt.map(YYYYMMDD::toLocalDate).map(t::endAt).orElse(t));
        Long userId = resolveUserId(authentication);
        String mkt = market.orElseGet(() ->
            switch(authentication.getPrincipal()) {
                case DefaultOidcUser p ->
                    userService.findById(p.getSubject()).map(User::getDefaultMarket).orElseThrow();

                default -> throw new IllegalStateException("Unexpected value: " + authentication.getPrincipal());
            }
        );
        return tenorRange
                .map(tr -> tradeRepository.findByUserIdMarketAndDates(userId, mkt, tr.start(), tr.end()))
                .orElseGet(() -> tradeRepository.findByUserIdAndMarket(userId, mkt));
    }

    @GetMapping("/confirmationId/{confirmationId}")
    public ResponseEntity<Trade> getByConfirmationId(@PathVariable("confirmationId") String confirmationId, Authentication authentication) {
        return tradeRepository.findByConfirmationId(confirmationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
