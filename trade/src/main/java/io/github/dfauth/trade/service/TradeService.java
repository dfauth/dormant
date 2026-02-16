package io.github.dfauth.trade.service;

import io.github.dfauth.trade.model.Trade;
import io.github.dfauth.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;

    @Transactional
    public List<Trade> createBatch(List<Trade> trades, Long userId) {
        for (Trade trade : trades) {
            if (tradeRepository.existsByConfirmationId(trade.getConfirmationId())) {
                throw new DuplicateTradeException(trade.getConfirmationId());
            }
            trade.setUserId(userId);
        }
        return tradeRepository.saveAll(trades);
    }
}
