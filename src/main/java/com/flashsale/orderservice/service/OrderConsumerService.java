package com.flashsale.orderservice.service;

import com.flashsale.orderservice.entity.Order;
import com.flashsale.orderservice.entity.OrderStatus;
import com.flashsale.orderservice.entity.Product;
import com.flashsale.orderservice.event.OrderCreatedEvent;
import com.flashsale.orderservice.repository.OrderRepository;
import com.flashsale.orderservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderConsumerService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @KafkaListener(
        topics = "${app.kafka.order-topic:flash-sale-orders}",
        groupId = "${spring.kafka.consumer.group-id:flash-sale-group}"
    )
    @Transactional
    public void processOrderEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent from Kafka for TrackingId: {}", event.orderTrackingId());

        try {
            // 1. Fetch Product from DB
            Product product = productRepository.findById(event.productId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + event.productId()));

            // 2. Double check DB-level stock
            if (product.getAvailableStock() < event.quantity()) {
                log.error("DB Stock mismatch for product ID: {}. Processing failed.", event.productId());
                return;
            }

            // 3. Deduct stock in Postgres DB
            product.setAvailableStock(product.getAvailableStock() - event.quantity());
            productRepository.save(product);

            // 4. Calculate total amount
            BigDecimal totalAmount = event.pricePerUnit().multiply(BigDecimal.valueOf(event.quantity()));

            // 5. Persist confirmed Order in Postgres DB
            Order order = Order.builder()
                    .orderTrackingId(event.orderTrackingId())
                    .userId(event.userId())
                    .productId(event.productId())
                    .quantity(event.quantity())
                    .totalAmount(totalAmount)
                    .status(OrderStatus.CONFIRMED)
                    .build();

            orderRepository.save(order);
            log.info("Order successfully persisted to PostgreSQL for TrackingId: {}", event.orderTrackingId());

        } catch (Exception e) {
            log.error("Error processing order from Kafka for TrackingId: {}", event.orderTrackingId(), e);
        }
    }
}