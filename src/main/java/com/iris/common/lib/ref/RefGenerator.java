package com.iris.common.lib.ref;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * M-Pesa-style short refs: 10 chars (+1 optional check char), e.g. ABQ4T7KM2X.
 *   [year][month][day][7 chars = keyed permutation of the day's counter]
 * year char = year - 2016 (valid 2016..2047), month/day chars = Crockford index of the number.
 * Uniqueness: (UTC date, counter) is unique via the durable block allocator; the permutation is a bijection.
 */
public final class RefGenerator implements AutoCloseable {
    public static final ZoneId ZONE = ZoneId.of("UTC");
    public static final Set<String> DEFAULT_BLOCKLIST = Set.of("SEX", "ASS", "KKK", "HIV", "TIT");
    public static final long DEFAULT_BLOCK_SIZE = 1_000_000L;

    private static final int YEAR_BASE = 2016;

    private record Block(LocalDate day, long start) {}

    private final BlockAllocator allocator;
    private final byte[] key;
    private final Clock clock;
    private final long blockSize;
    private final boolean checkChar;
    private final Set<String> blocklist;
    private final ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ref-block-prefetch");
        t.setDaemon(true);
        return t;
    });

    private LocalDate day;
    private RefCipher cipher;
    private long cur, end;
    private CompletableFuture<Block> prefetch;

    public RefGenerator(BlockAllocator allocator, byte[] masterKey) {
        this(allocator, masterKey, Clock.system(ZONE), DEFAULT_BLOCK_SIZE, false, DEFAULT_BLOCKLIST);
    }

    public RefGenerator(BlockAllocator allocator, byte[] masterKey, Clock clock,
                               long blockSize, boolean checkChar, Set<String> blocklist) {
        if (blockSize <= 0 || blockSize >= RefCipher.DOMAIN) throw new IllegalArgumentException("blockSize");
        new RefCipher(masterKey, LocalDate.now(clock.withZone(ZONE)));   // fail fast on bad key
        this.allocator = allocator;
        this.key = masterKey.clone();
        this.clock = clock.withZone(ZONE);
        this.blockSize = blockSize;
        this.checkChar = checkChar;
        this.blocklist = blocklist;
    }

    public synchronized String next() {
        for (;;) {
            LocalDate today = LocalDate.now(clock);
            if (!today.equals(day) || cur >= end) {
                refill(today);
            } else if (prefetch == null && end - cur <= blockSize / 5) {
                prefetch = CompletableFuture.supplyAsync(() -> lease(today), exec);
            }
            String ref = format(today, cipher.permute(cur++));
            if (!blocked(ref)) return ref;   // skipped counters are just gaps
        }
    }

    private void refill(LocalDate today) {
        Block b = null;
        if (prefetch != null) {
            try {
                Block p = prefetch.join();
                if (p.day().equals(today)) b = p;   // a block leased for a past day is discarded
            } catch (CompletionException ignored) { /* fall back to a synchronous lease */ }
            prefetch = null;
        }
        if (b == null) b = lease(today);
        if (!today.equals(day)) cipher = new RefCipher(key, today);
        day = today;
        cur = b.start();
        end = b.start() + blockSize;
    }

    private Block lease(LocalDate d) {
        long start = allocator.allocate(d, blockSize);
        if (start < 0 || start + blockSize > RefCipher.DOMAIN)
            throw new IllegalStateException("daily counter space exhausted for " + d);
        return new Block(d, start);
    }

    private String format(LocalDate d, long permuted) {
        int y = d.getYear() - YEAR_BASE;
        if (y < 0 || y > 31) throw new IllegalStateException("year outside supported window 2016..2047");
        StringBuilder sb = new StringBuilder(11)
            .append(Base32.A.charAt(y))
            .append(Base32.A.charAt(d.getMonthValue()))
            .append(Base32.A.charAt(d.getDayOfMonth()));
        for (int i = 6; i >= 0; i--) sb.append(Base32.A.charAt((int) ((permuted >> (5 * i)) & 31)));
        if (checkChar) sb.append(Base32.check(sb));
        return sb.toString();
    }

    private boolean blocked(String ref) {
        for (String w : blocklist) if (ref.contains(w)) return true;
        return false;
    }

    // ---- support-desk helpers (no DB needed) ----

    /** Canonical form of user input (case, O/0, I/L/1, dashes, spaces), or null if it has invalid chars. */
    public static String normalize(String input) { return Base32.normalize(input); }

    /** Structural validity: length, alphabet, plausible date, and check char when used. */
    public static boolean isValid(String input, boolean hasCheckChar) {
        String s = Base32.normalize(input);
        if (s == null || s.length() != (hasCheckChar ? 11 : 10)) return false;
        try { dateOf(s); } catch (DateTimeException e) { return false; }
        return !hasCheckChar || Base32.check(s.substring(0, 10)) == s.charAt(10);
    }

    /** UTC date the ref was issued on; also the partition key for lookups. */
    public static LocalDate dateOf(String normalizedRef) {
        return LocalDate.of(YEAR_BASE + Base32.idx(normalizedRef.charAt(0)),
                            Base32.idx(normalizedRef.charAt(1)),
                            Base32.idx(normalizedRef.charAt(2)));
    }

    @Override
    public void close() { exec.shutdownNow(); }
}
