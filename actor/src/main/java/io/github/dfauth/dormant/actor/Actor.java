package io.github.dfauth.dormant.actor;

import io.github.dfauth.dormant.actor.repository.PriceVolume;
import io.github.dfauth.dormant.actor.repository.TAContainerRepository;
import io.github.dfauth.dormant.model.BusinessDayCalendar;
import io.github.dfauth.dormant.model.MarketEnum;
import io.github.dfauth.dormant.model.Price;
import io.github.dfauth.dormant.model.repository.MarketHolidayRepository;
import io.github.dfauth.dormant.model.repository.PriceRepository;
import io.github.dfauth.ta.RingBuffer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static io.github.dfauth.trycatch.Function2.right;
import static io.github.dfauth.trycatch.TryCatch.tryCatch;
import static java.util.function.Predicate.not;

@Slf4j
@Component
public class Actor implements ConsumerSeekAware {

    private static final Function<Price, PriceVolume> extractor = p -> new PriceVolume(p.close(), p.volume());
    private final MarketHolidayRepository marketHolidayRepository;
    private final PriceRepository priceRepository;
    private final TAContainerRepository taContainerRepository;
    private final long startup;
    private final int period = 200;

    public Actor(MarketHolidayRepository marketHolidayRepository,
                 PriceRepository priceRepository,
                 TAContainerRepository taContainerRepository,
                 @Value("${io.github.dfauth.dormant.actor.Actor.rewrite:false}") boolean rewrite) {
        this.marketHolidayRepository = marketHolidayRepository;
        this.priceRepository = priceRepository;
        this.taContainerRepository = taContainerRepository;
        this.startup = rewrite ? 0 : Instant.now().toEpochMilli();
    }

    @KafkaListener(topics = {"prices"}, groupId = "prices")
    @Transactional
    public void onMessage(Price price) {
        log.info("Received price {}", price);
        RingBuffer<Price> ringBuffer = RingBuffer.create(new Price[period]);
        ExponentialMovingAverage<Price, PriceVolume> emaCalculator = new ExponentialMovingAverage<>(
                period,
                extractor,
                new SimpleMovingAverage<>(ringBuffer, extractor)
        );
        BusinessDayCalendar businessCalendar = marketHolidayRepository.findBusinessCalendarByMarket(MarketEnum.valueOf(price.getMarket()));
        LocalDate date = businessCalendar.nextOpen().minusDays(2 * period);
        priceRepository.findByCodeOrderByDateAsc(price.getMarketCode(), date)
                .stream()
                .flatMap(p -> emaCalculator.apply(p).stream())
                .reduce(right())
                .ifPresent(tryCatch(ema200 -> {
                    calculateEma(ringBuffer, 65).ifPresent(ema65 -> {
                        calculateEma(ringBuffer, 21).ifPresent(ema21 -> {
                            calculateEma(ringBuffer, 8).ifPresent(ema8 -> {
                                taContainerRepository.upsert(
                                        price.getMarket(),
                                        price.getCode(),
                                        price.getDate(),
                                        ema200,
                                        ema65,
                                        ema21,
                                        ema8
                                );
                                log.info("event: {} {} {} {}", ema200, ema65, ema21, ema8);
                            });
                        });
                    });
                }, e -> log.error(e.getMessage(), e)));
    }

    private Optional<PriceVolume> calculateEma(RingBuffer<Price> ringBuffer, int period) {
        ExponentialMovingAverage<Price, PriceVolume> emaCalculator = new ExponentialMovingAverage<>(
                period,
                extractor,
                new SimpleAverage<>(period, extractor, new PriceVolume())
        );
        return Optional.of(ringBuffer.stream(-2 * period)
                .flatMap(p -> emaCalculator.apply(p).stream()).toList())
                .filter(not(List::isEmpty))
                .map(List::getLast);
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        log.info("assignments {}", assignments);
        callback.seekToTimestamp(assignments.keySet(), startup);
    }

}
