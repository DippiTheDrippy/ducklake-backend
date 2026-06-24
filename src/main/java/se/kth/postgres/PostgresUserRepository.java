package se.kth.postgres;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import se.kth.common.PostgresAdminException;
import se.kth.postgres.util.PostgreSql;

@Slf4j
@ApplicationScoped
public class PostgresUserRepository {

    @Inject
    AgroalDataSource dataSource;

    @Inject
    PostgresAdminRepository postgresAdminRepository;

    @ConfigProperty(name = "app.ducklake.catalog.host")
    String credentialsHost;

    @ConfigProperty(name = "app.ducklake.catalog.port", defaultValue = "5432")
    int credentialsPort;

    public DbCredentials createReadOnlyUser(String database, OffsetDateTime validUntil) {
        PostgreSql.validateIdentifier(database);

        ensureDatasetAccessRoles(database);

        String username = randomUsername("ro");
        String password = UUID.randomUUID().toString().replace("-", "");

        createloginRole(username, password, validUntil);
        grantMembership(PostgreSql.readerGroupRole(database), username);

        return new DbCredentials(
                username,
                password,
                credentialsHost,
                credentialsPort,
                database,
                "read");
    }

    public DbCredentials createReadWriteUser(String database, OffsetDateTime validUntil) {
        PostgreSql.validateIdentifier(database);

        ensureDatasetAccessRoles(database);

        String username = randomUsername("rw");
        String password = UUID.randomUUID().toString().replace("-", "");

        createloginRole(username, password, validUntil);
        grantMembership(PostgreSql.writerGroupRole(database), username);

        return new DbCredentials(
                username,
                password,
                credentialsHost,
                credentialsPort,
                database,
                "readwrite");
    }

    public void revokeAccess(String username, String readerRole, String writerRole) {
        try {
            executeClusterDdl("""
                    REVOKE %s FROM %s
                    """.formatted(
                    PostgreSql.identifier(readerRole),
                    PostgreSql.temporaryUsername(username)));
        } catch (Exception e) {
            log.warn("Failed to revoke reader role from " + username);
        }

        try {
            executeClusterDdl("""
                    REVOKE %s FROM %s
                    """.formatted(
                    PostgreSql.identifier(writerRole),
                    PostgreSql.temporaryUsername(username)));
        } catch (Exception e) {
            log.warn("Failed to revoke writer role from " + username);
        }
    }

    public void deleteUser(String username, String database) {
        PostgreSql.validateTemporaryUsername(username);
        PostgreSql.validateIdentifier(database);

        String readerRole = PostgreSql.readerGroupRole(database);
        String writerRole = PostgreSql.writerGroupRole(database);

        revokeAccess(username, readerRole, writerRole);

        executeClusterDdl("DROP ROLE IF EXISTS " + PostgreSql.temporaryUsername(username));
    }

    public void dropUsersFromDataset(String database, List<String> users) {
        PostgreSql.validateIdentifier(database);

        String readerRole = PostgreSql.readerGroupRole(database);
        String writerRole = PostgreSql.writerGroupRole(database);

        for (String username : users) {
            revokeAccess(username, readerRole, writerRole);
            executeClusterDdl("DROP ROLE IF EXISTS " + PostgreSql.temporaryUsername(username));
        }

        executeClusterDdl("""
                DROP ROLE IF EXISTS %s
                """.formatted(PostgreSql.identifier(readerRole)));

        executeClusterDdl("""
                DROP ROLE IF EXISTS %s
                """.formatted(PostgreSql.identifier(writerRole)));
    }

    /**
     * Creates or refreshes stable per-dataset reader/writer group roles.
     *
     * Temporary users should not receive grants directly. They inherit from
     * these group roles instead.
     */
    private void ensureDatasetAccessRoles(String database) {
        PostgreSql.validateIdentifier(database);

        if (!postgresAdminRepository.databaseExists(database)) {
            throw new IllegalArgumentException("Dataset database does not exist: " + database);
        }

        String readerRole = PostgreSql.readerGroupRole(database);
        String writerRole = PostgreSql.writerGroupRole(database);

        createGroupRoleIfMissing(readerRole);
        createGroupRoleIfMissing(writerRole);

        grantDatabaseConnect(database, readerRole);
        grantDatabaseConnect(database, writerRole);

        grantPublicSchemaPrivileges(database, readerRole, writerRole);
    }

    private void createGroupRoleIfMissing(String roleName) {
        PostgreSql.validateIdentifier(roleName);

        if (roleExists(roleName)) {
            return;
        }

        executeClusterDdl("""
                CREATE ROLE %s
                NOlogIN
                NOSUPERUSER
                NOCREATEDB
                NOCREATEROLE
                NOREPLICATION
                INHERIT
                """.formatted(PostgreSql.identifier(roleName)));
    }

    private void grantDatabaseConnect(String database, String roleName) {
        executeClusterDdl("""
                GRANT CONNECT
                ON DATABASE %s
                TO %s
                """.formatted(
                PostgreSql.identifier(database),
                PostgreSql.identifier(roleName)));
    }

    /**
     * Grants against public only.
     *
     * This assumes your backend/admin role initializes DuckLake first.
     * If you want external writer credentials to bootstrap a brand-new
     * DuckLake catalog themselves, add CREATE on schema public to writerRole.
     */
    private void grantPublicSchemaPrivileges(String database, String readerRole, String writerRole) {
        String reader = PostgreSql.identifier(readerRole);
        String writer = PostgreSql.identifier(writerRole);

        postgresAdminRepository.withDatabaseConnection(database, conn -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                        GRANT USAGE
                        ON SCHEMA public
                        TO %s
                        """.formatted(reader));

                stmt.execute("""
                        GRANT USAGE
                        ON SCHEMA public
                        TO %s
                        """.formatted(writer));

                stmt.execute("""
                        GRANT SELECT
                        ON ALL TABLES IN SCHEMA public
                        TO %s
                        """.formatted(reader));

                stmt.execute("""
                        GRANT SELECT, INSERT, UPDATE, DELETE
                        ON ALL TABLES IN SCHEMA public
                        TO %s
                        """.formatted(writer));

                stmt.execute("""
                        ALTER DEFAULT PRIVILEGES IN SCHEMA public
                        GRANT SELECT ON TABLES TO %s
                        """.formatted(reader));

                stmt.execute("""
                        ALTER DEFAULT PRIVILEGES IN SCHEMA public
                        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %s
                        """.formatted(writer));

                return null;
            }
        });
    }

    private boolean roleExists(String roleName) {
        PostgreSql.validateIdentifier(roleName);

        String sql = "SELECT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = ?)";

        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roleName);

            try (var rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new PostgresAdminException("Failed to check PostgreSQL role existence: " + roleName, e);
        }
    }

    private void createloginRole(String username, String password, OffsetDateTime validUntil) {
        PostgreSql.validateTemporaryUsername(username);

        executeClusterDdl("""
                CREATE ROLE %s
                logIN
                PASSWORD %s
                VALID UNTIL %s
                NOSUPERUSER
                NOCREATEDB
                NOCREATEROLE
                NOREPLICATION
                INHERIT
                """.formatted(
                PostgreSql.temporaryUsername(username),
                PostgreSql.stringLiteral(password),
                PostgreSql.stringLiteral(validUntil.toString())));
    }

    private void grantMembership(String groupRole, String username) {
        executeClusterDdl("""
                GRANT %s TO %s
                """.formatted(
                PostgreSql.identifier(groupRole),
                PostgreSql.temporaryUsername(username)));
    }

    private void executeClusterDdl(String sql) {
        try (Connection conn = dataSource.getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();

            try {
                conn.setAutoCommit(true);

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                }
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new PostgresAdminException("Failed to execute PostgreSQL role/access DDL", e);
        }
    }

    private String randomUsername(String mode) {
        return "dl_" + mode + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    public record DbCredentials(
            String username,
            String password,
            String credentialsHost,
            int credentialsPort,
            String database,
            String mode) {
    }

}
