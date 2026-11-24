package com.iris.common.lib.ref;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RefCipherTest {
    static final LocalDate DAY = LocalDate.of(2026, 11, 23);
    static final byte[] KEY = new byte[32];
    static final byte[] KEY2 = new byte[32];
    static { KEY2[0] = 1; }

    @Test
    void rejectsMissingOrShortKey() {
        assertThrows(IllegalArgumentException.class, () -> new RefCipher(null, DAY));
        assertThrows(IllegalArgumentException.class, () -> new RefCipher(new byte[31], DAY));
    }

    @Test
    void rejectsOutOfRangeCounters() {
        var c = new RefCipher(KEY, DAY);
        assertThrows(IllegalArgumentException.class, () -> c.permute(-1));
        assertThrows(IllegalArgumentException.class, () -> c.permute(RefCipher.DOMAIN));
    }

    @Test
    void deterministicPerKeyAndDay() {
        assertEquals(new RefCipher(KEY, DAY).permute(42), new RefCipher(KEY, DAY).permute(42));
        assertNotEquals(new RefCipher(KEY, DAY).permute(42), new RefCipher(KEY2, DAY).permute(42));
    }
}
