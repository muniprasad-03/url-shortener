package com.muni.demo.controller;

import com.muni.demo.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to handle redirection from short code to original URL.
 */
@RestController
@Tag(name = "Redirection API", description = "Redirects short codes to their original long URLs")
public class RedirectController {

    private final UrlShortenerService urlShortenerService;

    public RedirectController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @GetMapping("/{shortCode}")
    @Operation(summary = "Redirect to Original URL", description = "Resolves a short code/alias and performs a 302 redirect to the original URL.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "302", description = "Redirecting to original URL"),
        @ApiResponse(responseCode = "404", description = "Short code not found or expired")
    })
    public ResponseEntity<Void> redirectToOriginal(
            @PathVariable @Parameter(description = "The short code or custom alias to resolve") String shortCode) {
        String originalUrl = urlShortenerService.getOriginalUrl(shortCode);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }
}
