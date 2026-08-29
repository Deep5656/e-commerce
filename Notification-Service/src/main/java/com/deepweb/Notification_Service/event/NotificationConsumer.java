package com.deepweb.Notification_Service.event;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.deepweb.Notification_Service.config.KafkaConsumerConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(id = "notification-listener",
            topics = KafkaConsumerConfig.NOTIFICATION_TOPIC,
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderPlacedEvent(ConsumerRecord<String, String> record) {
        // Log partition/offset too - without them you cannot tell a re-delivered
        // message from a new one when debugging "did my consumer actually run?".
        log.info("Kafka message received. topic={}, partition={}, offset={}, key={}, value={}",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());

        try {
            OrderPlacedEvent event = objectMapper.readValue(record.value(), OrderPlacedEvent.class);
            log.info("Received notification for Order ID: {}", event.getOrderId());
            log.info("Message: {}", event.getMessage());

            // skuCodes is absent whenever the producer sends a partial payload; the old
            // String.join blew up with an NPE here, which the container then retried
            // ~10 times before logging - easy to mistake for "nothing happened".
            List<String> skuCodes = event.getSkuCodes();
            log.info("Items: {}", skuCodes == null || skuCodes.isEmpty() ? "(none)" : String.join(", ", skuCodes));
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Kafka message at offset {}: {}", record.offset(), record.value(), e);
        }
    }
}
