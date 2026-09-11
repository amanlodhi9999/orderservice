package com.flashsale.orderservice;

import com.flashsale.orderservice.dto.OrderRequest;
import com.flashsale.orderservice.entity.Product;
import com.flashsale.orderservice.repository.OrderRepository;
import com.flashsale.orderservice.repository.ProductRepository;
import com.flashsale.orderservice.service.InventoryService;
import com.flashsale.orderservice.service.OrderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
public class FlashSaleConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Long testProductId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();

        // 1. Create a product with exactly 10 units in stock
        Product product = Product.builder()
                .name("Concurrency Test Phone")
                .price(new BigDecimal("999.00"))
                .totalStock(10)
                .availableStock(10)
                .build();
        product = productRepository.save(product);
        testProductId = product.getId();

        // 2. Pre-warm stock into Redis
        inventoryService.preloadProductStock(testProductId);
    }

    @Test
    void testConcurrentOrders_ZeroOversellGuarantee() throws InterruptedException {
        int numberOfThreads = 100; // 100 concurrent requests racing for 10 items
        ExecutorService executorService = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final long userId = 1000 + i;
            executorService.submit(() -> {
                try {
                    OrderRequest request = new OrderRequest(userId, testProductId, 1);
                    orderService.placeFlashSaleOrder(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // Wait for all 100 threads to finish
        executorService.shutdown();

        // Allow async Kafka consumer 3 seconds to persist all orders into Postgres
        Thread.sleep(3000);

        System.out.println("================ CONCURRENCY RESULTS ================");
        System.out.println("Total Concurrent Requests: " + numberOfThreads);
        System.out.println("Successful Orders Placed: " + successCount.get());
        System.out.println("Failed (Out of Stock) Orders: " + failureCount.get());
        System.out.println("Persisted Orders in DB: " + orderRepository.count());
        System.out.println("=====================================================");

        // Assertions proving zero overselling
        Assertions.assertEquals(10, successCount.get(), "Exactly 10 orders must succeed");
        Assertions.assertEquals(90, failureCount.get(), "Exactly 90 orders must fail");
        Assertions.assertEquals(10, orderRepository.count(), "DB must contain exactly 10 orders");
    }
}