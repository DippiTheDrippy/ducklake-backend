package se.kth.common.exceptions;

public class DatasetException extends RuntimeException {

    public DatasetException(String message) {
        super(message);
    }

    public DatasetException(String message, Exception cause) {
        super(message, cause);
    }

}
