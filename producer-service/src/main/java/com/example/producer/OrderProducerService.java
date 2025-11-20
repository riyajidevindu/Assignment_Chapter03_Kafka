package com.example.producer;

import com.example.avro.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderProducerService {

    private static final Logger log = LoggerFactory.getLogger(OrderProducerService.class);

    private final KafkaTemplate<String, Order> kafkaTemplate;
    private final String topicName;

    public OrderProducerService(KafkaTemplate<String, Order> kafkaTemplate,
                                @Value("${ORDERS_TOPIC:orders}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void send(Order order) {
        kafkaTemplate.send(topicName, order.getOrderId(), order)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish order {}", order.getOrderId(), ex);
                    } else if (result != null && result.getRecordMetadata() != null) {
                        log.info("Published order {} to partition {}", order.getOrderId(),
                                result.getRecordMetadata().partition());
                    } else {
                        log.info("Published order {}", order.getOrderId());
                    }
                });
    }
}
