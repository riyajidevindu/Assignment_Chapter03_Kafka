package com.example.producer;

import com.example.avro.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
public class OrderGenerator {

    private static final List<String> PRODUCTS = List.of("Laptop", "Phone", "Tablet", "Headphones", "Camera");
    private final Random random = new Random();

    public Order randomOrder() {
        String orderId = UUID.randomUUID().toString();
        String product = PRODUCTS.get(random.nextInt(PRODUCTS.size()));
        float price = 50 + random.nextFloat() * 950;

        return Order.newBuilder()
                .setOrderId(orderId)
                .setProduct(product)
                .setPrice(price)
                .build();
    }
}
