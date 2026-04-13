package si.helpdesk.message;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import si.helpdesk.conversation.Conversation;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "messages")
public class Message extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "messages_seq")
    @SequenceGenerator(name = "messages_seq", sequenceName = "messages_id_seq", allocationSize = 1)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    public Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    public SenderType senderType;

    @Column(name = "sender_id", nullable = false)
    public Long senderId;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String content;

    @Column(name = "sent_at", nullable = false)
    public Instant sentAt = Instant.now();

    public static List<Message> findByConversation(Long conversationId) {
        return list("conversation.id", conversationId);
    }

    public static List<Message> findByConversationSince(Long conversationId, Long sinceId) {
        return list("conversation.id = ?1 and id > ?2", conversationId, sinceId);
    }

    public static Message findFirstByConversation(Long conversationId) {
        return find("conversation.id = ?1 order by sentAt asc", conversationId).firstResult();
    }
}
