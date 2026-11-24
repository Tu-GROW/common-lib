package com.iris.common.lib.ref;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/** Postgres-backed allocator. One atomic upsert per block; autocommit so the lease is durable on return. */
public final class JdbcBlockAllocator implements BlockAllocator {
    private static final String SQL =
        "INSERT INTO ref_counters (day, next_value) VALUES (?, ?) " +
        "ON CONFLICT (day) DO UPDATE SET next_value = ref_counters.next_value + EXCLUDED.next_value " +
        "RETURNING next_value - ?";
    private static final int ATTEMPTS = 3;

    private final DataSource ds;

    public JdbcBlockAllocator(DataSource ds) { this.ds = ds; }

    @Override
    public long allocate(LocalDate day, long size) {
        SQLException last = null;
        for (int i = 1; i <= ATTEMPTS; i++) {
            try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(SQL)) {
                c.setAutoCommit(true);
                ps.setObject(1, day);
                ps.setLong(2, size);
                ps.setLong(3, size);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalStateException("allocator returned no row");
                    return rs.getLong(1);
                }
            } catch (SQLException e) {
                last = e;
                try { Thread.sleep(50L * i); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        throw new IllegalStateException("block allocation failed", last);
    }
}
