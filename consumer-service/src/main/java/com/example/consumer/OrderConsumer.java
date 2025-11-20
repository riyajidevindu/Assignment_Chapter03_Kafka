package com.example.consumer;

import com.example.avro.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.Random;

@Component
public class OrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("0.00");

    private final Random random = new Random();
    private double totalPrice = 0;
    private int count = 0;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000L),
            autoCreateTopics = "true",
            dltTopicSuffix = "_dlq"
    )
    @KafkaListener(topics = "${ORDERS_TOPIC:orders}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(Order order, Acknowledgment acknowledgement) {
        log.info("Received order {} for {} priced at {}", order.getOrderId(), order.getProduct(), order.getPrice());
        maybeFail();
        totalPrice += order.getPrice();
        count++;
        double average = totalPrice / count;
        log.info("Running average after {} orders: {}", count, PRICE_FORMAT.format(average));
        acknowledgement.acknowledge();
    }

    @DltHandler
    public void handleDeadLetter(Order order, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Order {} routed to DLQ {} after retries", order.getOrderId(), topic);
    }

    private void maybeFail() {
        if (random.nextInt(10) < 2) {
            throw new RuntimeException("Simulated failure");
        }
    }
}
