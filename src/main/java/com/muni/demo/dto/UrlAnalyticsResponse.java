package com.muni.demo.dto;

import java.time.LocalDateTime;

/**
 * Response payload containing analytical information for a shortened URL.
 */
public class UrlAnalyticsResponse {
    private String originalUrl;
    private String shortCode;
    private Long clickCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime expiresAt;

    public UrlAnalyticsResponse() {
    }

    public UrlAnalyticsResponse(String originalUrl, String shortCode, Long clickCount, LocalDateTime createdAt,
                                LocalDateTime updatedAt, LocalDateTime lastAccessedAt, LocalDateTime expiresAt) {
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.clickCount = clickCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastAccessedAt = lastAccessedAt;
        this.expiresAt = expiresAt;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public Long getClickCount() {
        return clickCount;
    }

    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
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
     * Custom builder class for UrlAnalyticsResponse.
     */
    public static class Builder {
        private String originalUrl;
        private String shortCode;
        private Long clickCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime lastAccessedAt;
        private LocalDateTime expiresAt;

        public Builder originalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
            return this;
        }

        public Builder shortCode(String shortCode) {
            this.shortCode = shortCode;
            return this;
        }

        public Builder clickCount(Long clickCount) {
            this.clickCount = clickCount;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder lastAccessedAt(LocalDateTime lastAccessedAt) {
            this.lastAccessedAt = lastAccessedAt;
            return this;
        }

        public Builder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public UrlAnalyticsResponse build() {
            return new UrlAnalyticsResponse(originalUrl, shortCode, clickCount, createdAt, updatedAt, lastAccessedAt, expiresAt);
        }
    }
}
