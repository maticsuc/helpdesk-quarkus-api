package si.helpdesk.conversation;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import si.helpdesk.operator.Operator;
import si.helpdesk.room.Room;
import si.helpdesk.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "conversations")
public class Conversation extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "conversations_seq")
    @SequenceGenerator(name = "conversations_seq", sequenceName = "conversations_id_seq", allocationSize = 1)
    public Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    public Operator operator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ConversationStatus status = ConversationStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();

    public static List<Conversation> findByStatus(ConversationStatus status) {
        return list("status", status);
    }

    public static Optional<Conversation> findByIdAndUser(Long id, Long userId) {
        return find("id = ?1 and user.id = ?2", id, userId).firstResultOptional();
    }
}
