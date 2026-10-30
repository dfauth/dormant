package io.github.dfauth.dormant.actor;

import io.github.dfauth.dormant.actor.repository.PriceVolume;
import io.github.dfauth.dormant.actor.repository.TAContainer;
import io.github.dfauth.dormant.actor.repository.TAContainerId;
import io.github.dfauth.dormant.actor.repository.TAContainerRepository;
import io.github.dfauth.dormant.model.Price;
import io.github.dfauth.dormant.model.Tenor;
import io.github.dfauth.dormant.model.repository.PriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

import static io.github.dfauth.ta.MovingAverage.ema;
import static io.github.dfauth.trycatch.Function2.peek;

@Slf4j
@Component
@RequiredArgsConstructor
public class Actor implements ConsumerSeekAware {

    private final PriceRepository priceRepository;
    private final TAContainerRepository taContainerRepository;
    private final long startup = Instant.now().toEpochMilli();

    @KafkaListener(topics = {"prices"}, groupId = "prices")
    public void onMessage(Price price) {
        log.info("Received price {}", price);
        priceRepository.findByCodeAndTenor(price.getMarketCode(), Tenor._2Y)
                .stream()
                .gather(new StatefulCalculationGatherer<>(
                        new EMACalculator<>(
                                new Price[200],
                                new Aggregator.AggregatorGatherer<>(new PriceVolumeAggregator()),
                                (l, r) -> new PriceVolume(ema(2.0, l.getPrice(), r.close()), ema(2.0, l.getVolume(), r.volume()))
                        )))
                .findFirst()
                .ifPresent(pv -> {
                    taContainerRepository.findById(new TAContainerId(price.getMarket(), price.getCode(), price.getDate()))
                            .filter(tac -> !tac.getEma200().equals(pv))
                            .map(peek(tac -> tac.setEma200(pv)))
                            .ifPresentOrElse(taContainerRepository::save,
                                    () -> taContainerRepository.save(new TAContainer(price.getMarket(), price.getCode(), price.getDate(), pv))
                            );
                    log.info("event: {}", pv);
                });
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        log.info("assignments {}", assignments);
        callback.seekToTimestamp(assignments.keySet(), startup);
    }

}
