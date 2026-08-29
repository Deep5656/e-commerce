package com.deepweb.Order_Service.event;

import java.util.concurrent.ExecutionException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void sendOrderPlacedEvent(OrderPlacedEvent event) {
        log.info("Sending order event to Kafka. topic=notificationTopic, orderId={}", event.getOrderId());

        CompletableFuture<SendResult<String, OrderPlacedEvent>> future = kafkaTemplate.send("notificationTopic", event.getOrderId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Order event sent successfully. topic=notificationTopic, partition={}, offset={}, orderId={}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getOrderId());
            } else {
                log.error("Unable to send order event to Kafka. topic=notificationTopic, orderId={}", event.getOrderId(), ex);
            }
        });
    }
}
