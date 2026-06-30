package se.kth.ducklake.util;

import java.util.regex.Pattern;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import lombok.val;

public class DucklakeSql {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[a-z_][a-z0-9_]{0,62}");

    @ConfigProperty(name = "app.ducklake.catalog.alias", defaultValue = "lake")
    static String defaultCatalogAlias;

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
        if (value == null) {
            throw new IllegalArgumentException("SQL string literal cannot be null");
        }

        return "'" + value.replace("'", "''") + "'";
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
        String type = determineFileType(path);

        String reader;
        if (type.equals("csv")) {
            reader = "read_csv_auto";
        } else if (type.equals("parquet")) {
            reader = "read_parquet";
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + path);
        }

        return """
                CREATE TABLE %s AS
                SELECT * FROM %s(%s);
                """.formatted(
                identifier(tableName),
                reader,
                quote(path));
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
        String type = determineFileType(path);

        String reader;
        if (type.equals("csv")) {
            reader = "read_csv_auto";
        } else if (type.equals("parquet")) {
            reader = "read_parquet";
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + path);
        }

        return """
                INSERT INTO %s BY NAME SELECT * FROM %s(%s);
                """.formatted(
                identifier(tableName),
                reader,
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

    public static String determineFileType(String path) {
        String lower = path.toLowerCase();

        if (lower.endsWith(".csv")) {
            return "csv";
        } else if (lower.endsWith(".parquet")) {
            return "parquet";
        } else if (lower.endsWith(".json") || lower.endsWith(".ndjson")) {
            return "json";
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + path);
        }
    }

    public static String createTableFromFileJSON(String tableName, String filePath, String rowSourcePath,
            String inferredRowShape) {
        return createOrInsertJSON(tableName, filePath, rowSourcePath, inferredRowShape, true);
    }

    public static String insertFileIntoTableJSON(String tableName, String filePath, String rowSourcePath,
            String inferredRowShape) {
        return createOrInsertJSON(tableName, filePath, rowSourcePath, inferredRowShape, false);
    }

    private static String createOrInsertJSON(String tableName, String filePath, String rowSourcePath,
            String inferredRowShape, boolean create) {
        String str = """
                SELECT
                    unnest(
                        from_json(
                            json_extract(upload.json, %s),
                            %s
                        ),
                        max_depth := 2
                    )
                FROM read_json_objects(
                    %s,
                    format = 'unstructured'
                ) AS upload;
                """.formatted(
                quote(rowSourcePath),
                quote(inferredRowShape),
                quote(filePath));

        if (create) {
            return """
                    CREATE OR REPLACE TABLE %s AS
                    %s
                    """.formatted(identifier(tableName), str);
        } else {
            return """
                    INSERT INTO %s BY NAME
                    %s
                    """.formatted(identifier(tableName), str);
        }
    }

    public static String findBestJsonArraySource(String filePath) {
        return """
                WITH upload AS (
                    SELECT json
                    FROM read_json_objects(
                        %s,
                        format = 'unstructured'
                    )
                )
                SELECT
                    fullkey AS row_source_path,
                    json_array_length(value) AS row_count,
                    json_type(value, '$[0]') AS first_item_type,
                    json_structure(value) AS inferred_row_shape
                FROM upload,
                     json_tree(json)
                WHERE type = 'ARRAY'
                  AND json_type(value, '$[0]') = 'OBJECT'
                  AND fullkey NOT LIKE '%%[%%'
                ORDER BY row_count DESC
                LIMIT 1;
                """.formatted(quote(filePath));
    }
}
