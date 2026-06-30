package com.muni.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.muni.demo.dto.UrlAnalyticsResponse;
import com.muni.demo.dto.UrlRequest;
import com.muni.demo.dto.UrlResponse;
import com.muni.demo.exception.GlobalExceptionHandler;
import com.muni.demo.exception.InvalidURLException;
import com.muni.demo.exception.ResourceNotFoundException;
import com.muni.demo.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UrlControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UrlShortenerService urlShortenerService;

    @InjectMocks
    private UrlController urlController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(urlController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testShortenUrlSuccess() throws Exception {
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://spring.io")
                .build();

        UrlResponse response = UrlResponse.builder()
                .originalUrl("https://spring.io")
                .shortCode("xyz123")
                .shortUrl("http://localhost:8080/xyz123")
                .clickCount(0L)
                .createdAt(LocalDateTime.now())
                .build();

        when(urlShortenerService.shortenUrl(any(UrlRequest.class), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("URL shortened successfully"))
                .andExpect(jsonPath("$.data.shortCode").value("xyz123"))
                .andExpect(jsonPath("$.data.shortUrl").value("http://localhost:8080/xyz123"));
    }

    @Test
    void testShortenUrlValidationFailure() throws Exception {
        // Empty URL is blank, triggering validation error
        UrlRequest request = UrlRequest.builder()
                .originalUrl("")
                .build();

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.originalUrl").exists());
    }

    @Test
    void testShortenUrlInvalidFormat() throws Exception {
        UrlRequest request = UrlRequest.builder()
                .originalUrl("invalid_format_url")
                .build();

        when(urlShortenerService.shortenUrl(any(UrlRequest.class), anyString()))
                .thenThrow(new InvalidURLException("The URL format is invalid."));

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("The URL format is invalid."));
    }

    @Test
    void testGetAnalyticsSuccess() throws Exception {
        String shortCode = "analytics";
        UrlAnalyticsResponse analytics = UrlAnalyticsResponse.builder()
                .originalUrl("https://google.com")
                .shortCode(shortCode)
                .clickCount(42L)
                .createdAt(LocalDateTime.now())
                .build();

        when(urlShortenerService.getAnalytics(shortCode)).thenReturn(analytics);

        mockMvc.perform(get("/api/v1/urls/" + shortCode + "/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clickCount").value(42L))
                .andExpect(jsonPath("$.data.originalUrl").value("https://google.com"));
    }

    @Test
    void testGetAnalyticsNotFound() throws Exception {
        String shortCode = "missing";

        when(urlShortenerService.getAnalytics(shortCode))
                .thenThrow(new ResourceNotFoundException("Short URL code 'missing' not found"));

        mockMvc.perform(get("/api/v1/urls/" + shortCode + "/analytics"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Short URL code 'missing' not found"));
    }

    @Test
    void testDeleteUrlSuccess() throws Exception {
        String shortCode = "deleteMe";
        doNothing().when(urlShortenerService).deleteUrl(shortCode);

        mockMvc.perform(delete("/api/v1/urls/" + shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("URL deleted successfully"));
    }
}
