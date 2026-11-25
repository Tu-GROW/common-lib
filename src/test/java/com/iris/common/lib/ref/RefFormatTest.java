package com.iris.common.lib.ref;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RefFormatTest {
    static class Plain { @Reference(display = RefDisplay.FORMATTED) String ref; }
    static class Invoice { @Reference(prefix = "INV-", suffix = "-KE", display = RefDisplay.FORMATTED) String ref; }
    static class Raw { @Reference(prefix = "RC-") String ref; }          // display COMPACT

    static RefGenerator generator(boolean checkChar) {
        Clock clock = Clock.fixed(Instant.parse("2026-11-23T06:00:00Z"), ZoneOffset.UTC);
        return new RefGenerator(new InMemoryBlockAllocator(), new byte[32], clock, 100, checkChar, Set.of());
    }

    @Test
    void groupsTenCharacterRefs() {
        assertEquals("ABQ-P30T-ZZC", RefFormat.display("ABQP30TZZC"));
        assertEquals("ABQ P30T ZZC", RefFormat.display("ABQP30TZZC", " "));
    }

    @Test
    void keepsTheCheckCharacterSeparate() {
        assertEquals("ABQ-P30T-ZZC-7", RefFormat.display("ABQP30TZZC7"));
    }

    @Test
    void leavesNullAndUnexpectedLengthsAlone() {
        assertNull(RefFormat.display(null));
        assertEquals("ABC", RefFormat.display("ABC"));
        assertEquals("ABQP30TZZC77", RefFormat.display("ABQP30TZZC77"));
    }

    @Test
    void displayGroupsOnlyFieldsMarkedFormat() {
        assertEquals("INV-ABQ-P30T-ZZC-KE", RefFormat.display(Invoice.class, "ref", "INV-ABQP30TZZC-KE"));
        assertEquals("ABQ-P30T-ZZC", RefFormat.display(Plain.class, "ref", "ABQP30TZZC"));
        assertEquals("RC-ABQP30TZZC", RefFormat.display(Raw.class, "ref", "RC-ABQP30TZZC"));   // display COMPACT: unchanged
    }

    @Test
    void displayStillGroupsBareRefsThatLackTheDeclaredAffixes() {
        assertEquals("ABQ-P30T-ZZC", RefFormat.display(Invoice.class, "ref", "ABQP30TZZC"));      // no INV-/-KE on the value
        assertEquals("ABQ-P30T-ZZC-7", RefFormat.display(Invoice.class, "ref", "ABQP30TZZC7"));
        assertEquals("INV-ABQ-P30T-ZZC", RefFormat.display(Invoice.class, "ref", "INV-ABQP30TZZC"));   // suffix missing
        assertEquals("ABQ-P30T-ZZC-KE", RefFormat.display(Invoice.class, "ref", "ABQP30TZZC-KE"));     // prefix missing
    }

    @Test
    void displayLeavesNullAndUnrecognisedValuesAlone() {
        assertNull(RefFormat.display(Invoice.class, "ref", null));
        assertEquals("ABQP30", RefFormat.display(Invoice.class, "ref", "ABQP30"));
        assertEquals("INV-ABQP30-KE", RefFormat.display(Invoice.class, "ref", "INV-ABQP30-KE"));   // affixes present, ref wrong length
        assertEquals("ABQP30TZZC", RefFormat.display(Raw.class, "ref", "ABQP30TZZC"));   // display COMPACT: never grouped
    }

    @Test
    void toStoredAlwaysReturnsTheCompactForm() {
        try (var g = generator(true)) {
            for (int i = 0; i < 50; i++) {
                String ref = g.next();
                String stored = "INV-" + ref + "-KE";
                String shown = RefFormat.display(Invoice.class, "ref", stored);
                assertEquals(ref, RefFormat.toStored(Plain.class, "ref", RefFormat.display(ref)));
                assertEquals(ref, RefFormat.toStored(Plain.class, "ref", RefFormat.display(ref).toLowerCase()));
                assertEquals(stored, RefFormat.toStored(Invoice.class, "ref", shown));                       // shown -> stored
                assertEquals(stored, RefFormat.toStored(Invoice.class, "ref", shown.toLowerCase()));
                assertEquals(stored, RefFormat.toStored(Invoice.class, "ref", RefFormat.display(ref)));      // affixes omitted
                assertEquals("RC-" + ref, RefFormat.toStored(Raw.class, "ref", RefFormat.display(ref)));     // flag off: still compact
            }
        }
    }

    @Test
    void toStoredToleratesLookalikesAndSeparators() {
        try (var g = generator(false)) {
            String ref = g.next();
            String typed = RefFormat.display(ref, " ").toLowerCase().replace('0', 'o').replace('1', 'l');
            assertEquals(ref, RefFormat.toStored(Plain.class, "ref", typed));
        }
    }

    @Test
    void toStoredRejectsInvalidInput() {
        assertNull(RefFormat.toStored(Plain.class, "ref", null));
        assertNull(RefFormat.toStored(Plain.class, "ref", "ABU-P30T-ZZC"));       // U is not in the alphabet
        assertNull(RefFormat.toStored(Plain.class, "ref", "ABQ-P30T"));           // too short
        assertNull(RefFormat.toStored(Plain.class, "ref", "AZZ-P30T-ZZC"));       // impossible date
        try (var g = generator(true)) {
            String ref = g.next();
            char last = ref.charAt(10);
            char wrong = Base32.A.charAt((Base32.idx(last) + 1) & 31);
            assertNull(RefFormat.toStored(Plain.class, "ref", ref.substring(0, 10) + wrong));   // wrong check character
        }
    }
}