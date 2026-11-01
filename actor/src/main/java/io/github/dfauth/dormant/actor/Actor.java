package io.github.dfauth.dormant.actor;

import io.github.dfauth.dormant.actor.repository.PriceVolume;
import io.github.dfauth.dormant.actor.repository.TAContainer;
import io.github.dfauth.dormant.actor.repository.TAContainerRepository;
import io.github.dfauth.dormant.model.Price;
import io.github.dfauth.dormant.model.repository.PriceRepository;
import io.github.dfauth.ta.RingBuffer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Gatherer;

import static io.github.dfauth.ta.MovingAverage.ema;
import static io.github.dfauth.trycatch.Function2.peek;
import static io.github.dfauth.trycatch.Function2.right;
import static io.github.dfauth.trycatch.TryCatch.tryCatch;

@Slf4j
@Component
public class Actor implements ConsumerSeekAware {

    private final PriceRepository priceRepository;
    private final TAContainerRepository taContainerRepository;
    private final long startup;
    private final int period = 200;

    public Actor(PriceRepository priceRepository,
                 TAContainerRepository taContainerRepository,
                 @Value("${io.github.dfauth.dormant.actor.Actor.rewrite:false}") boolean rewrite) {
        this.priceRepository = priceRepository;
        this.taContainerRepository = taContainerRepository;
        this.startup = rewrite ? 0 : Instant.now().toEpochMilli();
    }

    @KafkaListener(topics = {"prices"}, groupId = "prices")
    public void onMessage(Price price) {
        log.info("Received price {}", price);
        RingBuffer<Price> ringBuffer = RingBuffer.create(new Price[period]);
        Gatherer<Price, EMACalculationState<Price, PriceVolume>, PriceVolume> emaCalculator = new EMACalculationState<>(
                ringBuffer,
                new PriceVolumeAggregator(),
                (l, r) -> new PriceVolume(ema(2.0, period, r.close(), l.getPrice()), ema(2.0, period, r.volume(), l.getVolume()))
        ).gatherer();
        priceRepository.findByCodeAndLimit(price.getMarketCode(), 2 * period)
                .stream()
                .gather(emaCalculator)
                .reduce(right())
                .ifPresent(tryCatch(pv -> {
                    taContainerRepository.findByMarketAndCodeAndDate(price.getMarket(), price.getCode(), price.getDate())
                            .filter(tac -> !tac.getEma200().equals(pv))
                            .map(peek(tac -> tac.setEma200(pv)))
                            .ifPresentOrElse(taContainerRepository::save,
                                    () -> taContainerRepository.save(new TAContainer(price.getMarket(), price.getCode(), price.getDate(), pv))
                            );
                    log.info("event: {}", pv);
                }, e -> log.error(e.getMessage(), e)));
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        log.info("assignments {}", assignments);
        callback.seekToTimestamp(assignments.keySet(), startup);
    }

}
