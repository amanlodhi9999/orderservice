package com.flashsale.orderservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
@RestControllerAdvice
public class GlobalExceptionHandler{
        @ExceptionHandler(ProductOutOfStockException.class)
        public ResponseEntity<Map<String, Object>> handleOutOfStock(ProductOutOfStockException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "timestamp", LocalDateTime.now(),
                    "status", HttpStatus.BAD_REQUEST.value(),
                    "error", "Flash Sale Out of Stock",
                    "message", ex.getMessage()
            ));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "timestamp", LocalDateTime.now(),
            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "error", "Internal Server Error",
            "message", ex.getMessage()
            ));
        }
        @ExceptionHandler(RateLimitExceededException.class)
        public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExceededException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "timestamp", LocalDateTime.now(),
                    "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                    "error", "Rate Limit Exceeded",
                    "message", ex.getMessage()
            ));
        }
}
