package io.github.dfauth.dormant.actor;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.dfauth.dormant.actor.config.KafkaConfig;
import io.github.dfauth.dormant.actor.repository.TAContainer;
import io.github.dfauth.dormant.actor.repository.TAContainerRepository;
import io.github.dfauth.dormant.kafka.KafkaPublisher;
import io.github.dfauth.dormant.model.Price;
import io.github.dfauth.dormant.model.repository.PriceRepository;
import io.github.dfauth.ta.TestData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.github.dfauth.trycatch.CompletableFutureUtils.handler;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        }, TestData.CSL2);
        when(priceRepository.findByCodeAndLimit(anyString(), anyInt())).thenReturn(prices.subList(prices.size() - 400, prices.size()));
        CompletableFuture<SendResult<String, Price>> fut = kafkaPublisher.publish(CODE, prices.getLast());
        fut.handle(handler(sr -> {
            log.info("sendResult: {}", sr);
            }, ex ->
                log.info(ex.getMessage(), ex))
        );

        SendResult<String, Price> sr = fut.get(1000, TimeUnit.MILLISECONDS);
        sleep(10000);
        ArgumentCaptor<TAContainer> captor = ArgumentCaptor.forClass(TAContainer.class);
        verify(taContainerRepository, times(1)).save(captor.capture());
        assertEquals(161.71, captor.getValue().getEma200().getPrice(), 0.01);
    }
}
