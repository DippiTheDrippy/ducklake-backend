package se.kth.util.sql;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

public class PostgreSql {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[a-z_][a-z0-9_]{0,62}");

    private PostgreSql() {
    }

    public static void validateIdentifier(String value) {
        if (value == null || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid PostgreSQL identifier: " + value);
        }
    }

    public static String identifier(String value) {
        validateIdentifier(value);
        return "\"" + value + "\"";
    }

    public static String stringLiteral(String value) {
        if (value == null) {
            throw new IllegalArgumentException("SQL string literal cannot be null");
        }

        return "'" + value.replace("'", "''") + "'";
    }

    public static String readerGroupRole(String database) {
        return datasetGroupRole("ro", database);
    }

    public static String writerGroupRole(String database) {
        return datasetGroupRole("rw", database);
    }

    private static String datasetGroupRole(String mode, String database) {
        validateIdentifier(database);

        // Create a role name, with UUID and removed dashes
        return "dlg_" + mode + "_" + shortHash(database);
    }

    private static String shortHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}