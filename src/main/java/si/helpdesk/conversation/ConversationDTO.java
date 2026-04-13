package si.helpdesk.conversation;

import si.helpdesk.room.Room;

import java.time.Instant;

public class ConversationDTO {
    public Long id;
    public ConversationStatus status;
    public Room room;
    public UserSummary user;
    public OperatorSummary operator;
    public String firstMessage;
    public Instant createdAt;
    public Instant updatedAt;

    public static class UserSummary {
        public Long id;
        public String username;

        public UserSummary(Long id, String username) {
            this.id = id;
            this.username = username;
        }
    }

    public static class OperatorSummary {
        public Long id;
        public String username;

        public OperatorSummary(Long id, String username) {
            this.id = id;
            this.username = username;
        }
    }

    public static ConversationDTO from(Conversation conv, String firstMessage) {
        ConversationDTO dto = new ConversationDTO();
        dto.id = conv.id;
        dto.status = conv.status;
        dto.room = conv.room;
        dto.user = new UserSummary(conv.user.id, conv.user.username);
        dto.operator = conv.operator != null
                ? new OperatorSummary(conv.operator.id, conv.operator.username)
                : null;
        dto.firstMessage = firstMessage;
        dto.createdAt = conv.createdAt;
        dto.updatedAt = conv.updatedAt;
        return dto;
    }
}
