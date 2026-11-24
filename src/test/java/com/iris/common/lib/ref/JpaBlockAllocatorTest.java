package com.iris.common.lib.ref;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JpaBlockAllocatorTest {
    static final LocalDate DAY = LocalDate.of(2026, 11, 23);

    interface Handler { Object handle(String name, Object[] args) throws Throwable; }

    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> type, Handler h) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (p, m, a) -> h.handle(m.getName(), a));
    }

    static final class Tm implements PlatformTransactionManager {
        final List<Integer> propagation = new ArrayList<>();
        int commits, rollbacks;

        @Override public TransactionStatus getTransaction(TransactionDefinition d) {
            propagation.add(d.getPropagationBehavior());
            return new SimpleTransactionStatus();
        }
        @Override public void commit(TransactionStatus status) { commits++; }
        @Override public void rollback(TransactionStatus status) { rollbacks++; }
    }

    static final class Em {
        int failures;
        Object result = 1_000_000L;
        int queries;
        String sql;
        final Map<String, Object> params = new HashMap<>();

        EntityManager entityManager() {
            return proxy(EntityManager.class, (name, a) -> {
                if (!name.equals("createNativeQuery")) return null;
                sql = (String) a[0];
                queries++;
                Query[] self = new Query[1];
                self[0] = proxy(Query.class, (qn, qa) -> switch (qn) {
                    case "setParameter" -> { params.put((String) qa[0], qa[1]); yield self[0]; }
                    case "getSingleResult" -> {
                        if (failures-- > 0) throw new IllegalStateException("db down");
                        yield result;
                    }
                    default -> null;
                });
                return self[0];
            });
        }
    }

    @Test
    void leasesInItsOwnNewTransaction() {
        var em = new Em();
        var tm = new Tm();
        long start = new JpaBlockAllocator(em.entityManager(), tm).allocate(DAY, 500);
        assertEquals(1_000_000L, start);
        assertTrue(em.sql.contains("ref_counters"));
        assertEquals(DAY, em.params.get("day"));
        assertEquals(500L, em.params.get("size"));
        assertEquals(List.of(TransactionDefinition.PROPAGATION_REQUIRES_NEW), tm.propagation);
        assertEquals(1, tm.commits);
    }

    @Test
    void retriesAndRollsBackFailedAttempts() {
        var em = new Em();
        em.failures = 2;
        var tm = new Tm();
        assertEquals(1_000_000L, new JpaBlockAllocator(em.entityManager(), tm).allocate(DAY, 10));
        assertEquals(2, tm.rollbacks);
        assertEquals(1, tm.commits);
    }

    @Test
    void givesUpAfterThreeAttempts() {
        var em = new Em();
        em.failures = 10;
        var tm = new Tm();
        assertThrows(IllegalStateException.class, () -> new JpaBlockAllocator(em.entityManager(), tm).allocate(DAY, 10));
        assertEquals(3, em.queries);
        assertEquals(3, tm.rollbacks);
    }

    @Test
    void missingRowIsAnError() {
        var em = new Em();
        em.result = null;
        assertThrows(IllegalStateException.class, () -> new JpaBlockAllocator(em.entityManager(), new Tm()).allocate(DAY, 10));
    }

    @Test
    void publicConstructorWrapsTheEntityManagerFactory() {
        EntityManagerFactory emf = proxy(EntityManagerFactory.class, (name, a) -> null);
        assertNotNull(new JpaBlockAllocator(emf, new JpaTransactionManager()));
    }
}
