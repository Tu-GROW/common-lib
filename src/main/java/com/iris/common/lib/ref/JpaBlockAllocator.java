package com.iris.common.lib.ref;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

/**
 * Block allocator for Spring Data JPA apps (refgen.allocator=jpa). Uses the app's EntityManagerFactory and
 * JpaTransactionManager, but always in its OWN transaction (REQUIRES_NEW), so the lease commits immediately
 * and independently of the caller's business transaction. If it joined that transaction, a rollback would
 * rewind the counter and the same range could be leased again.
 * No entity or repository is needed: it is a single native upsert.
 */
public final class JpaBlockAllocator implements BlockAllocator {
    private static final String SQL =
        "INSERT INTO ref_counters (day, next_value) VALUES (:day, :size) " +
        "ON CONFLICT (day) DO UPDATE SET next_value = ref_counters.next_value + EXCLUDED.next_value " +
        "RETURNING next_value - :size";
    private static final int ATTEMPTS = 3;

    private final EntityManager em;           // shared proxy: binds to whichever transaction is current
    private final TransactionTemplate tx;

    public JpaBlockAllocator(EntityManagerFactory emf, JpaTransactionManager tm) {
        this.em = SharedEntityManagerCreator.createSharedEntityManager(emf);
        this.tx = new TransactionTemplate(tm);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // package-private: used by the test, which passes a fake EntityManager and transaction manager
    JpaBlockAllocator(EntityManager em, PlatformTransactionManager tm) {
        this.em = em;
        this.tx = new TransactionTemplate(tm);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public long allocate(LocalDate day, long size) {
        RuntimeException last = null;
        for (int i = 1; i <= ATTEMPTS; i++) {
            try {
                Number start = tx.execute(status -> (Number) em.createNativeQuery(SQL)
                        .setParameter("day", day)
                        .setParameter("size", size)
                        .getSingleResult());
                if (start == null) throw new IllegalStateException("allocator returned no row");
                return start.longValue();
            } catch (RuntimeException e) {
                last = e;
                try { Thread.sleep(50L * i); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        throw new IllegalStateException("block allocation failed", last);
    }
}
