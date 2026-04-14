package si.helpdesk.conversation;

public enum ConversationStatus {
    PENDING, ACTIVE, CLOSED;

    public static ConversationStatus fromString(String value) {
        return valueOf(value.toUpperCase());
    }
}
