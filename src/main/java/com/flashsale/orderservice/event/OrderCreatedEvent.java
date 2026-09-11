package com.flashsale.orderservice.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCreatedEvent(
        String orderTrackingId,
        Long userId,
        Long productId,
        Integer quantity,
        BigDecimal pricePerUnit,
        LocalDateTime timestamp
)implements Serializable {}
