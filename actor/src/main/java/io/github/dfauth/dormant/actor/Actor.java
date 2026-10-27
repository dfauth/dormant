package io.github.dfauth.dormant.actor;

import io.github.dfauth.dormant.actor.repository.PriceVolume;
import io.github.dfauth.dormant.actor.repository.TAContainer;
import io.github.dfauth.dormant.actor.repository.TAContainerRepository;
import io.github.dfauth.dormant.model.Price;
import io.github.dfauth.dormant.model.Tenor;
import io.github.dfauth.dormant.model.repository.PriceRepository;
import io.github.dfauth.trycatch.Tuple2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static io.github.dfauth.ta.MovingAverage.ema;
import static io.github.dfauth.trycatch.Tuple2.tuple2;

@Slf4j
@Component
@RequiredArgsConstructor
public class Actor {

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
                    log.info("event: {}", pv);
                    taContainerRepository.save(new TAContainer(0L, price.getMarket(), price.getCode(), price.getDate(), pv));
                });
    }

}
