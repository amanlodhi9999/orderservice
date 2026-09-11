package com.flashsale.orderservice.service;

import com.flashsale.orderservice.entity.Product;
import com.flashsale.orderservice.exception.ProductOutOfStockException;
import com.flashsale.orderservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    private static final String STOCK_KEY_PREFIX = "product:stock:";
    private static final String LOCK_KEY_PREFIX = "lock:product:";

    // 1. Pre-warm Redis Cache with Product Stock from PostgreSQL
    public void preloadProductStock(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        String stockKey = STOCK_KEY_PREFIX + productId;
        redisTemplate.opsForValue().set(stockKey, String.valueOf(product.getAvailableStock()));
        log.info("Preloaded stock for product ID {} into Redis: {}", productId, product.getAvailableStock());
    }

    // 2. High-Concurrency Stock Decrement using Distributed Locking
    public boolean deductStock(Long productId, int quantity) {
        String lockKey = LOCK_KEY_PREFIX + productId;
        String stockKey = STOCK_KEY_PREFIX + productId;

        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Try to acquire the distributed lock (wait up to 2s, auto-release after 5s)
            boolean isLocked = lock.tryLock(2, 5, TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("Could not acquire lock for product ID: {}", productId);
                throw new RuntimeException("Server is busy. Please try again.");
            }

            // Check current stock in Redis RAM
            String currentStockStr = redisTemplate.opsForValue().get(stockKey);
            if (currentStockStr == null) {
                throw new RuntimeException("Product is not available for flash sale.");
            }

            int currentStock = Integer.parseInt(currentStockStr);
            if (currentStock < quantity) {
                log.info("Flash sale stock depleted for product ID: {}", productId);
                throw new ProductOutOfStockException("Item is OUT OF STOCK!");
            }

            // Atomically decrement stock in Redis
            redisTemplate.opsForValue().decrement(stockKey, quantity);
            log.info("Stock successfully deducted in Redis for product ID: {}. Remaining: {}", 
                     productId, currentStock - quantity);
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting for lock", e);
        } finally {
            // Always release lock if held by the current thread
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}