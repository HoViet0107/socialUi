package personal.social.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a user in the system.
 *
 * <p>This entity holds personal and credential information for users of the application.
 * It also defines the relationship between a user and their assigned role.</p>
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code userId} - Unique identifier for the user.</li>
 *   <li>{@code email} - Unique email address used for login and identification.</li>
 *   <li>{@code password} - Hashed password for authentication (ignored in JSON serialization).</li>
 *   <li>{@code firstName} - User's first name.</li>
 *   <li>{@code surname} - User's middle name or optional second name.</li>
 *   <li>{@code lastName} - User's last name or family name.</li>
 *   <li>{@code phone} - Optional unique phone number (e.g., for contact or 2FA).</li>
 *   <li>{@code dob} - Date of birth of the user.</li>
 *   <li>{@code createdAt} - Timestamp of when the user was created in the system.</li>
 *   <li>{@code roles} - User's assigned role, linked to {@link Roles} entity.</li>
 * </ul>
 * </p>
 *
 * <p>This class is essential for user authentication, authorization, and profile management.</p>
 *
 * @author
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Users {
    /**
     * Unique identifier for the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User's unique email address.
     */
    @Column(columnDefinition = "VARCHAR(155)", unique = true, nullable = false)
    private String email;

    /**
     * User's hashed password. Excluded from JSON serialization for security.
     */
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    /**
     * User's first name.
     */
    @Column(name = "first_name", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String firstName;

    /**
     * User's optional middle name or surname.
     */
    @Column(columnDefinition = "NVARCHAR(255)")
    private String surname;

    /**
     * User's last name.
     */
    @Column(name = "last_name", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String lastName;

    /**
     * User's avatar url.
     */
    @Column(columnDefinition = "TEXT")
    private String avatarUrl;

    /**
     * User's latest active time.
     */
    @Column(name = "last_active")
    private LocalDateTime lastActive;

    /**
     * User's online status.
     */
    @Column(name = "is_online")
    private boolean isOnline;

    /**
     * User's unique phone number. Optional.
     */
    @Column(columnDefinition = "VARCHAR(12)", unique = true)
    private String phone;

    /**
     * User's date of birth.
     */
    @Column(nullable = false)
    private LocalDate dob;

    /**
     * The date and time when the user account was created.
     */
    @Column(name = "created_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    /**
     * The role associated with this user (e.g., ADMIN, USER).
     */
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Roles roles;

    // Thêm method này để tương thích
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}