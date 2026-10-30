package io.github.dfauth.dormant.actor;

import io.github.dfauth.dormant.actor.repository.PriceVolume;
import io.github.dfauth.dormant.actor.repository.TAContainer;
import io.github.dfauth.dormant.actor.repository.TAContainerRepository;
import io.github.dfauth.dormant.model.Price;
import io.github.dfauth.dormant.model.Tenor;
import io.github.dfauth.dormant.model.repository.PriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.jspecify.annotations.Nullable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

import static io.github.dfauth.ta.MovingAverage.ema;
import static io.github.dfauth.trycatch.TryCatch.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class Actor implements ConsumerSeekAware {

    private final PriceRepository priceRepository;
    private final TAContainerRepository taContainerRepository;

    @KafkaListener(topics = {"prices"}, groupId = "prices")
    public void onMessage(Price price) {
        log.info("Received price {}", price);
        priceRepository.findByCodeAndTenor(price.getMarketCode(), Tenor._2Y)
                .stream()
                .gather(new StatefulCalculationGatherer<>(
                            new EMACalculator<Price, PriceVolume>(
                                    new Price[200],
                                    new Aggregator.AggregatorGatherer<>(new PriceVolumeAggregator()),
                                    (l, r) -> new PriceVolume(ema(2.0, l.getPrice(), r.close()), ema(2.0, l.getVolume(), r.volume()))
                            )))
                .findFirst()
                .ifPresent(pv -> {
                    tryCatchRunnable(
                            () -> taContainerRepository.save(new TAContainer(price.getMarket(), price.getCode(), price.getDate(), pv)),
                            ex -> log.error(ex.getMessage(), ex)
                            );
                    log.info("event: {}", pv);
                });
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        log.info("assignments {}", assignments);
        callback.seekToBeginning(assignments.keySet());
    }

    @Override
    public void onFirstPoll() {
        ConsumerSeekAware.super.onFirstPoll();
        log.info("firstPoll");
    }

    @Override
    public void onPartitionsRevoked(@Nullable Collection<TopicPartition> partitions) {
        ConsumerSeekAware.super.onPartitionsRevoked(partitions);
        log.info("partitions revoked {}", partitions);
    }

    @Override
    public void onIdleContainer(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        ConsumerSeekAware.super.onIdleContainer(assignments, callback);
        log.info("on idle container {}", assignments);
    }
}
