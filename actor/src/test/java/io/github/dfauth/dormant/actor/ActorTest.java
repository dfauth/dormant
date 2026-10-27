package io.github.dfauth.dormant.actor;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.dfauth.dormant.actor.config.KafkaConfig;
import io.github.dfauth.dormant.actor.repository.TAContainerRepository;
import io.github.dfauth.dormant.kafka.KafkaPublisher;
import io.github.dfauth.dormant.model.Price;
import io.github.dfauth.dormant.model.Tenor;
import io.github.dfauth.dormant.model.repository.PriceRepository;
import io.github.dfauth.ta.TestData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.github.dfauth.trycatch.CompletableFutureUtils.handler;
import static io.github.dfauth.trycatch.Utils.bd;
import static java.lang.Thread.sleep;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@EmbeddedKafka(
        ports = 9092,
        partitions = 1,
        controlledShutdown = true,
        topics = {"prices"}
)
@ActiveProfiles("test")
@SpringBootTest(classes = {KafkaConfig.class, Actor.class})
public class ActorTest {

    private static final String CODE = "ASX:PNR";
    @Autowired private Actor actor;
    @Autowired private KafkaPublisher<String, Price> kafkaPublisher;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockitoBean
    private PriceRepository priceRepository;

    @MockitoBean
    private TAContainerRepository taContainerRepository;


    @Test
    public void testIt() throws InterruptedException, ExecutionException, TimeoutException {
        List<Price> prices = TestData.unzipCandles(new TypeReference<>() {
        }, TestData.PNR);
        when(priceRepository.findByCodeAndTenor(anyString(), any(Tenor.class))).thenReturn(prices);
        CompletableFuture<SendResult<String, Price>> fut = kafkaPublisher.publish(CODE, new Price(CODE, LocalDate.now(), bd(0.81), bd(0.83), bd(0.805), bd(0.82), 100000));
        fut.handle(handler(sr -> {
            log.info("sendResult: {}", sr);
            }, ex ->
                log.info(ex.getMessage(), ex))
        );

        SendResult<String, Price> sr = fut.get(1000, TimeUnit.MILLISECONDS);
        sleep(10000);
        verify(taContainerRepository, times(1)).save(any());
    }
}
