package si.helpdesk.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import si.helpdesk.room.Room;

public class CreateConversationRequest {
    @NotNull
    public Room room;

    @NotBlank
    public String message;
}
