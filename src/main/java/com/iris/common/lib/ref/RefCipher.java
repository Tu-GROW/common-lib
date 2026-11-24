package com.iris.common.lib.ref;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDate;

/**
 * Keyed bijection over [0, 2^35): 36-bit balanced Feistel (4 rounds, HMAC-SHA256 round function)
 * with cycle-walking. Bijective, so distinct counters always map to distinct outputs.
 * The key is derived per day from the master key, so rotating the master key never mixes mappings
 * within a day; only roll it at a day boundary.
 */
final class RefCipher {
    static final long DOMAIN = 1L << 35;
    private static final long M18 = (1L << 18) - 1;
    private static final int ROUNDS = 4;

    private final Mac mac;

    RefCipher(byte[] masterKey, LocalDate day) {
        if (masterKey == null || masterKey.length < 32) throw new IllegalArgumentException("master key must be >= 32 bytes");
        try {
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(new SecretKeySpec(masterKey, "HmacSHA256"));
            byte[] dayKey = m.doFinal(("ref-generator/v1/" + day).getBytes(StandardCharsets.UTF_8));
            mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(dayKey, "HmacSHA256"));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    synchronized long permute(long v) {
        if (v < 0 || v >= DOMAIN) throw new IllegalArgumentException("counter out of range: " + v);
        do { v = feistel(v); } while (v >= DOMAIN);   // expected ~2 iterations
        return v;
    }

    private long feistel(long v) {
        long l = v >>> 18, r = v & M18;
        for (int i = 0; i < ROUNDS; i++) {
            long t = r;
            r = l ^ f(i, r);
            l = t;
        }
        return (l << 18) | r;
    }

    private long f(int round, long r) {
        byte[] h = mac.doFinal(new byte[] { (byte) round, (byte) (r >>> 16), (byte) (r >>> 8), (byte) r });
        return (((h[0] & 0xFFL) << 16) | ((h[1] & 0xFFL) << 8) | (h[2] & 0xFFL)) & M18;
    }
}
