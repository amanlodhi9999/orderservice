package com.flashsale.orderservice.service;

import com.flashsale.orderservice.dto.OrderRequest;
import com.flashsale.orderservice.dto.OrderResponse;
import com.flashsale.orderservice.entity.OrderStatus;
import com.flashsale.orderservice.entity.Product;
import com.flashsale.orderservice.event.OrderCreatedEvent;
import com.flashsale.orderservice.repository.OrderRepository;
import com.flashsale.orderservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final OrderProducerService orderProducerService;

    public OrderResponse placeFlashSaleOrder(OrderRequest request) {
        // 1. Fetch product to get price per unit
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + request.productId()));

        // 2. High-speed atomic stock deduction in Redis RAM
        inventoryService.deductStock(request.productId(), request.quantity());

        // 3. Generate unique tracking ID
        String trackingId = UUID.randomUUID().toString();

        // 4. Build lightweight event for Kafka
        OrderCreatedEvent event = new OrderCreatedEvent(
                trackingId,
                request.userId(),
                request.productId(),
                request.quantity(),
                product.getPrice(),
                LocalDateTime.now()
        );

        // 5. Asynchronously publish event to Kafka
        orderProducerService.sendOrderEvent(event);

        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(request.quantity()));

        // 6. Return immediate 202 Accepted Response
        return new OrderResponse(
                trackingId,
                request.userId(),
                request.productId(),
                request.quantity(),
                totalAmount,
                OrderStatus.PENDING,
                "Order request accepted and queued for processing",
                LocalDateTime.now()
        );
    }

    public OrderResponse getOrderStatus(String trackingId) {
        return orderRepository.findByOrderTrackingId(trackingId)
                .map(order -> new OrderResponse(
                        order.getOrderTrackingId(),
                        order.getUserId(),
                        order.getProductId(),
                        order.getQuantity(),
                        order.getTotalAmount(),
                        order.getStatus(),
                        "Order status fetched successfully",
                        order.getCreatedAt()
                ))
                .orElseThrow(() -> new RuntimeException("Order not found with tracking ID: " + trackingId));
    }
}