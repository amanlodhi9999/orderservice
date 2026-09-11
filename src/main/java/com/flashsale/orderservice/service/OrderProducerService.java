package com.flashsale.orderservice.service;

import com.flashsale.orderservice.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProducerService {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    @Value("${app.kafka.order-topic:flash-sale-orders}")
    private String orderTopic;

    public void sendOrderEvent(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent to Kafka topic [{}]: TrackingId={}", orderTopic, event.orderTrackingId());
        // We use productId as the Kafka partition key to ensure order operations on the same product maintain order
        kafkaTemplate.send(orderTopic, String.valueOf(event.productId()), event);
    }
}