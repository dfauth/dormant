package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.Trade;
import io.github.dfauth.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeRepository tradeRepository;

    @PostMapping
    public ResponseEntity<?> createTrade(@RequestBody Trade trade) {
        if (tradeRepository.existsByConfirmationId(trade.getConfirmationId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Trade with confirmation_id '" + trade.getConfirmationId() + "' already exists"));
        }
        Trade saved = tradeRepository.save(trade);
        log.info("Persisted trade: market={}, confirmationId={}, code={}, side={}", saved.getMarket(), saved.getConfirmationId(), saved.getCode(), saved.getSide());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/batch")
    public ResponseEntity<?> createTrades(@RequestBody List<Trade> trades) {
        List<Trade> saved = trades.stream()
                .filter(t -> !tradeRepository.existsByConfirmationId(t.getConfirmationId()))
                .map(tradeRepository::save)
                .toList();
        log.info("Persisted {} of {} trades", saved.size(), trades.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/market/{market}")
    public List<Trade> getTradesByMarket(@PathVariable("market") String market) {
        return tradeRepository.findByMarket(market);
    }

    @GetMapping("/confirmationId/{confirmationId}")
    public ResponseEntity<Trade> getByConfirmationId(@PathVariable("confirmationId") String confirmationId) {
        return tradeRepository.findByConfirmationId(confirmationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
