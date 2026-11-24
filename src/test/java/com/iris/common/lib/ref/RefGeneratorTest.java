package com.iris.common.lib.ref;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RefGeneratorTest {
    static final byte[] KEY = new byte[32];
    static { for (int i = 0; i < 32; i++) KEY[i] = (byte) (i * 7 + 1); }

    static final class MutableClock extends Clock {
        volatile Instant now;
        MutableClock(String iso) { now = Instant.parse(iso); }
        public ZoneId getZone() { return RefGenerator.ZONE; }
        public Clock withZone(ZoneId z) { return this; }
        public Instant instant() { return now; }
    }

    static RefGenerator gen(BlockAllocator a, Clock c, long block, boolean check, Set<String> bl) {
        return new RefGenerator(a, KEY, c, block, check, bl);
    }

    @Test
    void formatAndDatePrefix() {
        var clock = new MutableClock("2026-11-23T06:00:00Z");   // 06:00 UTC
        try (var g = gen(new InMemoryBlockAllocator(), clock, 1000, false, Set.of())) {
            String ref = g.next();
            assertEquals(10, ref.length());
            assertTrue(ref.startsWith("ABQ"), ref);              // A=2026, B=11, Q=23
            assertEquals(LocalDate.of(2026, 11, 23), RefGenerator.dateOf(ref));
            for (char c : ref.toCharArray()) assertTrue(Base32.idx(c) >= 0);
        }
    }

    @Test
    void uniqueAcrossConcurrentNodesSharingAllocator() throws Exception {
        var alloc = new InMemoryBlockAllocator();
        var clock = new MutableClock("2026-11-23T06:00:00Z");
        int nodes = 4, per = 300_000;
        Set<String> seen = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(nodes);
        List<Future<Integer>> fs = new ArrayList<>();
        for (int n = 0; n < nodes; n++) {
            fs.add(pool.submit(() -> {
                int dupes = 0;
                try (var g = gen(alloc, clock, 50_000, false, Set.of())) {
                    for (int i = 0; i < per; i++) if (!seen.add(g.next())) dupes++;
                }
                return dupes;
            }));
        }
        for (var f : fs) assertEquals(0, f.get());
        pool.shutdown();
        assertEquals(nodes * per, seen.size());
    }

    @Test
    void cipherIsInjectiveOnSample() {
        var c = new RefCipher(KEY, LocalDate.of(2026, 11, 23));
        Set<Long> out = new HashSet<>();
        for (long i = 0; i < 1_000_000; i++) {
            long p = c.permute(i);
            assertTrue(p >= 0 && p < RefCipher.DOMAIN);
            assertTrue(out.add(p), "collision at " + i);
        }
    }

    @Test
    void outputLooksNonSequential() {
        var c = new RefCipher(KEY, LocalDate.of(2026, 11, 23));
        long diffs = 0;
        for (long i = 0; i < 1000; i++) if (Math.abs(c.permute(i + 1) - c.permute(i)) > 1_000_000L) diffs++;
        assertTrue(diffs > 900);
    }

    @Test
    void differentDaysUseDifferentMappings() {
        var a = new RefCipher(KEY, LocalDate.of(2026, 11, 23));
        var b = new RefCipher(KEY, LocalDate.of(2026, 11, 24));
        assertNotEquals(a.permute(0), b.permute(0));
    }

    @Test
    void checkCharCatchesTypos() {
        var clock = new MutableClock("2026-11-23T06:00:00Z");
        try (var g = gen(new InMemoryBlockAllocator(), clock, 1000, true, Set.of())) {
            for (int n = 0; n < 200; n++) {
                String ref = g.next();
                assertEquals(11, ref.length());
                assertTrue(RefGenerator.isValid(ref, true));
                assertTrue(RefGenerator.isValid(ref.toLowerCase(), true));
                for (int i = 0; i < ref.length(); i++) {          // every single-char substitution is caught
                    for (char c : Base32.A.toCharArray()) {
                        if (c == ref.charAt(i)) continue;
                        String bad = ref.substring(0, i) + c + ref.substring(i + 1);
                        if (i < 3) continue;                       // date chars may still be a valid-looking date
                        assertFalse(RefGenerator.isValid(bad, true), bad);
                    }
                }
            }
        }
    }

    @Test
    void normalizeHandlesAmbiguousInput() {
        assertEquals("ABQ4T7KM20", RefGenerator.normalize("abq-4t7 km2o"));
        assertEquals("1100", RefGenerator.normalize("IlOo"));
        assertNull(RefGenerator.normalize("ABU"));          // U not in alphabet
    }

    @Test
    void blocklistIsSkipped() {
        var clock = new MutableClock("2026-11-23T06:00:00Z");
        try (var g = gen(new InMemoryBlockAllocator(), clock, 1000, false, Set.of("7"))) {
            for (int i = 0; i < 2000; i++) assertFalse(g.next().contains("7"));
        }
    }

    @Test
    void dayRolloverRestartsWithNewPrefixAndStaysUnique() {
        var clock = new MutableClock("2026-11-23T23:59:59Z");     // 23:59:59 UTC
        var alloc = new InMemoryBlockAllocator();
        Set<String> seen = new HashSet<>();
        try (var g = gen(alloc, clock, 100, false, Set.of())) {
            for (int i = 0; i < 250; i++) assertTrue(seen.add(g.next()));
            clock.now = Instant.parse("2026-11-24T00:00:01Z");    // 00:00:01 UTC, next day
            String first = g.next();
            assertTrue(first.startsWith("ABR"), first);           // R = 24
            for (int i = 0; i < 250; i++) assertTrue(seen.add(g.next()));
        }
    }

    @Test
    void exhaustionFailsLoudly() {
        var clock = new MutableClock("2026-11-23T06:00:00Z");
        var alloc = new InMemoryBlockAllocator();
        alloc.seed(LocalDate.of(2026, 11, 23), RefCipher.DOMAIN - 10);
        try (var g = gen(alloc, clock, 100, false, Set.of())) {
            assertThrows(IllegalStateException.class, g::next);
        }
    }

    @Test
    void prefetchDoesNotCauseOverlap() {
        var clock = new MutableClock("2026-11-23T06:00:00Z");
        Set<String> seen = new HashSet<>();
        try (var g = gen(new InMemoryBlockAllocator(), clock, 1000, false, Set.of())) {
            for (int i = 0; i < 20_000; i++) assertTrue(seen.add(g.next()));
        }
    }

    @Test
    void rejectsBadConfig() {
        var alloc = new InMemoryBlockAllocator();
        var clock = new MutableClock("2026-11-23T06:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> gen(alloc, clock, 0, false, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> gen(alloc, clock, RefCipher.DOMAIN, false, Set.of()));
        assertThrows(IllegalArgumentException.class,
            () -> new RefGenerator(alloc, new byte[16], clock, 100, false, Set.of()));
    }

    @Test
    void yearOutsideWindowFailsLoudly() {
        var clock = new MutableClock("2050-01-01T00:00:00Z");
        try (var g = gen(new InMemoryBlockAllocator(), clock, 100, false, Set.of())) {
            assertThrows(IllegalStateException.class, g::next);
        }
    }

    @Test
    void survivesFailedPrefetch() {
        var calls = new AtomicInteger();
        var inner = new InMemoryBlockAllocator();
        BlockAllocator flaky = (day, size) -> {
            if (calls.incrementAndGet() == 2) throw new IllegalStateException("store down");   // the prefetch
            return inner.allocate(day, size);
        };
        var clock = new MutableClock("2026-11-23T06:00:00Z");
        Set<String> seen = new HashSet<>();
        try (var g = gen(flaky, clock, 10, false, Set.of())) {
            for (int i = 0; i < 60; i++) assertTrue(seen.add(g.next()));
        }
        assertTrue(calls.get() >= 3);
    }

    @Test
    void prefetchedBlockForPastDayIsDiscarded() {
        var clock = new MutableClock("2026-11-23T23:59:59Z");
        Set<String> seen = new HashSet<>();
        try (var g = gen(new InMemoryBlockAllocator(), clock, 10, false, Set.of())) {
            for (int i = 0; i < 9; i++) assertTrue(seen.add(g.next()));   // 9th call triggers the prefetch
            clock.now = Instant.parse("2026-11-24T00:00:01Z");
            String next = g.next();
            assertTrue(next.startsWith("ABR"), next);
            assertTrue(seen.add(next));
        }
    }

    @Test
    void isValidRejectsMalformedInput() {
        assertFalse(RefGenerator.isValid("ABU4T7KM2X", false));    // U is not in the alphabet
        assertFalse(RefGenerator.isValid("ABQ4T7KM2", false));     // too short
        assertFalse(RefGenerator.isValid("ABQ4T7KM2XX", false));   // too long
        assertFalse(RefGenerator.isValid("AZZ4T7KM2X", false));    // month 31: impossible date
        assertFalse(RefGenerator.isValid("ABQ4T7KM2X", true));     // check char missing
        assertTrue(RefGenerator.isValid("abq-4t7 km2x", false));   // case, dash, space tolerated
    }

    @Test
    void isValidRejectsWrongCheckChar() {
        var clock = new MutableClock("2026-11-23T06:00:00Z");
        try (var g = gen(new InMemoryBlockAllocator(), clock, 100, true, Set.of())) {
            String ref = g.next();
            char last = ref.charAt(10);
            char other = Base32.A.charAt((Base32.idx(last) + 1) & 31);
            assertTrue(RefGenerator.isValid(ref, true));
            assertFalse(RefGenerator.isValid(ref.substring(0, 10) + other, true));
        }
    }
}
