package com.muni.demo.config;

import com.muni.demo.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interceptor to enforce API rate limiting per IP address.
 * Resilient to Redis connection failures with an in-memory fallback.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private static final String REDIS_PREFIX_RATE = "rate:limit:";
    private static final int LIMIT = 60; // 60 requests
    private static final Duration WINDOW = Duration.ofMinutes(1); // per 1 minute

    private final StringRedisTemplate redisTemplate;

    // In-memory rate limiting fallback cache in case Redis goes down
    private final ConcurrentHashMap<String, RequestCounter> fallbackCache = new ConcurrentHashMap<>();

    public RateLimitInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Only rate limit API calls
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return true;
        }

        String clientIp = getClientIp(request);

        try {
            // Try rate limiting using Redis
            boolean allowed = checkRedisRateLimit(clientIp);
            if (!allowed) {
                throw new RateLimitExceededException("API rate limit exceeded. Max " + LIMIT + " requests per minute.");
            }
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis rate limiter failed, falling back to in-memory: {}", e.getMessage());
            // Fallback to In-Memory rate limiting
            boolean allowed = checkInMemoryRateLimit(clientIp);
            if (!allowed) {
                throw new RateLimitExceededException("API rate limit exceeded (Fallback). Max " + LIMIT + " requests per minute.");
            }
        }

        return true;
    }

    private boolean checkRedisRateLimit(String ip) {
        String key = REDIS_PREFIX_RATE + ip;
        String countStr = redisTemplate.opsForValue().get(key);

        if (countStr == null) {
            // Initialize window
            redisTemplate.opsForValue().set(key, "1", WINDOW);
            return true;
        }

        int count = Integer.parseInt(countStr);
        if (count >= LIMIT) {
            return false;
        }

        redisTemplate.opsForValue().increment(key);
        return true;
    }

    private boolean checkInMemoryRateLimit(String ip) {
        long now = System.currentTimeMillis();
        RequestCounter counter = fallbackCache.compute(ip, (key, existing) -> {
            if (existing == null || now > existing.windowEndTime) {
                return new RequestCounter(1, now + WINDOW.toMillis());
            } else {
                existing.count.incrementAndGet();
                return existing;
            }
        });

        return counter.count.get() <= LIMIT;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    /**
     * Inner class to keep track of requests for in-memory rate limiting.
     */
    private static class RequestCounter {
        final AtomicInteger count;
        final long windowEndTime;

        RequestCounter(int initialCount, long windowEndTime) {
            this.count = new AtomicInteger(initialCount);
            this.windowEndTime = windowEndTime;
        }
    }
}
