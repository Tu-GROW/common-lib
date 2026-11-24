package com.iris.common.lib.ref;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;

/** Tests / local dev only: not durable. */
public final class InMemoryBlockAllocator implements BlockAllocator {
    private final ConcurrentHashMap<LocalDate, Long> next = new ConcurrentHashMap<>();

    @Override
    public long allocate(LocalDate day, long size) {
        long[] start = new long[1];
        next.merge(day, size, (old, s) -> { start[0] = old; return old + s; });
        return start[0];
    }

    public void seed(LocalDate day, long nextValue) { next.put(day, nextValue); }
}
