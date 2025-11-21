package com.example.producer;

import com.example.avro.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

    public Order fromRequest(OrderRequest request) {
        Order template = randomOrder();
        if (request == null) {
            return template;
        }

        String orderId = StringUtils.hasText(request.orderId()) ? request.orderId() : template.getOrderId();
        String product = StringUtils.hasText(request.product()) ? request.product() : template.getProduct();
        float price = request.price() != null ? request.price() : template.getPrice();

        return Order.newBuilder()
                .setOrderId(orderId)
                .setProduct(product)
                .setPrice(price)
                .build();
    }
}
