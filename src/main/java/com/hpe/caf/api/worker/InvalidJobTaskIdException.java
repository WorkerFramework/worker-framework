package com.hpe.caf.api.worker;

/**
 * Indicates that a job task identifier, used in job tracking, has an invalid format.
 */
public class InvalidJobTaskIdException extends WorkerException {
    public InvalidJobTaskIdException(String message) {
        super(message);
    }

    public InvalidJobTaskIdException(String message, Throwable cause) {
        super(message, cause);
    }
}
