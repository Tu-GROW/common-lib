package com.iris.common.lib.ref;


import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;

import static org.junit.jupiter.api.Assertions.*;

class RefBeforeConvertCallbackTest {
    static class Aggregate {
        @Id Long id;
        @Reference String ref;
    }

    final RelationalMappingContext context = new RelationalMappingContext();
    final RefBeforeConvertCallback callback = new RefBeforeConvertCallback(new RefInjector(() -> "R1"), () -> context);

    @Test
    void injectsIntoNewAggregates() {
        Aggregate a = new Aggregate();
        assertSame(a, callback.onBeforeConvert(a));
        assertEquals("R1", a.ref);
    }

    @Test
    void leavesExistingAggregatesAlone() {
        Aggregate a = new Aggregate();
        a.id = 5L;
        callback.onBeforeConvert(a);
        assertNull(a.ref);
    }
}
