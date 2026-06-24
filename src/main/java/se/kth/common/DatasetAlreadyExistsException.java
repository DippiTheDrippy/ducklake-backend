package se.kth.common;

public class DatasetAlreadyExistsException extends RuntimeException {

    public DatasetAlreadyExistsException(String message, Exception cause) {
        super(message, cause);
    }

}
