package com.muni.demo.service;

import com.muni.demo.dto.UrlAnalyticsResponse;
import com.muni.demo.dto.UrlRequest;
import com.muni.demo.dto.UrlResponse;
import com.muni.demo.entity.UrlMapping;
import com.muni.demo.exception.DuplicateShortCodeException;
import com.muni.demo.exception.InvalidURLException;
import com.muni.demo.exception.ResourceNotFoundException;
import com.muni.demo.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UrlShortenerServiceTest {

    private UrlShortenerService urlShortenerService;

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        urlShortenerService = new UrlShortenerServiceImpl(urlMappingRepository, redisTemplate);
    }

    @Test
    void testShortenUrlSuccessfully() {
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://github.com/google/gemini-cookbook")
                .build();

        UrlMapping mockSaved = UrlMapping.builder()
                .id(1L)
                .originalUrl(request.getOriginalUrl())
                .shortCode("aBcd12")
                .clickCount(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlMappingRepository.save(any(UrlMapping.class))).thenReturn(mockSaved);

        UrlResponse response = urlShortenerService.shortenUrl(request, "http://localhost:8080");

        assertNotNull(response);
        assertEquals("aBcd12", response.getShortCode());
        assertEquals("http://localhost:8080/aBcd12", response.getShortUrl());
        assertEquals(request.getOriginalUrl(), response.getOriginalUrl());
    }

    @Test
    void testShortenUrlInvalidFormat() {
        UrlRequest request = UrlRequest.builder()
                .originalUrl("not-a-valid-url")
                .build();

        assertThrows(InvalidURLException.class, () -> 
                urlShortenerService.shortenUrl(request, "http://localhost:8080"));
    }

    @Test
    void testShortenUrlCustomAliasAlreadyInUse() {
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://google.com")
                .customAlias("google")
                .build();

        when(urlMappingRepository.existsByShortCode("google")).thenReturn(true);

        assertThrows(DuplicateShortCodeException.class, () ->
                urlShortenerService.shortenUrl(request, "http://localhost:8080"));
    }

    @Test
    void testGetOriginalUrlFromCache() {
        String shortCode = "cached";
        String originalUrl = "https://redis.io";

        when(valueOperations.get("url:original:" + shortCode)).thenReturn(originalUrl);

        String result = urlShortenerService.getOriginalUrl(shortCode);

        assertEquals(originalUrl, result);
        verify(valueOperations).increment("url:clicks:" + shortCode);
    }

    @Test
    void testGetOriginalUrlFromDatabase() {
        String shortCode = "dbcode";
        String originalUrl = "https://mysql.com";

        UrlMapping mapping = UrlMapping.builder()
                .originalUrl(originalUrl)
                .shortCode(shortCode)
                .clickCount(10L)
                .build();

        when(valueOperations.get("url:original:" + shortCode)).thenReturn(null);
        when(urlMappingRepository.findByShortCode(shortCode)).thenReturn(Optional.of(mapping));

        String result = urlShortenerService.getOriginalUrl(shortCode);

        assertEquals(originalUrl, result);
        verify(valueOperations).set("url:original:" + shortCode, originalUrl, java.time.Duration.ofHours(24));
        verify(valueOperations).increment("url:clicks:" + shortCode);
    }

    @Test
    void testGetOriginalUrlNotFound() {
        String shortCode = "absent";

        when(valueOperations.get("url:original:" + shortCode)).thenReturn(null);
        when(urlMappingRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                urlShortenerService.getOriginalUrl(shortCode));
    }

    @Test
    void testGetOriginalUrlExpired() {
        String shortCode = "expired";
        UrlMapping mapping = UrlMapping.builder()
                .originalUrl("https://expired.com")
                .shortCode(shortCode)
                .expiresAt(LocalDateTime.now().minusDays(1)) // Expired 1 day ago
                .build();

        when(valueOperations.get("url:original:" + shortCode)).thenReturn(null);
        when(urlMappingRepository.findByShortCode(shortCode)).thenReturn(Optional.of(mapping));

        assertThrows(ResourceNotFoundException.class, () ->
                urlShortenerService.getOriginalUrl(shortCode));

        verify(urlMappingRepository).delete(mapping);
    }

    @Test
    void testGetAnalyticsSuccess() {
        String shortCode = "analytics";
        UrlMapping mapping = UrlMapping.builder()
                .originalUrl("https://analytics.com")
                .shortCode(shortCode)
                .clickCount(100L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(urlMappingRepository.findByShortCode(shortCode)).thenReturn(Optional.of(mapping));
        when(valueOperations.get("url:clicks:" + shortCode)).thenReturn("5"); // 5 unsynced clicks

        UrlAnalyticsResponse response = urlShortenerService.getAnalytics(shortCode);

        assertNotNull(response);
        assertEquals(105L, response.getClickCount()); // 100 + 5
        assertEquals(mapping.getOriginalUrl(), response.getOriginalUrl());
    }
}
