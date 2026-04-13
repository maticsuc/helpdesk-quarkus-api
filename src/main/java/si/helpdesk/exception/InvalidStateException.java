package si.helpdesk.exception;

public class InvalidStateException extends RuntimeException {
    public final String errorCode;

    public InvalidStateException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
