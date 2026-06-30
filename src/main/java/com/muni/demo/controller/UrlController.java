package com.muni.demo.controller;

import com.muni.demo.dto.ApiResponse;
import com.muni.demo.dto.UrlAnalyticsResponse;
import com.muni.demo.dto.UrlRequest;
import com.muni.demo.dto.UrlResponse;
import com.muni.demo.service.UrlShortenerService;
import com.muni.demo.util.QrCodeGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for short URL operations (Shortening, Analytics, Deleting, Listing).
 */
@RestController
@RequestMapping("/api/v1/urls")
@Tag(name = "URL Shortener API", description = "Endpoints for managing and generating short URLs")
public class UrlController {

    private final UrlShortenerService urlShortenerService;

    public UrlController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @PostMapping
    @Operation(summary = "Shorten a URL", description = "Creates a Base62 short URL mapping. Optionally accepts a custom alias and an expiration date.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "URL shortened successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid URL format or request parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Custom alias is already in use")
    })
    public ResponseEntity<ApiResponse<UrlResponse>> shortenUrl(
            @Valid @RequestBody UrlRequest request,
            HttpServletRequest httpServletRequest) {
        String baseUrl = getBaseUrl(httpServletRequest);
        UrlResponse response = urlShortenerService.shortenUrl(request, baseUrl);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("URL shortened successfully", response));
    }

    @GetMapping("/{shortCode}/analytics")
    @Operation(summary = "Get URL Analytics", description = "Retrieves click count, creation date, and last accessed time for a shortened URL.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Analytics fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found or expired")
    })
    public ResponseEntity<ApiResponse<UrlAnalyticsResponse>> getAnalytics(
            @PathVariable @Parameter(description = "The short code or alias") String shortCode) {
        UrlAnalyticsResponse response = urlShortenerService.getAnalytics(shortCode);
        return ResponseEntity.ok(ApiResponse.success("Analytics retrieved successfully", response));
    }

    @DeleteMapping("/{shortCode}")
    @Operation(summary = "Delete Shortened URL", description = "Deletes the URL mapping from the database and evicts it from the cache.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "URL deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteUrl(
            @PathVariable @Parameter(description = "The short code or alias to delete") String shortCode) {
        urlShortenerService.deleteUrl(shortCode);
        return ResponseEntity.ok(ApiResponse.success("URL deleted successfully"));
    }

    @GetMapping
    @Operation(summary = "List URL Mappings", description = "Retrieves a paginated list of all shortened URLs.")
    public ResponseEntity<ApiResponse<Page<UrlResponse>>> listUrls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpServletRequest) {
        String baseUrl = getBaseUrl(httpServletRequest);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UrlResponse> response = urlShortenerService.getAllUrls(pageable, baseUrl);
        return ResponseEntity.ok(ApiResponse.success("URL list retrieved successfully", response));
    }

    @GetMapping(value = "/{shortCode}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Get URL QR Code", description = "Generates and returns a QR Code image as raw PNG bytes that links directly to the shortened URL.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "QR Code image generated successfully",
                content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found or expired")
    })
    public ResponseEntity<byte[]> getQrCode(
            @PathVariable @Parameter(description = "The short code or alias") String shortCode,
            HttpServletRequest httpServletRequest) {
        // Validate code exists
        urlShortenerService.getAnalytics(shortCode);

        String shortUrl = getBaseUrl(httpServletRequest) + "/" + shortCode;
        try {
            byte[] qrCodeBytes = QrCodeGenerator.generateQrCodeImage(shortUrl, 300, 300);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                    .body(qrCodeBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Constructs the base URL from the incoming HTTP request.
     */
    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        if (("http".equals(scheme) && serverPort != 80) || ("https".equals(scheme) && serverPort != 443)) {
            url.append(":").append(serverPort);
        }

        // Handle context path if present (e.g. deployed under /myapp)
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()) {
            url.append(contextPath);
        }

        return url.toString();
    }
}
