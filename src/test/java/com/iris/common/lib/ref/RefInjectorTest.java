package com.iris.common.lib.ref;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RefInjectorTest {
    static class Base { @Reference String baseRef; }
    static class Receipt extends Base { @Reference String ref; String other; }
    static class Bad { @Reference Long ref; }
    static class Finalised { @Reference final String ref = null; }

    @Test
    void injectsOnlyWhenNull() {
        AtomicInteger n = new AtomicInteger();
        var inj = new RefInjector(() -> "REF" + n.incrementAndGet());

        Receipt fresh = inj.inject(new Receipt());
        assertNotNull(fresh.ref);
        assertNotNull(fresh.baseRef);
        assertNotEquals(fresh.ref, fresh.baseRef);
        assertNull(fresh.other);

        Receipt preset = new Receipt();
        preset.ref = "MINE";
        preset.baseRef = "ALSO";
        int before = n.get();
        inj.inject(preset);
        assertEquals("MINE", preset.ref);
        assertEquals("ALSO", preset.baseRef);
        assertEquals(before, n.get(), "no counter value consumed when already set");
    }

    @Test
    void idempotentAndNullSafe() {
        AtomicInteger n = new AtomicInteger();
        var inj = new RefInjector(() -> "R" + n.incrementAndGet());
        Receipt r = new Receipt();
        inj.inject(r);
        String first = r.ref;
        inj.inject(r);
        assertEquals(first, r.ref);
        assertNull(inj.inject(null));
    }

    @Test
    void rejectsInvalidFields() {
        var inj = new RefInjector(() -> "R");
        assertThrows(IllegalStateException.class, () -> inj.inject(new Bad()));
        assertThrows(IllegalStateException.class, () -> inj.inject(new Finalised()));
    }

    @Test
    void worksWithRealGenerator() {
        try (var g = new RefGenerator(new InMemoryBlockAllocator(), new byte[32])) {
            Receipt r = new RefInjector(g).inject(new Receipt());
            assertTrue(RefGenerator.isValid(r.ref, false));
        }
    }

    @Test
    void exposesFieldNamesAndNextRef() {
        assertEquals(java.util.Set.of("ref", "baseRef"),
            new java.util.HashSet<>(java.util.Arrays.asList(RefInjector.fieldNames(Receipt.class))));
        assertEquals(0, RefInjector.fieldNames(String.class).length);
        assertEquals("X", new RefInjector(() -> "X").nextRef());
    }
}
