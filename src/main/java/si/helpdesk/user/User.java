package si.helpdesk.user;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq")
    @SequenceGenerator(name = "users_seq", sequenceName = "users_id_seq", allocationSize = 1)
    public Long id;

    @Column(unique = true, nullable = false)
    public String username;

    @Column(name = "password_hash", nullable = false)
    public String passwordHash;

    public static User findByUsername(String username) {
        return find("username", username).firstResult();
    }
}
