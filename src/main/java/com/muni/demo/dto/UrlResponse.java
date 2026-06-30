package com.muni.demo.dto;

import java.time.LocalDateTime;

/**
 * Response payload returned after a URL is shortened.
 */
public class UrlResponse {
    private String originalUrl;
    private String shortUrl;
    private String shortCode;
    private Long clickCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String qrCodeBase64;

    public UrlResponse() {
    }

    public UrlResponse(String originalUrl, String shortUrl, String shortCode, Long clickCount,
                       LocalDateTime createdAt, LocalDateTime expiresAt, String qrCodeBase64) {
        this.originalUrl = originalUrl;
        this.shortUrl = shortUrl;
        this.shortCode = shortCode;
        this.clickCount = clickCount;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.qrCodeBase64 = qrCodeBase64;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
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

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getQrCodeBase64() {
        return qrCodeBase64;
    }

    public void setQrCodeBase64(String qrCodeBase64) {
        this.qrCodeBase64 = qrCodeBase64;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Custom builder class for UrlResponse.
     */
    public static class Builder {
        private String originalUrl;
        private String shortUrl;
        private String shortCode;
        private Long clickCount;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private String qrCodeBase64;

        public Builder originalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
            return this;
        }

        public Builder shortUrl(String shortUrl) {
            this.shortUrl = shortUrl;
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

        public Builder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder qrCodeBase64(String qrCodeBase64) {
            this.qrCodeBase64 = qrCodeBase64;
            return this;
        }

        public UrlResponse build() {
            return new UrlResponse(originalUrl, shortUrl, shortCode, clickCount, createdAt, expiresAt, qrCodeBase64);
        }
    }
}
