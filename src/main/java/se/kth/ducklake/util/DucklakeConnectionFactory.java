package se.kth.ducklake.util;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@ApplicationScoped
public class DucklakeConnectionFactory {

    private static final String GARAGE_SECRET_NAME = "garage_secret";
    private static final String POSTGRES_SECRET_NAME = "postgres_secret";

    @ConfigProperty(name = "app.ducklake.catalog.username")
    String defaultCatalogUsername;

    @ConfigProperty(name = "app.ducklake.catalog.password")
    String defaultCatalogPassword;

    @ConfigProperty(name = "app.ducklake.catalog.host")
    String defaultCatalogHost;

    @ConfigProperty(name = "app.ducklake.catalog.port", defaultValue = "5432")
    int defaultCatalogPort;

    @ConfigProperty(name = "app.ducklake.catalog.alias", defaultValue = "lake")
    String defaultCatalogAlias;

    @ConfigProperty(name = "app.ducklake.garage.region", defaultValue = "garage")
    String defaultGarageRegion;

    @ConfigProperty(name = "app.ducklake.garage.endpoint", defaultValue = "localhost:3900")
    String defaultGarageEndpoint;

    @ConfigProperty(name = "app.ducklake.garage.url-style", defaultValue = "path")
    String defaultGarageUrlStyle;

    @ConfigProperty(name = "app.ducklake.garage.use-ssl", defaultValue = "false")
    boolean defaultGarageUseSsl;

    @ConfigProperty(name = "app.ducklake.local.enabled", defaultValue = "false")
    boolean localModeEnabled;

    @ConfigProperty(name = "app.ducklake.local.path", defaultValue = "ducklake_data/")
    String localDataPath;

    public DucklakeConnectionRequest defaultConnectionRequest(
            String catalogDbName,
            String bucketName,
            String garageKeyId,
            String garageSecret) {
        return new DucklakeConnectionRequest(
                defaultCatalogHost,
                defaultCatalogPort,
                catalogDbName,
                defaultCatalogUsername,
                defaultCatalogPassword,
                bucketName,
                garageKeyId,
                garageSecret,
                defaultGarageRegion,
                defaultGarageEndpoint,
                defaultGarageUrlStyle,
                defaultGarageUseSsl,
                defaultCatalogAlias);
    }

    public Connection openConnection(
            String catalogDbName,
            String bucketName,
            String garageKeyId,
            String garageSecret) throws SQLException {
        return openConnection(new DucklakeConnectionRequest(
                defaultCatalogHost,
                defaultCatalogPort,
                catalogDbName,
                defaultCatalogUsername,
                defaultCatalogPassword,
                bucketName,
                garageKeyId,
                garageSecret,
                defaultGarageRegion,
                defaultGarageEndpoint,
                defaultGarageUrlStyle,
                defaultGarageUseSsl,
                defaultCatalogAlias));
    }

    public Connection openConnection(DucklakeConnectionRequest request) throws SQLException {
        validateRequest(request);

        Connection conn = DriverManager.getConnection("jdbc:duckdb:");

        try {
            initializeConnection(conn, request);
            return conn;
        } catch (SQLException | RuntimeException e) {
            try {
                conn.close();
            } catch (SQLException closeException) {
                e.addSuppressed(closeException);
            }

            throw e;
        }
    }

    private void initializeConnection(Connection conn, DucklakeConnectionRequest request) throws SQLException {
        installAndLoadExtensions(conn);
        createSecrets(conn, request);
        attachDucklake(conn, request);
        useDucklake(conn, request.catalogAlias());
    }

    private void installAndLoadExtensions(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("INSTALL ducklake");
            st.execute("INSTALL postgres");
            st.execute("INSTALL httpfs");

            st.execute("LOAD ducklake");
            st.execute("LOAD postgres");
            st.execute("LOAD httpfs");
        }
    }

    private void createSecrets(Connection conn, DucklakeConnectionRequest request) throws SQLException {
        if (!localModeEnabled) {
            createGarageSecret(conn, request);
        }
        createPostgresSecret(conn, request);
    }

    private void createGarageSecret(Connection conn, DucklakeConnectionRequest request) throws SQLException {
        String sql = """
                CREATE OR REPLACE SECRET %s (
                    TYPE s3,
                    PROVIDER config,
                    KEY_ID %s,
                    SECRET %s,
                    REGION %s,
                    ENDPOINT %s,
                    URL_STYLE %s,
                    USE_SSL %s,
                    SCOPE %s
                )
                """.formatted(
                safeIdentifier(GARAGE_SECRET_NAME),
                sqlStringLiteral(request.garageKeyId()),
                sqlStringLiteral(request.garageSecret()),
                sqlStringLiteral(request.garageRegion()),
                sqlStringLiteral(request.garageEndpoint()),
                sqlStringLiteral(request.garageUrlStyle()),
                request.garageUseSsl(),
                sqlStringLiteral(buildS3DataPath(request.bucketName())));

        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    private void createPostgresSecret(Connection conn, DucklakeConnectionRequest request) throws SQLException {
        String sql = """
                CREATE OR REPLACE SECRET %s (
                    TYPE postgres,
                    HOST %s,
                    PORT %d,
                    DATABASE %s,
                    USER %s,
                    PASSWORD %s
                )
                """.formatted(
                safeIdentifier(POSTGRES_SECRET_NAME),
                sqlStringLiteral(request.catalogHost()),
                request.catalogPort(),
                sqlStringLiteral(request.catalogDbName()),
                sqlStringLiteral(request.catalogUsername()),
                sqlStringLiteral(request.catalogPassword()));

        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    private void attachDucklake(Connection conn, DucklakeConnectionRequest request) throws SQLException {
        String ducklakeUri = "ducklake:postgres:host=%s port=%d dbname=%s user=%s password=%s"
                .formatted(
                        request.catalogHost(),
                        request.catalogPort(),
                        request.catalogDbName(),
                        request.catalogUsername(),
                        request.catalogPassword());

        String dataPath = localModeEnabled
                ? localDataPath
                : buildS3DataPath(request.bucketName());

        String sql = """
                ATTACH %s AS %s
                (
                    DATA_PATH %s
                )
                """.formatted(
                sqlStringLiteral(ducklakeUri),
                safeIdentifier(request.catalogAlias()),
                sqlStringLiteral(dataPath));

        log.debug(
                "Attaching DuckLake alias '{}' using catalog database '{}' and bucket '{}'",
                request.catalogAlias(),
                request.catalogDbName(),
                request.bucketName());

        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    private void useDucklake(Connection conn, String catalogAlias) throws SQLException {
        String sql = "USE " + safeIdentifier(catalogAlias);

        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    /*
     * HELPERS | SAFETY CHECKS
     */

    private static String buildS3DataPath(String bucketName) {
        return "s3://" + bucketName + "/";
    }

    private static void validateRequest(DucklakeConnectionRequest request) {
        requireText(request.catalogHost(), "catalogHost");
        requirePositivePort(request.catalogPort(), "catalogPort");
        requireSafePostgresDatabaseName(request.catalogDbName());
        requireText(request.catalogUsername(), "catalogUsername");
        requireText(request.catalogPassword(), "catalogPassword");

        requireSafeBucketName(request.bucketName());
        requireText(request.garageKeyId(), "garageKeyId");
        requireText(request.garageSecret(), "garageSecret");
        requireText(request.garageRegion(), "garageRegion");
        requireText(request.garageEndpoint(), "garageEndpoint");
        requireText(request.garageUrlStyle(), "garageUrlStyle");

        requireSafeIdentifier(request.catalogAlias(), "catalogAlias");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static void requirePositivePort(int value, String name) {
        if (value <= 0 || value > 65535) {
            throw new IllegalArgumentException(name + " must be a valid TCP port");
        }
    }

    private static void requireSafePostgresDatabaseName(String value) {
        requireText(value, "catalogDbName");

        if (!value.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(
                    "Unsafe Postgres database name: " + value);
        }
    }

    private static void requireSafeBucketName(String value) {
        requireText(value, "bucketName");

        if (!value.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")
                || value.contains("..")
                || value.contains(".-")
                || value.contains("-.")) {
            throw new IllegalArgumentException(
                    "Unsafe bucket name: " + value);
        }
    }

    private static void requireSafeIdentifier(String value, String name) {
        requireText(value, name);

        if (!value.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Unsafe SQL identifier " + name + ": " + value);
        }
    }

    private static String sqlStringLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static String safeIdentifier(String identifier) {
        requireSafeIdentifier(identifier, "identifier");
        return "\"" + identifier + "\"";
    }

    // RECORD FOR CONNECTING TO DB

    public record DucklakeConnectionRequest(
            String catalogHost,
            int catalogPort,
            String catalogDbName,
            String catalogUsername,
            String catalogPassword,
            String bucketName,
            String garageKeyId,
            String garageSecret,
            String garageRegion,
            String garageEndpoint,
            String garageUrlStyle,
            boolean garageUseSsl,
            String catalogAlias) {
    }
}