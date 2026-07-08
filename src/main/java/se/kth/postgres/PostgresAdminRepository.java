package se.kth.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import se.kth.common.exceptions.PostgresAdminException;
import se.kth.postgres.util.PostgreSql;
import se.kth.postgres.util.SqlConnectionWork;

@Slf4j
@ApplicationScoped
public class PostgresAdminRepository {

    @Inject
    AgroalDataSource dataSource;

    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String baseJdbcUrl;

    @ConfigProperty(name = "quarkus.datasource.username")
    String adminUser;

    @ConfigProperty(name = "quarkus.datasource.password")
    String adminPassword;

    public boolean databaseExists(String database) {
        PostgreSql.validateIdentifier(database);

        String sql = "SELECT EXISTS (SELECT 1 FROM pg_database WHERE datname = ?)";

        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, database);
            try (var rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new PostgresAdminException("Failed to check db existence:" + database, e);
        }
    }

    public void executeVoidQuery(String sql) {
        try (Connection connection = dataSource.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();

            try {
                // Do not annotate this service with @Transactional.
                // CREATE DATABASE / DROP DATABASE must be standalone.
                connection.setAutoCommit(true);

                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(sql);
                }
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new PostgresAdminException("Failed to execute PostgreSQL query", e);
        }
    }

    public void createDatabase(String database) {
        PostgreSql.validateIdentifier(database);

        executeVoidQuery("CREATE DATABASE " + PostgreSql.identifier(database));
    }

    public void dropDatabaseIfExist(String database) {
        PostgreSql.validateIdentifier(database);

        executeVoidQuery("DROP DATABASE IF EXISTS " + PostgreSql.identifier(database) + " WITH (FORCE)");
    }

    public void revokePublicAccess(String database) {
        PostgreSql.validateIdentifier(database);

        executeVoidQuery("""
                    REVOKE CONNECT, TEMPORARY
                    ON DATABASE %s
                    FROM PUBLIC
                """.formatted(PostgreSql.identifier(database)));
    }

    public <T> T withDatabaseConnection(String database, SqlConnectionWork<T> work) {
        PostgreSql.validateIdentifier(database);
        String jdbcUrl = jdbcUrlForDatabase(database);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, adminUser, adminPassword)) {
            boolean previousAutoCommit = conn.getAutoCommit();

            try {
                conn.setAutoCommit(true);
                return work.execute(conn);
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new PostgresAdminException("Failed to connect to target database", e);
        }
    }

    public void executeInDatabase(String database, String sql) {
        withDatabaseConnection(database, connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
                return null;
            }
        });
    }

    public String jdbcUrlForDatabase(String database) {
        int index = baseJdbcUrl.lastIndexOf('/');

        if (index == -1) {
            throw new IllegalArgumentException("Invalid JDBC URL: " + baseJdbcUrl);
        }

        return baseJdbcUrl.substring(0, index) + '/' + database;
    }

}
