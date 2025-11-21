package com.example.producer;

import com.example.avro.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderGenerator orderGenerator;
    private final OrderProducerService orderProducerService;

    public OrderController(OrderGenerator orderGenerator, OrderProducerService orderProducerService) {
        this.orderGenerator = orderGenerator;
        this.orderProducerService = orderProducerService;
    }

    @PostMapping({"/produce", "/api/orders"})
    public ResponseEntity<String> produceSingle(@RequestBody(required = false) OrderRequest orderRequest) {
        Order order = orderGenerator.fromRequest(orderRequest);
        orderProducerService.send(order);
        log.info("Requested production of a single order {}", order.getOrderId());
        return ResponseEntity.accepted().body("Order dispatched: " + order.getOrderId());
    }

    @PostMapping({"/produce/bulk", "/api/orders/bulk"})
        public ResponseEntity<String> produceBulk(
            @RequestParam(name = "count", defaultValue = "10") int count,
            @RequestBody(required = false) List<OrderRequest> customOrders) {

        if (customOrders != null && !customOrders.isEmpty()) {
            customOrders.stream()
                    .map(orderGenerator::fromRequest)
                    .forEach(orderProducerService::send);
            log.info("Requested production of {} custom orders", customOrders.size());
            return ResponseEntity.accepted().body("Dispatched " + customOrders.size() + " orders");
        }

        if (count <= 0) {
            return ResponseEntity.badRequest().body("count must be greater than 0");
        }

        for (int i = 0; i < count; i++) {
            orderProducerService.send(orderGenerator.randomOrder());
        }
        log.info("Requested production of {} generated orders", count);
        return ResponseEntity.accepted().body("Dispatched " + count + " orders");
    }
}
