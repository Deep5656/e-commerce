package com.deepweb.Notification_Service.config;

import java.util.Collection;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.util.backoff.FixedBackOff;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer-side Kafka wiring for the notification listener.
 *
 * <p>Everything here exists to make a non-consuming consumer *visible*. Previously the
 * only signal that the listener worked was the log line inside the listener itself, so
 * "no logs" was ambiguous between "not subscribed", "no messages", "broker unreachable"
 * and "handler blowing up".
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    public static final String NOTIFICATION_TOPIC = "notificationTopic";

    private final KafkaListenerEndpointRegistry listenerRegistry;

    /**
     * Create the topic up front via KafkaAdmin rather than leaning on the broker's
     * auto-create. Auto-create is what made the old behaviour confusing: the topic
     * appeared the moment the consumer subscribed, so seeing it in Kafka UI told you
     * nothing about whether anything had ever been produced to it.
     */
    @Bean
    public NewTopic notificationTopic() {
        return TopicBuilder.name(NOTIFICATION_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Container factory with a bounded error handler and a rebalance listener.
     *
     * <p>The default error handler retries a failing record ~10 times and then logs;
     * an exception in the handler could previously look like silence. Partition
     * assignment logging is the positive proof that the consumer joined the group.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        factory.setCommonErrorHandler(new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "Giving up on record after retries. topic={}, partition={}, offset={}, value={}",
                        record.topic(), record.partition(), record.offset(), record.value(), exception),
                new FixedBackOff(1000L, 2L)));

        factory.getContainerProperties().setConsumerRebalanceListener(new ConsumerAwareRebalanceListener() {
            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                log.info("Kafka partitions assigned to notification-group: {}", partitions);
            }

            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                log.info("Kafka partitions revoked from notification-group: {}", partitions);
            }
        });

        return factory;
    }

    /**
     * One unambiguous line at startup saying whether the listener container is actually
     * running. If this says {@code running=false} the problem is registration, not Kafka.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void reportListenerState() {
        listenerRegistry.getListenerContainers().forEach(container -> log.info(
                "Kafka listener container '{}' -> running={}, topics={}",
                container.getListenerId(),
                container.isRunning(),
                container.getContainerProperties().getTopics() == null
                        ? "(pattern/partitions)"
                        : String.join(", ", container.getContainerProperties().getTopics())));

        if (listenerRegistry.getListenerContainers().isEmpty()) {
            log.error("No Kafka listener containers registered - @KafkaListener beans were not picked up.");
        }
    }
}
