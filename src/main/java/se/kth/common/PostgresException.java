package se.kth.common;

public class PostgresException extends RuntimeException {

    public PostgresException(String message) {
        super(message);
    }

    public PostgresException(String message, Exception cause) {
        super(message, cause);
    }

}
