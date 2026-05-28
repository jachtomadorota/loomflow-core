package io.loomflow.data;

import javax.sql.DataSource;
import java.lang.ScopedValue;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * Explicit transaction manager — no AOP, no @Transactional, no proxy magic.
 *
 * Virtual-thread safe: uses a ScopedValue to hold the active connection
 * within a transaction scope, ensuring a single connection per logical task.
 *
 * Usage:
 * <pre>
 *   TransactionManager tm = new TransactionManager(dataSource);
 *
 *   // Execute work in a transaction
 *   User user = tm.transaction(() -> {
 *       jdbcTemplate.update("INSERT INTO users (name) VALUES (?)", "Alice");
 *       return jdbcTemplate.queryOne("SELECT * FROM users WHERE name = ?", mapper, "Alice").orElseThrow();
 *   });
 *
 *   // Read-only transaction (sets Connection.setReadOnly(true))
 *   List<User> users = tm.readOnly(() -> jdbcTemplate.queryList("SELECT * FROM users", mapper));
 * </pre>
 *
 * Transactions do not nest by default — if a transaction is already active,
 * the inner call participates in the existing transaction (propagation: REQUIRED).
 */
public final class TransactionManager {

    private static final ScopedValue<Connection> CURRENT_CONNECTION = ScopedValue.newInstance();

    private final DataSource dataSource;

    public TransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Executes the given callable within a read-write transaction.
     * Commits on success, rolls back on exception.
     *
     * If a transaction is already active (nested call), participates in it
     * without opening a new connection.
     *
     * @param action the work to perform inside the transaction
     * @param <T>    the return type
     * @return the result of the action
     * @throws DataAccessException wrapping any SQL or checked exception
     */
    public <T> T transaction(Callable<T> action) {
        return doInTransaction(action, false);
    }

    /**
     * Executes the given callable in a transaction with no return value.
     *
     * @param action the work to perform
     * @throws DataAccessException wrapping any SQL or checked exception
     */
    public void transaction(ThrowingRunnable action) {
        transaction(() -> {
            action.run();
            return null;
        });
    }

    /**
     * Executes the given callable in a read-only transaction.
     * Sets {@link Connection#setReadOnly(true)} — useful for query optimization.
     *
     * @param action the work to perform
     * @param <T>    return type
     * @return the result of the action
     */
    public <T> T readOnly(Callable<T> action) {
        return doInTransaction(action, true);
    }

    /**
     * Returns the active connection for the current scope, or null if no
     * transaction is active. Used by {@link JdbcTemplate} to participate in transactions.
     */
    Connection currentConnection() {
        return CURRENT_CONNECTION.orElse(null);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private <T> T doInTransaction(Callable<T> action, boolean readOnly) {
        // Propagation: REQUIRED — participate in existing transaction
        if (CURRENT_CONNECTION.isBound()) {
            try {
                return action.call();
            } catch (Exception e) {
                throw wrapException(e);
            }
        }

        // Begin new transaction
        Connection conn;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            if (readOnly) conn.setReadOnly(true);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to obtain connection: " + e.getMessage(), e);
        }

        try {
            T result = ScopedValue.where(CURRENT_CONNECTION, conn).call(action::call);
            conn.commit();
            return result;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                // Log but don't swallow the original exception
                rollbackEx.addSuppressed(e);
                throw new DataAccessException("Rollback failed: " + rollbackEx.getMessage(), rollbackEx);
            }
            throw wrapException(e);
        } finally {
            try {
                if (readOnly) conn.setReadOnly(false);
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {}
        }
    }

    private DataAccessException wrapException(Exception e) {
        if (e instanceof DataAccessException dae) return dae;
        if (e instanceof SQLException se) {
            return new DataAccessException("SQL error: " + se.getMessage(), se);
        }
        return new DataAccessException("Transaction failed: " + e.getMessage(), e);
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
