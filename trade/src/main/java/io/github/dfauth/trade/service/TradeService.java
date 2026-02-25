package io.github.dfauth.trade.service;

import io.github.dfauth.trade.model.Trade;
import io.github.dfauth.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static io.github.dfauth.trycatch.Function2.peek;
import static java.util.function.Predicate.not;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;

    @Transactional
    public List<Trade> createBatch(List<Trade> trades, Long userId) {
        return tradeRepository.saveAll(trades.stream()
                .filter(not(t -> tradeRepository.existsByConfirmationId(t.getConfirmationId())))
                        .map(peek(t -> t.setUserId(userId)))
                .toList());
    }
}
