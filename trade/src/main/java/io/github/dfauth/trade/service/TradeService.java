package io.github.dfauth.trade.service;

import io.github.dfauth.trade.model.Trade;
import io.github.dfauth.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.dfauth.trycatch.Function2.peek;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;

    @Transactional
    public List<Trade> createBatch(List<Trade> trades, Long userId) {
        Map<Boolean, List<Trade>> x = trades.stream().collect(Collectors.partitioningBy(t -> tradeRepository.existsByConfirmationId(t.getConfirmationId())));
        return tradeRepository.saveAll(
                trades.stream()
                        .map(peek(t -> tradeRepository.findByConfirmationId(t.getConfirmationId())
                                .map(peek(_t -> {
                                    t.setId(_t.getId());
                                    t.setUserId(userId);
                                }))
                                .orElseGet(() -> {
                                    t.setUserId(userId);
                                    return t;
                                })
                        )).toList());
    }
}
