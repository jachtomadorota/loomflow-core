package io.loomflow.data;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lightweight JDBC helper that eliminates try/catch/finally boilerplate
 * without hiding SQL or adding magic. Explicit, no-reflection, no ORM.
 *
 * All methods acquire a connection from the DataSource (or from active
 * TransactionManager if one exists for this thread/scope), execute the SQL,
 * and return the result. Connections are always properly closed.
 *
 * Usage:
 * <pre>
 *   JdbcTemplate jdbc = new JdbcTemplate(dataSource);
 *
 *   // Query single row
 *   Optional<User> user = jdbc.queryOne(
 *       "SELECT id, name FROM users WHERE id = ?",
 *       rs -> new User(rs.getLong("id"), rs.getString("name")),
 *       userId
 *   );
 *
 *   // Query list
 *   List<User> users = jdbc.queryList(
 *       "SELECT id, name FROM users",
 *       rs -> new User(rs.getLong("id"), rs.getString("name"))
 *   );
 *
 *   // Update / insert / delete
 *   int rows = jdbc.update("INSERT INTO users (name) VALUES (?)", "Alice");
 *
 *   // Insert with generated key
 *   long id = jdbc.insertAndReturnKey("INSERT INTO users (name) VALUES (?)", "Bob");
 * </pre>
 */
public final class JdbcTemplate {

    private final DataSource dataSource;
    private final TransactionManager transactionManager;

    /** Creates a JdbcTemplate without transaction support. */
    public JdbcTemplate(DataSource dataSource) {
        this(dataSource, null);
    }

    /** Creates a JdbcTemplate that participates in transactions managed by the given TransactionManager. */
    public JdbcTemplate(DataSource dataSource, TransactionManager transactionManager) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
    }

    // -------------------------------------------------------------------------
    // Query methods
    // -------------------------------------------------------------------------

    /**
     * Executes a SELECT and maps each row using the given mapper.
     *
     * @param sql    SQL query string
     * @param mapper function mapping a ResultSet row to the target type
     * @param params positional parameters for the PreparedStatement
     * @param <T>    result element type
     * @return list of mapped rows, possibly empty
     * @throws DataAccessException on SQL error
     */
    public <T> List<T> queryList(String sql, RowMapper<T> mapper, Object... params) {
        return execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                setParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    List<T> results = new ArrayList<>();
                    while (rs.next()) {
                        results.add(mapper.map(rs));
                    }
                    return results;
                }
            }
        });
    }

    /**
     * Executes a SELECT and returns the first row mapped by the given mapper,
     * or empty if no row was found.
     *
     * @param sql    SQL query string
     * @param mapper function mapping a ResultSet row to the target type
     * @param params positional parameters
     * @param <T>    result type
     * @return Optional containing the first row, or empty
     * @throws DataAccessException on SQL error or multiple rows returned
     */
    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> results = queryList(sql, mapper, params);
        if (results.isEmpty()) return Optional.empty();
        if (results.size() > 1) {
            throw new DataAccessException("Expected at most 1 row but got " + results.size() + " for: " + sql);
        }
        return Optional.of(results.get(0));
    }

    /**
     * Executes a SELECT and maps the first column of the first row to the expected type.
     * Useful for COUNT(*), MAX(), scalar lookups.
     *
     * @param sql         SQL query string
     * @param returnType  expected Java type of the scalar result
     * @param params      positional parameters
     * @param <T>         return type
     * @return Optional containing the scalar value, or empty if no row
     */
    public <T> Optional<T> queryScalar(String sql, Class<T> returnType, Object... params) {
        return execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                setParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.ofNullable(returnType.cast(rs.getObject(1)));
                    }
                    return Optional.empty();
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Update methods
    // -------------------------------------------------------------------------

    /**
     * Executes an INSERT, UPDATE, or DELETE statement.
     *
     * @param sql    SQL statement
     * @param params positional parameters
     * @return number of affected rows
     * @throws DataAccessException on SQL error
     */
    public int update(String sql, Object... params) {
        return execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                setParams(ps, params);
                return ps.executeUpdate();
            }
        });
    }

    /**
     * Executes an INSERT and returns the generated primary key.
     *
     * @param sql    INSERT statement
     * @param params positional parameters
     * @return the generated key as long
     * @throws DataAccessException on SQL error or no generated key
     */
    public long insertAndReturnKey(String sql, Object... params) {
        return execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setParams(ps, params);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getLong(1);
                    }
                    throw new DataAccessException("No generated key returned for: " + sql);
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Batch operations
    // -------------------------------------------------------------------------

    /**
     * Executes a batch update using the given parameter sets.
     *
     * @param sql        SQL statement
     * @param paramSets  list of parameter arrays, one per batch row
     * @return array of update counts per batch row
     * @throws DataAccessException on SQL error
     */
    public int[] batchUpdate(String sql, List<Object[]> paramSets) {
        return execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Object[] params : paramSets) {
                    setParams(ps, params);
                    ps.addBatch();
                }
                return ps.executeBatch();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Connection management
    // -------------------------------------------------------------------------

    private Connection getConnection() throws SQLException {
        if (transactionManager != null) {
            Connection txConn = transactionManager.currentConnection();
            if (txConn != null) return txConn;
        }
        return dataSource.getConnection();
    }

    private boolean isManagedConnection(Connection conn) {
        if (transactionManager == null) return false;
        return conn == transactionManager.currentConnection();
    }

    private <T> T execute(SqlFunction<T> function) {
        Connection conn = null;
        boolean managed = false;
        try {
            conn = getConnection();
            managed = isManagedConnection(conn);
            return function.apply(conn);
        } catch (SQLException e) {
            throw new DataAccessException("SQL error: " + e.getMessage(), e);
        } finally {
            if (!managed && conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    // -------------------------------------------------------------------------
    // Parameter binding
    // -------------------------------------------------------------------------

    private void setParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    // -------------------------------------------------------------------------
    // Functional interfaces
    // -------------------------------------------------------------------------

    /**
     * Maps a single ResultSet row to a target object.
     * The ResultSet cursor is already positioned on the row — do not call next().
     */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    private interface SqlFunction<T> {
        T apply(Connection conn) throws SQLException;
    }
}
