package com.flashsale.orderservice.controller;

import com.flashsale.orderservice.config.RateLimit;
import com.flashsale.orderservice.dto.OrderRequest;
import com.flashsale.orderservice.dto.OrderResponse;
import com.flashsale.orderservice.service.InventoryService;
import com.flashsale.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final InventoryService inventoryService;

    // 1. Endpoint to pre-warm Redis with stock before the flash sale goes live
    @PostMapping("/preload/{productId}")
    public ResponseEntity<String> preloadProduct(@PathVariable Long productId) {
        inventoryService.preloadProductStock(productId);
        return ResponseEntity.ok("Product " + productId + " stock successfully preloaded into Redis cache!");
    }

    // 2. High-concurrency flash sale order placement
    @PostMapping
    @RateLimit(maxRequests = 5, windowSeconds = 10)
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.placeFlashSaleOrder(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    // 3. Check order status using tracking ID
    @GetMapping("/{trackingId}")
    public ResponseEntity<OrderResponse> getOrderStatus(@PathVariable String trackingId) {
        OrderResponse response = orderService.getOrderStatus(trackingId);
        return ResponseEntity.ok(response);
    }
}