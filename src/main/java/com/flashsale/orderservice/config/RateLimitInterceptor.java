package com.flashsale.orderservice.config;

import com.flashsale.orderservice.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        String clientIp = request.getRemoteAddr();
        String redisKey = "rate_limit:" + request.getRequestURI() + ":" + clientIp;

        Long currentCount = redisTemplate.opsForValue().increment(redisKey);

        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(rateLimit.windowSeconds()));
        }

        if (currentCount != null && currentCount > rateLimit.maxRequests()) {
            throw new RateLimitExceededException(
                "Too many requests! Allowed: " + rateLimit.maxRequests() + " per " + rateLimit.windowSeconds() + " seconds."
            );
        }

        return true;
    }
}