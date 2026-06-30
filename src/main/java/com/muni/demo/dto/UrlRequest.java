package com.muni.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Request payload for creating a shortened URL.
 */
public class UrlRequest {

    @NotBlank(message = "Original URL cannot be blank")
    @Size(max = 2048, message = "Original URL cannot exceed 2048 characters")
    private String originalUrl;

    @Size(min = 3, max = 50, message = "Custom alias must be between 3 and 50 characters")
    private String customAlias;

    private LocalDateTime expiresAt;

    public UrlRequest() {
    }

    public UrlRequest(String originalUrl, String customAlias, LocalDateTime expiresAt) {
        this.originalUrl = originalUrl;
        this.customAlias = customAlias;
        this.expiresAt = expiresAt;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getCustomAlias() {
        return customAlias;
    }

    public void setCustomAlias(String customAlias) {
        this.customAlias = customAlias;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Custom builder class for UrlRequest.
     */
    public static class Builder {
        private String originalUrl;
        private String customAlias;
        private LocalDateTime expiresAt;

        public Builder originalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
            return this;
        }

        public Builder customAlias(String customAlias) {
            this.customAlias = customAlias;
            return this;
        }

        public Builder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public UrlRequest build() {
            return new UrlRequest(originalUrl, customAlias, expiresAt);
        }
    }
}
