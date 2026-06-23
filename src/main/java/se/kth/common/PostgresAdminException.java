package se.kth.common;

import java.sql.SQLException;

public class PostgresAdminException extends RuntimeException {

    public PostgresAdminException(String message, SQLException cause) {
        super(message, cause);
    }

    public PostgresAdminException(String message, Exception cause) {
        super(message, cause);
    }

}
