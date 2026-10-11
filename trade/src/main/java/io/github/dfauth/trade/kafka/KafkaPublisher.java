package io.github.dfauth.trade.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static io.github.dfauth.trycatch.CompletableFutureUtils.*;

@RequiredArgsConstructor
public class KafkaPublisher<K, V> {

    private final KafkaTemplate<K, V> kafkaTemplate;
    private final String topic;

    public CompletableFuture<SendResult<K,V>> publish(K code, V p) {
        return kafkaTemplate.send(topic, code, p).handle(onException(logIt.andThen(returnNull())));
    }
}
