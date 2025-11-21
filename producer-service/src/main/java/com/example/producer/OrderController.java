package com.example.producer;

import com.example.avro.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<String> produceSingle() {
        Order order = orderGenerator.randomOrder();
        orderProducerService.send(order);
        log.info("Requested production of a single order {}", order.getOrderId());
        return ResponseEntity.accepted().body("Order dispatched: " + order.getOrderId());
    }

    @PostMapping({"/produce/bulk", "/api/orders/bulk"})
    public ResponseEntity<String> produceBulk(@RequestParam(name = "count", defaultValue = "10") int count) {
        if (count <= 0) {
            return ResponseEntity.badRequest().body("count must be greater than 0");
        }
        for (int i = 0; i < count; i++) {
            orderProducerService.send(orderGenerator.randomOrder());
        }
        log.info("Requested production of {} orders", count);
        return ResponseEntity.accepted().body("Dispatched " + count + " orders");
    }
}
