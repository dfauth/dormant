package io.github.dfauth.trade.config;

import io.github.dfauth.dormant.Dormant;
import io.github.dfauth.trade.kafka.DormantSerde;
import io.github.dfauth.trade.kafka.KafkaPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

@Slf4j
@Configuration
public class KafkaConfig {

    @Bean
    public Map<String, Object> kafkaProperties() {
        return Map.of(
                CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092"
        );
    }

    @Bean
    public <T extends Dormant> DormantSerde<T> dormantSerde() {
        return new DormantSerde<>();
    }

    @Bean
    public <V extends Dormant> ProducerFactory<String, V> kafkaProducerFactory(@Qualifier("kafkaProperties") Map<String, Object> props, DormantSerde<V> dormantSerde) {
        return () -> new KafkaProducer<>(props, new StringSerializer(), dormantSerde.serializer());
    }

    @Bean
    public <V extends Dormant> KafkaTemplate<String, V> kafkaTemplate(ProducerFactory<String, V> factory) {
        return new KafkaTemplate<>(factory);
    }

    @Bean
    public <V extends Dormant> KafkaPublisher<String, V> kafkaPublisher(KafkaTemplate<String, V> kafkaTemplate) {
        return new KafkaPublisher<>(kafkaTemplate, "prices");
    }

}
