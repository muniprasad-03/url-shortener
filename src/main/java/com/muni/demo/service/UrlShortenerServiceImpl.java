package com.muni.demo.service;

import com.muni.demo.dto.UrlAnalyticsResponse;
import com.muni.demo.dto.UrlRequest;
import com.muni.demo.dto.UrlResponse;
import com.muni.demo.entity.UrlMapping;
import com.muni.demo.exception.DuplicateShortCodeException;
import com.muni.demo.exception.InvalidURLException;
import com.muni.demo.exception.ResourceNotFoundException;
import com.muni.demo.repository.UrlMappingRepository;
import com.muni.demo.util.Base62;
import com.muni.demo.util.QrCodeGenerator;
import com.muni.demo.util.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Implementation of {@link UrlShortenerService}.
 */
@Service
public class UrlShortenerServiceImpl implements UrlShortenerService {

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerServiceImpl.class);

    private static final String REDIS_PREFIX_ORIGINAL = "url:original:";
    private static final String REDIS_PREFIX_CLICKS = "url:clicks:";
    private static final String REDIS_PREFIX_LAST_ACCESS = "url:last_accessed:";
    private static final int MAX_COLLISION_RETRIES = 5;

    private final UrlMappingRepository urlMappingRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * Constructor injection only, matching the SOLID/Spring best practices.
     */
    public UrlShortenerServiceImpl(UrlMappingRepository urlMappingRepository,
                                   StringRedisTemplate redisTemplate) {
        this.urlMappingRepository = urlMappingRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public UrlResponse shortenUrl(UrlRequest request, String baseUrl) {
        String originalUrl = request.getOriginalUrl().trim();

        // 1. Validate original URL
        if (!UrlValidator.isValidUrl(originalUrl)) {
            throw new InvalidURLException("The URL format is invalid. It must start with http:// or https://");
        }

        // 2. Validate expiration date
        if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidURLException("Expiration date cannot be in the past");
        }

        String shortCode;

        // 3. Handle Custom Alias
        if (request.getCustomAlias() != null && !request.getCustomAlias().trim().isEmpty()) {
            String alias = request.getCustomAlias().trim();
            // Validate alias format (alphanumeric and dashes/underscores)
            if (!alias.matches("^[a-zA-Z0-9-_]+$")) {
                throw new InvalidURLException("Custom alias can only contain letters, numbers, hyphens, and underscores");
            }
            if (urlMappingRepository.existsByShortCode(alias)) {
                throw new DuplicateShortCodeException("Custom alias '" + alias + "' is already in use");
            }
            shortCode = alias;
        } else {
            // 4. Generate Random Short Code and handle collisions
            shortCode = generateUniqueShortCode();
        }

        // 5. Create and Save the Entity
        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl(originalUrl)
                .shortCode(shortCode)
                .customAlias(request.getCustomAlias() != null ? request.getCustomAlias().trim() : null)
                .expiresAt(request.getExpiresAt())
                .clickCount(0L)
                .build();

        UrlMapping savedMapping = urlMappingRepository.save(urlMapping);
        String finalShortCode = savedMapping.getShortCode();
        log.info("Successfully shortened URL: {} -> {}", originalUrl, finalShortCode);

        // 6. Build the Response details
        String shortUrl = baseUrl + "/" + finalShortCode;
        String qrCodeBase64 = generateQrCodeSilently(shortUrl);

        // Cache the newly created URL to Redis
        cacheOriginalUrlSilently(finalShortCode, originalUrl, request.getExpiresAt());

        return UrlResponse.builder()
                .originalUrl(savedMapping.getOriginalUrl())
                .shortCode(finalShortCode)
                .shortUrl(shortUrl)
                .clickCount(savedMapping.getClickCount())
                .createdAt(savedMapping.getCreatedAt())
                .expiresAt(savedMapping.getExpiresAt())
                .qrCodeBase64(qrCodeBase64)
                .build();
    }

    @Override
    @Transactional
    public String getOriginalUrl(String shortCode) {
        // 1. Try to load from Redis Cache (resilient to Redis failures)
        String cachedOriginalUrl = getCachedOriginalUrlSilently(shortCode);
        if (cachedOriginalUrl != null) {
            log.debug("Redis cache hit for short code: {}", shortCode);
            incrementClicksInRedisSilently(shortCode);
            return cachedOriginalUrl;
        }

        // 2. Fallback to MySQL DB
        log.debug("Redis cache miss. Fetching from MySQL database for short code: {}", shortCode);
        UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL code '" + shortCode + "' not found"));

        // 3. Check for expiration
        if (mapping.isExpired()) {
            // Delete expired URLs to clean up
            urlMappingRepository.delete(mapping);
            evictCacheSilently(shortCode);
            throw new ResourceNotFoundException("Short URL code '" + shortCode + "' has expired");
        }

        // 4. Cache back to Redis
        cacheOriginalUrlSilently(shortCode, mapping.getOriginalUrl(), mapping.getExpiresAt());
        incrementClicksInRedisSilently(shortCode);

        return mapping.getOriginalUrl();
    }

    @Override
    @Transactional(readOnly = true)
    public UrlAnalyticsResponse getAnalytics(String shortCode) {
        UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL code '" + shortCode + "' not found"));

        if (mapping.isExpired()) {
            throw new ResourceNotFoundException("Short URL code '" + shortCode + "' has expired");
        }

        // Check if there are unsynced clicks in Redis to show accurate statistics
        long cachedClicks = getCachedClicksSilently(shortCode);
        long totalClicks = mapping.getClickCount() + cachedClicks;

        LocalDateTime lastAccessed = mapping.getLastAccessedAt();
        String cachedLastAccessStr = getCachedLastAccessSilently(shortCode);
        if (cachedLastAccessStr != null) {
            lastAccessed = LocalDateTime.parse(cachedLastAccessStr);
        }

        return UrlAnalyticsResponse.builder()
                .originalUrl(mapping.getOriginalUrl())
                .shortCode(mapping.getShortCode())
                .clickCount(totalClicks)
                .createdAt(mapping.getCreatedAt())
                .updatedAt(mapping.getUpdatedAt())
                .lastAccessedAt(lastAccessed)
                .expiresAt(mapping.getExpiresAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteUrl(String shortCode) {
        UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL code '" + shortCode + "' not found"));

        urlMappingRepository.delete(mapping);
        log.info("Deleted URL mapping for short code: {}", shortCode);

        // Evict from Cache
        evictCacheSilently(shortCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UrlResponse> getAllUrls(Pageable pageable, String baseUrl) {
        return urlMappingRepository.findAll(pageable).map(mapping -> {
            String shortUrl = baseUrl + "/" + mapping.getShortCode();
            return UrlResponse.builder()
                    .originalUrl(mapping.getOriginalUrl())
                    .shortCode(mapping.getShortCode())
                    .shortUrl(shortUrl)
                    .clickCount(mapping.getClickCount())
                    .createdAt(mapping.getCreatedAt())
                    .expiresAt(mapping.getExpiresAt())
                    .qrCodeBase64(generateQrCodeSilently(shortUrl))
                    .build();
        });
    }

    /**
     * Helper to generate a unique short code with collision handling.
     */
    private String generateUniqueShortCode() {
        int length = 6;
        for (int i = 0; i < MAX_COLLISION_RETRIES; i++) {
            String code = Base62.generateRandomCode(length);
            if (!urlMappingRepository.existsByShortCode(code)) {
                return code;
            }
            log.warn("Collision detected for code: {}. Retrying...", code);
            // Increase length slightly if we keep colliding
            if (i > 2) {
                length++;
            }
        }
        throw new RuntimeException("Failed to generate a unique short code after " + MAX_COLLISION_RETRIES + " attempts");
    }

    /**
     * Generate QR Code and handle exceptions silently.
     */
    private String generateQrCodeSilently(String text) {
        try {
            return QrCodeGenerator.generateQrCodeBase64(text, 250, 250);
        } catch (Exception e) {
            log.error("Failed to generate QR Code for URL: {}", text, e);
            return null;
        }
    }

    // ==========================================
    // RESILIENT REDIS CACHE OPERATIONS
    // ==========================================

    private void cacheOriginalUrlSilently(String shortCode, String originalUrl, LocalDateTime expiresAt) {
        try {
            String key = REDIS_PREFIX_ORIGINAL + shortCode;
            if (expiresAt != null) {
                Duration ttl = Duration.between(LocalDateTime.now(), expiresAt);
                if (!ttl.isNegative()) {
                    redisTemplate.opsForValue().set(key, originalUrl, ttl);
                }
            } else {
                // Cache for 24 hours by default
                redisTemplate.opsForValue().set(key, originalUrl, Duration.ofHours(24));
            }
        } catch (Exception e) {
            log.error("Redis error while caching URL mapping for: {}", shortCode, e);
        }
    }

    private String getCachedOriginalUrlSilently(String shortCode) {
        try {
            return redisTemplate.opsForValue().get(REDIS_PREFIX_ORIGINAL + shortCode);
        } catch (Exception e) {
            log.error("Redis error while retrieving cached URL mapping for: {}", shortCode, e);
            return null;
        }
    }

    private void incrementClicksInRedisSilently(String shortCode) {
        try {
            redisTemplate.opsForValue().increment(REDIS_PREFIX_CLICKS + shortCode);
            redisTemplate.opsForValue().set(REDIS_PREFIX_LAST_ACCESS + shortCode, LocalDateTime.now().toString());
        } catch (Exception e) {
            log.error("Redis error while incrementing clicks for: {}", shortCode, e);
        }
    }

    private long getCachedClicksSilently(String shortCode) {
        try {
            String countStr = redisTemplate.opsForValue().get(REDIS_PREFIX_CLICKS + shortCode);
            return countStr != null ? Long.parseLong(countStr) : 0L;
        } catch (Exception e) {
            log.error("Redis error while reading clicks from cache for: {}", shortCode, e);
            return 0L;
        }
    }

    private String getCachedLastAccessSilently(String shortCode) {
        try {
            return redisTemplate.opsForValue().get(REDIS_PREFIX_LAST_ACCESS + shortCode);
        } catch (Exception e) {
            log.error("Redis error while reading last access from cache for: {}", shortCode, e);
            return null;
        }
    }

    private void evictCacheSilently(String shortCode) {
        try {
            redisTemplate.delete(REDIS_PREFIX_ORIGINAL + shortCode);
            redisTemplate.delete(REDIS_PREFIX_CLICKS + shortCode);
            redisTemplate.delete(REDIS_PREFIX_LAST_ACCESS + shortCode);
        } catch (Exception e) {
            log.error("Redis error while evicting cache for: {}", shortCode, e);
        }
    }
}
