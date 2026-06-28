package se.kth.common.exceptions;

public class DatasetCreationException extends RuntimeException {

    public DatasetCreationException(String message, Exception cause) {
        super(message, cause);
    }

}
