package si.helpdesk.exception;

import java.time.Instant;

public class ErrorResponse {
    public String error;
    public String message;
    public Instant timestamp;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.timestamp = Instant.now();
    }
}
