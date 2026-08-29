package com.deepweb.Notification_Service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;

import com.deepweb.Notification_Service.event.OrderPlacedEvent;

/**
 * End-to-end check that a message produced the way Order-Service produces it
 * (JSON value, no type headers) actually reaches {@code NotificationConsumer} and
 * gets logged. This is the regression guard for "the consumer prints nothing".
 */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "eureka.client.enabled=false"
})
@EmbeddedKafka(partitions = 1, topics = "notificationTopic")
@ExtendWith(OutputCaptureExtension.class)
class NotificationConsumerTest {

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    void consumesOrderPlacedEventAndLogsIt(CapturedOutput output) throws Exception {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put("bootstrap.servers", broker.getBrokersAsString());
        producerProps.put("key.serializer", StringSerializer.class);
        producerProps.put("value.serializer", JsonSerializer.class);
        producerProps.put("spring.json.add.type.headers", false);

        KafkaTemplate<String, OrderPlacedEvent> template =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));

        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId("ORD-TEST-1")
                .skuCodes(List.of("iphone_13", "iphone_13_red"))
                .message("Order placed successfully for: iphone_13, iphone_13_red")
                .build();

        template.send("notificationTopic", event.getOrderId(), event).get(10, TimeUnit.SECONDS);

        awaitLog(output, "Received notification for Order ID: ORD-TEST-1");

        assertThat(output).contains("Kafka message received.");
        assertThat(output).contains("Items: iphone_13, iphone_13_red");
    }

    /** Payload without skuCodes - used to NPE inside the listener and retry silently. */
    @Test
    void toleratesEventWithoutSkuCodes(CapturedOutput output) throws Exception {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put("bootstrap.servers", broker.getBrokersAsString());
        producerProps.put("key.serializer", StringSerializer.class);
        producerProps.put("value.serializer", StringSerializer.class);

        KafkaTemplate<String, String> template =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));

        template.send("notificationTopic", "ORD-TEST-2",
                "{\"orderId\":\"ORD-TEST-2\",\"message\":\"no skus here\"}").get(10, TimeUnit.SECONDS);

        awaitLog(output, "Received notification for Order ID: ORD-TEST-2");

        assertThat(output).contains("Items: (none)");
    }

    private static void awaitLog(CapturedOutput output, String expected) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        while (Instant.now().isBefore(deadline)) {
            if (output.getAll().contains(expected)) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(200);
        }
        assertThat(output.getAll()).contains(expected);
    }
}
