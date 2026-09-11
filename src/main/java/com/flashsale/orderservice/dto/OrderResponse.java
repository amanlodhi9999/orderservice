package com.flashsale.orderservice.dto;

import com.flashsale.orderservice.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        String orderTrackingId,
        Long userId,
        Long productId,
        Integer quantity,
        BigDecimal totalAmount,
        OrderStatus status,
        String message,
        LocalDateTime createdAt
) {}
