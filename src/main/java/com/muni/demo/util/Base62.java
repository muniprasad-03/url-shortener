package com.muni.demo.util;

import java.security.SecureRandom;

/**
 * Utility class for Base62 encoding and decoding.
 * Also provides a method to generate a secure random Base62 code.
 */
public final class Base62 {

    private static final String BASE62_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = BASE62_CHARACTERS.length();
    private static final SecureRandom RANDOM = new SecureRandom();

    private Base62() {
        // Prevent instantiation
    }

    /**
     * Encodes a base10 long value to a Base62 string.
     *
     * @param value The long value to encode.
     * @return The Base62 encoded string.
     */
    public static String encode(long value) {
        if (value == 0) {
            return String.valueOf(BASE62_CHARACTERS.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        long temp = value;
        while (temp > 0) {
            int remainder = (int) (temp % BASE);
            sb.append(BASE62_CHARACTERS.charAt(remainder));
            temp /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 string back to a base10 long value.
     *
     * @param base62Str The Base62 string to decode.
     * @return The decoded long value.
     */
    public static long decode(String base62Str) {
        long result = 0;
        long multiplier = 1;
        for (int i = base62Str.length() - 1; i >= 0; i--) {
            char c = base62Str.charAt(i);
            int index = BASE62_CHARACTERS.indexOf(c);
            if (index == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            result += index * multiplier;
            multiplier *= BASE;
        }
        return result;
    }

    /**
     * Generates a random Base62 string of the specified length.
     *
     * @param length The length of the code to generate.
     * @return A random Base62 string.
     */
    public static String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62_CHARACTERS.charAt(RANDOM.nextInt(BASE)));
        }
        return sb.toString();
    }
}
