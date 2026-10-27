package io.github.dfauth.dormant.actor.config;

import io.github.dfauth.dormant.Dormant;
import io.github.dfauth.dormant.kafka.DormantSerde;
import io.github.dfauth.dormant.kafka.KafkaPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;

import java.util.Collection;


@Slf4j
@Configuration
@EnableAutoConfiguration
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    @Bean
    public <T extends Dormant> DormantSerde<T> dormantSerde() {
        return new DormantSerde<>();
    }

    @Bean
    public <V extends Dormant> ConsumerFactory<String, V> kafkaConsumerFactory(KafkaProperties props, DormantSerde<V> dormantSerde) {

        return new DefaultKafkaConsumerFactory<>(props.buildConsumerProperties(), new StringDeserializer(), dormantSerde.deserializer());
    }

    @Bean
    public <V extends Dormant> ProducerFactory<String, V> kafkaProducerFactory(KafkaProperties props, DormantSerde<V> dormantSerde) {
        return () -> new KafkaProducer<>(props.buildProducerProperties(), new StringSerializer(), dormantSerde.serializer());
    }

    @Bean
    public <V extends Dormant> KafkaTemplate<String, V> kafkaTemplate(ProducerFactory<String, V> factory) {
        return new KafkaTemplate<>(factory);
    }

    @Bean
    public <V extends Dormant> KafkaPublisher<String, V> kafkaPublisher(KafkaTemplate<String, V> kafkaTemplate) {
        return new KafkaPublisher<>(kafkaTemplate, "prices");
    }

    @Bean
    public ConsumerAwareRebalanceListener consumerRebalanceListener() {
        return new ConsumerAwareRebalanceListener() {
            @Override
            public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
                consumer.seekToBeginning(partitions);
            }
        };
    }

    @Bean
    public <V> ConcurrentKafkaListenerContainerFactory<String, V> kafkaListenerContainerFactory(
            ConsumerFactory<String, V> consumerFactory,
            ConsumerAwareRebalanceListener customRebalanceListener) {

        ConcurrentKafkaListenerContainerFactory<String, V> listenerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // Connect the rebalance listener to the container properties
        listenerFactory.getContainerProperties().setConsumerRebalanceListener(customRebalanceListener);

        // Apply auto-configured Spring Boot properties to the factory
        listenerFactory.setConsumerFactory(consumerFactory);
        return listenerFactory;
    }

}
