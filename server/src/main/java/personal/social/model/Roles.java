package personal.social.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import personal.social.enums.RoleEnum;

/**
 * Represents a role assigned to a user in the system.
 *
 * <p>This entity maps to a single record in the roles table,
 * where each record defines a specific {@link RoleEnum} value (e.g., ADMIN, USER, MODERATOR).</p>
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code roleId} - The unique identifier for this role entry.</li>
 *   <li>{@code role} - The enumerated type of role defined by {@link RoleEnum}.</li>
 * </ul>
 * </p>
 *
 * <p>This class is typically used in user-role mapping to enforce access control or permissions in the application.</p>
 *
 * @author
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Roles {
    /**
     * The unique identifier for this role.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The role type (e.g., ADMIN, USER) defined in {@link RoleEnum}.
     */
    @Enumerated(EnumType.STRING)
    private RoleEnum role;

    /**
     * Constructs a Roles object with the specified role.
     *
     * @param role the role to be assigned
     */
    public Roles(RoleEnum role) {
        this.role = role;
    }
}