package se.kth.common.exceptions;

public class PostgresException extends RuntimeException {

    public PostgresException(String message) {
        super(message);
    }

    public PostgresException(String message, Exception cause) {
        super(message, cause);
    }

}
