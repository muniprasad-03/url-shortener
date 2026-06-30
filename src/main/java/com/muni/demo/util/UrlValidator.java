package com.muni.demo.util;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utility class to validate original URLs.
 */
public final class UrlValidator {

    private UrlValidator() {
        // Prevent instantiation
    }

    /**
     * Checks if a URL string is valid.
     * Must have http/https protocol and a valid host.
     *
     * @param url The URL string to validate.
     * @return True if valid, false otherwise.
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return false;
            }
            String host = uri.getHost();
            return host != null && !host.trim().isEmpty();
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
