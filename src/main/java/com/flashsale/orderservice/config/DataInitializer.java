package com.flashsale.orderservice.config;

import com.flashsale.orderservice.entity.Product;
import com.flashsale.orderservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final ProductRepository productRepository;

    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            if (productRepository.count() == 0) {
                Product product = Product.builder()
                        .name("Limited Edition Gaming Laptop")
                        .price(new BigDecimal("1299.99"))
                        .totalStock(10)      // Total 10 units available for the sale
                        .availableStock(10)
                        .build();

                Product saved = productRepository.save(product);
                log.info("Initial product seeded into PostgreSQL with ID: {}", saved.getId());
            }
        };
    }
}