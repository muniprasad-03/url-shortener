package com.muni.demo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Base62Test {

    @Test
    void testEncodeDecodeRoundTrip() {
        long originalValue = 123456789L;
        String encoded = Base62.encode(originalValue);
        long decoded = Base62.decode(encoded);

        assertEquals(originalValue, decoded);
    }

    @Test
    void testEncodeZero() {
        assertEquals("0", Base62.encode(0));
    }

    @Test
    void testDecodeZero() {
        assertEquals(0, Base62.decode("0"));
    }

    @Test
    void testInvalidDecodeCharacter() {
        assertThrows(IllegalArgumentException.class, () -> Base62.decode("abc#123"));
    }

    @Test
    void testGenerateRandomCode() {
        String code = Base62.generateRandomCode(6);
        assertNotNull(code);
        assertEquals(6, code.length());

        String longerCode = Base62.generateRandomCode(8);
        assertEquals(8, longerCode.length());
    }
}
