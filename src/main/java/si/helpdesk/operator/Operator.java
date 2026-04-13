package si.helpdesk.operator;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "operators")
public class Operator extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "operators_seq")
    @SequenceGenerator(name = "operators_seq", sequenceName = "operators_id_seq", allocationSize = 1)
    public Long id;

    @Column(unique = true, nullable = false)
    public String username;

    @Column(name = "password_hash", nullable = false)
    public String passwordHash;

    public String email;

    public static Operator findByUsername(String username) {
        return find("username", username).firstResult();
    }
}
