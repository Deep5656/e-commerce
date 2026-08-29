package com.deepweb.Notification_Service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootTest(properties = {
		"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
		"eureka.client.enabled=false"
})
@EmbeddedKafka(partitions = 1, topics = "notificationTopic")
class NotificationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
