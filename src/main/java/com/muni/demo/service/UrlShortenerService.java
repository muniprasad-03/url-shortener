package com.muni.demo.service;

import com.muni.demo.dto.UrlAnalyticsResponse;
import com.muni.demo.dto.UrlRequest;
import com.muni.demo.dto.UrlResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for URL shortening operations.
 */
public interface UrlShortenerService {

    /**
     * Shortens a long URL based on the request parameters.
     *
     * @param request The request containing the long URL and optional configurations.
     * @param baseUrl The base URL of the shortening service to construct the short URL.
     * @return The response containing the shortened URL details.
     */
    UrlResponse shortenUrl(UrlRequest request, String baseUrl);

    /**
     * Resolves a short code/alias to its original long URL, tracking analytics.
     *
     * @param shortCode The short code or custom alias.
     * @return The original long URL.
     */
    String getOriginalUrl(String shortCode);

    /**
     * Retrieves analytics information for a given short code.
     *
     * @param shortCode The short code or custom alias.
     * @return The analytics response.
     */
    UrlAnalyticsResponse getAnalytics(String shortCode);

    /**
     * Deletes a shortened URL mapping.
     *
     * @param shortCode The short code or custom alias to delete.
     */
    void deleteUrl(String shortCode);

    /**
     * Retrieves a paginated list of all URL mappings.
     *
     * @param pageable The pagination parameters.
     * @param baseUrl  The base URL of the shortening service.
     * @return A page of shortened URL responses.
     */
    Page<UrlResponse> getAllUrls(Pageable pageable, String baseUrl);
}
