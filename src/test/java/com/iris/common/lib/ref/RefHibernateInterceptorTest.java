package com.iris.common.lib.ref;
import org.hibernate.Interceptor;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RefHibernateInterceptorTest {
    static class Receipt { @Reference String ref; String other; }
    static class Prefixed { @Reference(prefix = "RC-") String ref; }
    static class Plain { String x; }

    final AtomicInteger counter = new AtomicInteger();
    final AtomicInteger resolved = new AtomicInteger();
    final RefInjector injector = new RefInjector(() -> "R" + counter.incrementAndGet());
    final Interceptor ours = new RefHibernateInterceptor(() -> { resolved.incrementAndGet(); return injector; });
    final String[] names = { "other", "ref" };

    @Test
    void fillsNullRefInStateAndReportsChange() {
        Object[] state = { "x", null };
        assertTrue(ours.onSave(new Receipt(), 1L, state, names, null));
        assertEquals("R1", state[1]);
        assertEquals("x", state[0]);
    }

    @Test
    void keepsPresetRefAndUsesNoCounterValue() {
        Object[] state = { "x", "MINE" };
        assertFalse(ours.onSave(new Receipt(), 1L, state, names, null));
        assertEquals("MINE", state[1]);
        assertEquals(0, counter.get());
    }

    @Test
    void otherEntitiesAreANoOpAndNeverResolveTheInjector() {
        assertFalse(ours.onSave(new Plain(), 1L, new Object[] { "x" }, new String[] { "x" }, null));
        assertEquals(0, resolved.get());
    }

    @Test
    void injectorIsResolvedOnlyOnce() {
        ours.onSave(new Receipt(), 1L, new Object[] { "x", null }, names, null);
        ours.onSave(new Receipt(), 2L, new Object[] { "y", null }, names, null);
        assertEquals(1, resolved.get());
        assertEquals(2, counter.get());
    }

    @Test
    void chainRunsUserInterceptorAndOurs() {
        Interceptor user = new Interceptor() {
            @Override
            public boolean onSave(Object e, Object id, Object[] s, String[] n, org.hibernate.type.Type[] t) {
                s[0] = "user";
                return true;
            }
        };
        Object[] state = { "x", null };
        assertTrue(RefHibernateInterceptor.chain(user, ours).onSave(new Receipt(), 1L, state, names, null));
        assertEquals("user", state[0]);
        assertEquals("R1", state[1]);
    }

    @Test
    void chainDelegatesEverythingElseAndUnwrapsExceptions() {
        Interceptor user = new Interceptor() {
            @Override
            public Boolean isTransient(Object entity) {
                if (entity == null) throw new IllegalArgumentException("boom");
                return Boolean.TRUE;
            }
        };
        Interceptor chained = RefHibernateInterceptor.chain(user, ours);
        assertEquals(Boolean.TRUE, chained.isTransient(new Receipt()));
        assertThrows(IllegalArgumentException.class, () -> chained.isTransient(null));
    }

    @Test
    void nullNonRefPropertiesAreNotTouched() {
        Object[] state = { null, "MINE" };            // "other" is null, but it is not a @Reference property
        assertFalse(ours.onSave(new Receipt(), 1L, state, names, null));
        assertNull(state[0]);
        assertEquals("MINE", state[1]);
        assertEquals(0, counter.get());
    }

    @Test
    void appliesThePropertysPrefix() {
        Object[] state = { null };
        assertTrue(ours.onSave(new Prefixed(), 1L, state, new String[] { "ref" }, null));
        assertEquals("RC-R1", state[0]);
    }
}