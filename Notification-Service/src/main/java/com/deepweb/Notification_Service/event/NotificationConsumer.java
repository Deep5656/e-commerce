package com.deepweb.Notification_Service.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "notificationTopic", groupId = "notification-group", autoStartup = "true")
    public void handleOrderPlacedEvent(
            String eventJson,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Kafka message received: topic=notificationTopic, partition={}, offset={}, payload={}",
                partition, offset, eventJson);

        try {
            OrderPlacedEvent event = objectMapper.readValue(eventJson, OrderPlacedEvent.class);
            log.info("Received notification for Order ID: {}", event.getOrderId());
            log.info("Message: {}", event.getMessage());
            log.info("Items: {}", String.join(", ", event.getSkuCodes()));
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Kafka message: {}", e.getMessage(), e);
        }
    }
}
