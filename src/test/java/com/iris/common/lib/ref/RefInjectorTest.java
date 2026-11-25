package com.iris.common.lib.ref;

import com.iris.common.lib.ref.RefInjector;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RefInjectorTest {
    static class Base { @Reference String baseRef; }
    static class Receipt extends Base { @Reference String ref; String other; }
    static class Invoice {
        @Reference(prefix = "INV-", suffix = "-KE") String ref;
        @Reference(prefix = "P-") String other;
    }
    static class Formatted {
        @Reference(prefix = "RC-", display = RefDisplay.FORMATTED) String ref;
        @Reference String plain;
    }
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

    @Test
    void appliesPrefixAndSuffixPerField() {
        AtomicInteger n = new AtomicInteger();
        var inj = new RefInjector(() -> "R" + n.incrementAndGet());
        Invoice inv = inj.inject(new Invoice());
        assertTrue(inv.ref.startsWith("INV-") && inv.ref.endsWith("-KE"), inv.ref);
        assertTrue(inv.other.startsWith("P-") && !inv.other.endsWith("-KE"), inv.other);
        assertNotEquals(inv.ref.substring(4, inv.ref.length() - 3), inv.other.substring(2));
    }

    @Test
    void presetValuesAreNotDecorated() {
        var inj = new RefInjector(() -> "R1");
        Invoice inv = new Invoice();
        inv.ref = "MINE";
        inj.inject(inv);
        assertEquals("MINE", inv.ref);
    }

    @Test
    void nextRefForFieldIsDecorated() {
        var inj = new RefInjector(() -> "R1");
        assertEquals("INV-R1-KE", inj.nextRef(Invoice.class, "ref"));
        assertThrows(IllegalArgumentException.class, () -> inj.nextRef(Invoice.class, "nope"));
    }

    @Test
    void unwrapStripsPrefixAndSuffix() {
        assertEquals("ABQ4T7KM2X", RefInjector.unwrap(Invoice.class, "ref", "inv-ABQ4T7KM2X-ke"));
        assertEquals("ABQ4T7KM2X", RefInjector.unwrap(Invoice.class, "other", " P-ABQ4T7KM2X "));
        assertNull(RefInjector.unwrap(Invoice.class, "ref", "ABQ4T7KM2X"));          // no prefix/suffix
        assertNull(RefInjector.unwrap(Invoice.class, "ref", "INV-"));                // too short
        assertNull(RefInjector.unwrap(Invoice.class, "ref", "INV-ABQ4T7KM2X-XX"));   // wrong suffix
        assertNull(RefInjector.unwrap(Invoice.class, "ref", null));
        assertThrows(IllegalArgumentException.class, () -> RefInjector.unwrap(Invoice.class, "nope", "x"));
    }

    @Test
    void alwaysStoresTheCompactFormEvenWhenDisplayIsFormated() {
        var inj = new RefInjector(() -> "ABQP30TZZC");
        Formatted f = inj.inject(new Formatted());
        assertEquals("RC-ABQP30TZZC", f.ref);
        assertEquals("ABQP30TZZC", f.plain);
        assertEquals("RC-ABQP30TZZC7", new RefInjector(() -> "ABQP30TZZC7").nextRef(Formatted.class, "ref"));
    }

    @Test
    void unwrapReturnsTheCompactRef() {
        assertEquals("ABQP30TZZC", RefInjector.unwrap(Formatted.class, "ref", "rc-ABQP30TZZC"));
    }

    @Test
    void specExposesWhatTheFieldDeclares() {
        assertEquals(new RefInjector.Spec("RC-", "", RefDisplay.FORMATTED), RefInjector.spec(Formatted.class, "ref"));
        assertEquals(new RefInjector.Spec("", "", RefDisplay.COMPACT), RefInjector.spec(Formatted.class, "plain"));
    }
}