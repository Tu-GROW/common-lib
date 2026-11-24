package com.iris.common.lib.ref;

import com.iris.common.lib.config.RefProperties;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RefPropertiesTest {
    @Test
    void defaults() {
        var p = new RefProperties();
        assertNull(p.getMasterKey());
        assertEquals("jdbc", p.getAllocator());
        assertTrue(p.isInjectJpa());
        assertFalse(p.isCheckChar());
        assertEquals(RefGenerator.DEFAULT_BLOCK_SIZE, p.getBlockSize());
        assertEquals(RefGenerator.DEFAULT_BLOCKLIST, p.getBlocklist());
    }

    @Test
    void settersRoundTrip() {
        var p = new RefProperties();
        p.setMasterKey("k");
        p.setAllocator("jpa");
        p.setInjectJpa(false);
        p.setCheckChar(true);
        p.setBlockSize(42);
        p.setBlocklist(Set.of("AAA"));
        assertEquals("k", p.getMasterKey());
        assertEquals("jpa", p.getAllocator());
        assertFalse(p.isInjectJpa());
        assertTrue(p.isCheckChar());
        assertEquals(42L, p.getBlockSize());
        assertEquals(Set.of("AAA"), p.getBlocklist());
    }
}
