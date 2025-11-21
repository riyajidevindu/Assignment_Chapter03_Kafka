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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Component
public class OrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("0.00");

    private final Random random = new Random();
    private final Map<String, ProductStats> productAverages = new HashMap<>();
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
        ProductStats stats = productAverages.computeIfAbsent(order.getProduct(), key -> new ProductStats());
        stats.add(order.getPrice());

        totalPrice += order.getPrice();
        count++;

        double productAverage = stats.average();
        double overallAverage = totalPrice / count;
        log.info("Averages => {} [{}]: {} | Overall [{}]: {}",
            order.getProduct(),
            stats.count,
            PRICE_FORMAT.format(productAverage),
            count,
            PRICE_FORMAT.format(overallAverage));
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

    private static class ProductStats {
        private double total = 0;
        private int count = 0;

        void add(double price) {
            this.total += price;
            this.count++;
        }

        double average() {
            return count == 0 ? 0 : total / count;
        }
    }
}
