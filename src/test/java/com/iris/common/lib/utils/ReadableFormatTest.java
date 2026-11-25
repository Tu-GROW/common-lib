package com.iris.common.lib.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReadableFormatTest {
    @Test
    void explicitGroupsWithLeftoverAsLastGroup() {
        assertEquals("ABQ-P30T-ZZC", ReadableFormat.groups(3, 4).apply("ABQP30TZZC"));   // leftover "ZZC"
        assertEquals("ABQ-P30T-ZZC", ReadableFormat.groups(3, 4, 3).apply("ABQP30TZZC"));
    }

    @Test
    void unusedSizesAreSimplySkippedWhenTheValueRunsOut() {
        assertEquals("ABQ-P30T-ZZC", ReadableFormat.groups(3, 4, 3, 1).apply("ABQP30TZZC"));
        assertEquals("ABQ-P30T-ZZC-7", ReadableFormat.groups(3, 4, 3, 1).apply("ABQP30TZZC7"));
        assertEquals("AB", ReadableFormat.groups(3, 4).apply("AB"));
        assertEquals("", ReadableFormat.groups(3).apply(""));
    }

    @Test
    void uniformGroupsAndRemainder() {
        assertEquals("4111 1111 1111 1111", ReadableFormat.every(4).separator(" ").apply("4111111111111111"));
        assertEquals("123-456-78", ReadableFormat.every(3).apply("12345678"));
    }

    @Test
    void groupingFromTheEnd() {
        assertEquals("1,234,567", ReadableFormat.every(3).separator(",").fromEnd().apply("1234567"));
        assertEquals("123,456", ReadableFormat.every(3).separator(",").fromEnd().apply("123456"));
        assertEquals("71-234-5678", ReadableFormat.groups(4, 3, 3).fromEnd().apply("712345678"));
    }

    @Test
    void multiCharacterSeparatorAndEmptySeparator() {
        assertEquals("ABC / DEF", ReadableFormat.every(3).separator(" / ").apply("ABCDEF"));
        assertEquals("ABCDEF", ReadableFormat.every(3).separator("").apply("ABCDEF"));
    }

    @Test
    void caseConversion() {
        assertEquals("ABQ-P30T-ZZC", ReadableFormat.groups(3, 4, 3).upperCase().apply("abqp30tzzc"));
        assertEquals("abq-p30t-zzc", ReadableFormat.groups(3, 4, 3).lowerCase().apply("ABQP30TZZC"));
        assertEquals("aBq", ReadableFormat.every(3).apply("aBq"));   // untouched by default
    }

    @Test
    void nullPassesThrough() {
        assertNull(ReadableFormat.every(3).apply(null));
        assertNull(ReadableFormat.every(3).strip(null));
    }

    @Test
    void stripReversesApply() {
        var f = ReadableFormat.groups(3, 4, 3).upperCase();
        assertEquals("ABQP30TZZC", f.strip("abq-p30t zzc"));
        assertEquals("ABQP30TZZC", f.strip(f.apply("abqp30tzzc")));
        var thousands = ReadableFormat.every(3).separator(",").fromEnd();
        assertEquals("1234567", thousands.strip("1,234,567"));
        assertEquals("aBq", ReadableFormat.every(3).strip(" aBq "));   // case kept
    }

    @Test
    void instancesAreImmutable() {
        var base = ReadableFormat.every(3);
        var spaced = base.separator(" ");
        assertEquals("ABC-DEF", base.apply("ABCDEF"));
        assertEquals("ABC DEF", spaced.apply("ABCDEF"));
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> ReadableFormat.every(0));
        assertThrows(IllegalArgumentException.class, () -> ReadableFormat.groups(3, -1));
        assertThrows(IllegalArgumentException.class, () -> ReadableFormat.every(3).separator(null));
    }
}
