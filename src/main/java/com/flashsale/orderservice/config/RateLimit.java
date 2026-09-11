package com.flashsale.orderservice.config;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int maxRequests() default 5;       // Maximum requests allowed
    int windowSeconds() default 10;     // Time window in seconds
}