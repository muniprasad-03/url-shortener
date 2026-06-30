package com.muni.demo.service;

import com.muni.demo.repository.UrlMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Scheduled component to sync click metrics from Redis cache into the MySQL database.
 */
@Component
public class ClickSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClickSyncScheduler.class);

    private static final String REDIS_PREFIX_CLICKS = "url:clicks:";
    private static final String REDIS_PREFIX_LAST_ACCESS = "url:last_accessed:";

    private final UrlMappingRepository urlMappingRepository;
    private final StringRedisTemplate redisTemplate;

    public ClickSyncScheduler(UrlMappingRepository urlMappingRepository,
                              StringRedisTemplate redisTemplate) {
        this.urlMappingRepository = urlMappingRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Periodically syncs click counts and last accessed timestamps from Redis to MySQL.
     * Runs every 30 seconds.
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void syncClicksToDatabase() {
        try {
            Set<String> clickKeys = redisTemplate.keys(REDIS_PREFIX_CLICKS + "*");
            if (clickKeys == null || clickKeys.isEmpty()) {
                return;
            }

            log.debug("Found {} short code click records to sync in Redis.", clickKeys.size());

            for (String key : clickKeys) {
                String shortCode = key.substring(REDIS_PREFIX_CLICKS.length());
                String clicksStr = redisTemplate.opsForValue().get(key);

                if (clicksStr != null && !clicksStr.equals("0")) {
                    long clicks = Long.parseLong(clicksStr);

                    // Read and delete last access timestamp
                    String lastAccessKey = REDIS_PREFIX_LAST_ACCESS + shortCode;
                    String lastAccessStr = redisTemplate.opsForValue().get(lastAccessKey);
                    LocalDateTime lastAccessTime = null;

                    if (lastAccessStr != null) {
                        try {
                            lastAccessTime = LocalDateTime.parse(lastAccessStr);
                        } catch (Exception e) {
                            log.error("Failed to parse last accessed time for short code: {}", shortCode, e);
                        }
                    }

                    // Decrement click count in Redis to preserve new clicks during the sync
                    redisTemplate.opsForValue().decrement(key, clicks);
                    redisTemplate.delete(lastAccessKey);

                    // Update MySQL
                    final LocalDateTime finalLastAccessTime = lastAccessTime;
                    urlMappingRepository.findByShortCode(shortCode).ifPresentOrElse(mapping -> {
                        mapping.setClickCount(mapping.getClickCount() + clicks);
                        if (finalLastAccessTime != null) {
                            mapping.setLastAccessedAt(finalLastAccessTime);
                        }
                        urlMappingRepository.save(mapping);
                        log.debug("Synced {} clicks for short code: {}", clicks, shortCode);
                    }, () -> {
                        log.warn("Mapping for short code '{}' not found in database during sync. Purging Redis metrics.", shortCode);
                        redisTemplate.delete(key);
                    });
                }
            }
        } catch (Exception e) {
            String errorStr = e.toString();
            if (errorStr.contains("RedisConnectionFailureException") || errorStr.contains("RedisConnection")) {
                log.warn("Redis is offline. Skipping click synchronization from cache (application is falling back to MySQL).");
            } else {
                log.error("Error occurred while syncing clicks from Redis to MySQL: ", e);
            }
        }
    }
}
