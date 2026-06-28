package se.kth.common.exceptions;

public class DatasetAlreadyExistsException extends RuntimeException {

    public DatasetAlreadyExistsException(String message, Exception cause) {
        super(message, cause);
    }

}
