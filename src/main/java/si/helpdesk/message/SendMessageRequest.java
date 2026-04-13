package si.helpdesk.message;

import jakarta.validation.constraints.NotBlank;

public class SendMessageRequest {
    @NotBlank
    public String content;
}
