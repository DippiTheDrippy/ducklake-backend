package se.kth.ducklake.util;

import java.util.regex.Pattern;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import lombok.val;

public class DucklakeSql {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[a-z_][a-z0-9_]{0,62}");

    @ConfigProperty(name = "app.ducklake.catalog.alias", defaultValue = "lake")
    static String defaultCatalogAlias;

    public static final String ROLLBACK_COMMAND = "ROLLBACK;";

    private DucklakeSql() {
    }

    public static void validateIdentifier(String value) {
        if (value == null || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid PostgreSQL identifier: " + value);
        }
    }

    public static String identifier(String value) {
        if (value == null || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid PostgreSQL identifier: " + value);
        }

        return value;
    }

    public static String quote(String value) {
        return "'" + value + "'";
    }

    public static String commit(String author, String commitMessage) {
        return """
                CALL org_lake.set_commit_message(
                        %s,
                        %s,
                );
                """.formatted(
                author,
                commitMessage);
    }

    public static String createTableFromFile(String tableName, String path) {
        return """
                CREATE TABLE %s FROM %s;
                """.formatted(
                identifier(tableName),
                path);
    }

    public static String selectFromTable(String tableName, int limit, int offset) {
        return """
                SELECT * FROM %s LIMIT %d OFFSET %d;
                """.formatted(
                identifier(tableName),
                limit,
                offset);
    }

    public static String insertFileIntoTable(String tableName, String path) {
        return """
                INSERT INTO %s BY NAME SELECT * FROM %s;
                """.formatted(
                identifier(tableName),
                quote(path));
    }

    public static String dropTable(String tableName) {
        return """
                DROP TABLE %s;
                """.formatted(
                identifier(tableName));
    }

    /*
     * VARCHAR column_name,
     * VARCHAR column_type,
     * VARCHAR min,
     * VARCHAR max,
     * INT64 approx_unique,
     * VARCHAR avg,
     * VARCHAR std,
     * VARCHAR q25,
     * VARCHAR q50,
     * VARCHAR q75,
     * INT64 count,
     * DECIMAL null_percentage
     */
    public static String summarize(String tableName) {
        return """
                SUMMARIZE %s;
                """.formatted(
                identifier(tableName));
    }

    // Returns total row_count in given table
    public static String totalRowCount(String tableName) {
        return """
                SELECT COUNT(*) AS row_count FROM %s;
                """.formatted(
                identifier(tableName));
    }

    public static String tableFileSize(String tableName) {
        return """
                SELECT
                    table_name,
                    file_size_bytes
                FROM ducklake_table_info(%s)
                WHERE table_name = %s;
                """.formatted(
                quote(identifier(defaultCatalogAlias)),
                quote(identifier(tableName)));
    }

    public static String transaction(String query) {
        return """
                BEGIN;

                %s

                END;
                """.formatted(query);
    }

}
