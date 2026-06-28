package se.kth.common.exceptions;

public class DatasetDeletionException extends RuntimeException {

    public DatasetDeletionException(String message, Exception cause) {
        super(message, cause);
    }

}
