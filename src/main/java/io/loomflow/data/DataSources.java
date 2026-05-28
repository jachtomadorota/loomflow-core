package io.loomflow.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Factory for creating HikariCP connection pools.
 *
 * Explicit, no-magic configuration — pass a HikariConfig or use the builder.
 * No YAML, no properties files, no classpath scanning.
 *
 * Usage:
 * <pre>
 *   DataSource ds = DataSources.create()
 *       .jdbcUrl("jdbc:postgresql://localhost:5432/mydb")
 *       .username("user")
 *       .password("secret")
 *       .maximumPoolSize(10)
 *       .build();
 *
 *   // With H2 for testing
 *   DataSource ds = DataSources.h2InMemory("testdb");
 * </pre>
 */
public final class DataSources {

    private DataSources() {}

    /** Creates a new HikariCP builder. */
    public static Builder create() {
        return new Builder();
    }

    /**
     * Creates a HikariCP DataSource from a pre-built HikariConfig.
     *
     * @param config the HikariCP configuration
     * @return a ready-to-use DataSource
     */
    public static DataSource from(HikariConfig config) {
        return new HikariDataSource(config);
    }

    /**
     * Convenience factory for H2 in-memory database (testing/development).
     *
     * @param dbName the database name (used as H2 catalog name)
     * @return a ready-to-use DataSource
     */
    public static DataSource h2InMemory(String dbName) {
        return create()
                .jdbcUrl("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                .driverClassName("org.h2.Driver")
                .username("sa")
                .password("")
                .maximumPoolSize(5)
                .minimumIdle(1)
                .build();
    }

    public static final class Builder {
        private final HikariConfig config = new HikariConfig();

        private Builder() {
            // Sensible defaults
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30_000);
            config.setIdleTimeout(600_000);
            config.setMaxLifetime(1_800_000);
            config.setAutoCommit(true);
        }

        public Builder jdbcUrl(String url) {
            config.setJdbcUrl(url);
            return this;
        }

        public Builder username(String username) {
            config.setUsername(username);
            return this;
        }

        public Builder password(String password) {
            config.setPassword(password);
            return this;
        }

        public Builder driverClassName(String className) {
            config.setDriverClassName(className);
            return this;
        }

        public Builder maximumPoolSize(int size) {
            config.setMaximumPoolSize(size);
            return this;
        }

        public Builder minimumIdle(int idle) {
            config.setMinimumIdle(idle);
            return this;
        }

        public Builder connectionTimeout(long millis) {
            config.setConnectionTimeout(millis);
            return this;
        }

        public Builder idleTimeout(long millis) {
            config.setIdleTimeout(millis);
            return this;
        }

        public Builder maxLifetime(long millis) {
            config.setMaxLifetime(millis);
            return this;
        }

        public Builder poolName(String name) {
            config.setPoolName(name);
            return this;
        }

        public Builder autoCommit(boolean autoCommit) {
            config.setAutoCommit(autoCommit);
            return this;
        }

        /**
         * Additional HikariCP property (e.g. "dataSource.cachePrepStmts", "true").
         */
        public Builder addDataSourceProperty(String key, Object value) {
            config.addDataSourceProperty(key, value);
            return this;
        }

        public DataSource build() {
            return new HikariDataSource(config);
        }
    }
}
