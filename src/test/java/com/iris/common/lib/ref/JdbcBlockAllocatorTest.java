package com.iris.common.lib.ref;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JdbcBlockAllocatorTest {
    interface Handler { Object handle(String name, Object[] args) throws Throwable; }

    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> type, Handler h) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (p, m, a) -> h.handle(m.getName(), a));
    }

    /** Hand-rolled fake database: no mocking library needed. */
    static final class Db {
        int connectFailures;
        boolean emptyResult;
        long start = 1_000_000L;
        int connects;
        String sql;
        final List<Object> params = new ArrayList<>();
        final List<Boolean> autoCommit = new ArrayList<>();

        DataSource dataSource() {
            return proxy(DataSource.class, (name, a) -> {
                if (!name.equals("getConnection")) return null;
                connects++;
                if (connectFailures-- > 0) throw new SQLException("db down");
                return connection();
            });
        }

        private Connection connection() {
            return proxy(Connection.class, (name, a) -> switch (name) {
                case "prepareStatement" -> { sql = (String) a[0]; yield statement(); }
                case "setAutoCommit" -> { autoCommit.add((Boolean) a[0]); yield null; }
                default -> null;
            });
        }

        private PreparedStatement statement() {
            return proxy(PreparedStatement.class, (name, a) -> switch (name) {
                case "setObject", "setLong" -> { params.add(a[1]); yield null; }
                case "executeQuery" -> resultSet();
                default -> null;
            });
        }

        private ResultSet resultSet() {
            return proxy(ResultSet.class, (name, a) -> switch (name) {
                case "next" -> !emptyResult;
                case "getLong" -> start;
                default -> null;
            });
        }
    }

    static final LocalDate DAY = LocalDate.of(2026, 11, 23);

    @Test
    void returnsLeasedStartAndBindsParameters() {
        var db = new Db();
        long start = new JdbcBlockAllocator(db.dataSource()).allocate(DAY, 500);
        assertEquals(1_000_000L, start);
        assertTrue(db.sql.contains("ref_counters"));
        assertEquals(List.of(DAY, 500L, 500L), db.params);
        assertEquals(List.of(true), db.autoCommit);
    }

    @Test
    void retriesTransientFailures() {
        var db = new Db();
        db.connectFailures = 2;
        assertEquals(1_000_000L, new JdbcBlockAllocator(db.dataSource()).allocate(DAY, 10));
        assertEquals(3, db.connects);
    }

    @Test
    void givesUpAfterThreeAttempts() {
        var db = new Db();
        db.connectFailures = 10;
        var ex = assertThrows(IllegalStateException.class, () -> new JdbcBlockAllocator(db.dataSource()).allocate(DAY, 10));
        assertEquals(3, db.connects);
        assertTrue(ex.getCause() instanceof SQLException);
    }

    @Test
    void emptyResultIsAnError() {
        var db = new Db();
        db.emptyResult = true;
        assertThrows(IllegalStateException.class, () -> new JdbcBlockAllocator(db.dataSource()).allocate(DAY, 10));
    }
}
