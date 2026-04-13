package si.helpdesk.message;

import java.time.Instant;

public class MessageDTO {
    public Long id;
    public SenderType senderType;
    public Long senderId;
    public String content;
    public Instant sentAt;

    public static MessageDTO from(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.id = message.id;
        dto.senderType = message.senderType;
        dto.senderId = message.senderId;
        dto.content = message.content;
        dto.sentAt = message.sentAt;
        return dto;
    }
}
