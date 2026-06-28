package se.kth.common.exceptions;

public class DatasetInsertionException extends RuntimeException {

    public DatasetInsertionException(String message, Exception cause) {
        super(message, cause);
    }

}
