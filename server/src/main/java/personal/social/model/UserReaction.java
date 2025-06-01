package personal.social.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import personal.social.enums.ObjectType;
import personal.social.enums.ReactionType;

import java.time.LocalDateTime;

/**
 * Represents a specific reaction made by a user on a feed item (e.g., post or comment).
 *
 * <p>This entity logs individual user reactions such as LIKE, DISLIKE, REPORT, or SHARE,
 * and associates each reaction with a specific user and a feed item.</p>
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code id} - Unique identifier for the reaction entry.</li>
 *   <li>{@code user} - The user who made the reaction.</li>
 *   <li>{@code reactionType} - Type of reaction (e.g., LIKE, DISLIKE, REPORT, SHARE) defined in {@link ReactionType}.</li>
 *   <li>{@code objectType} - Type of object reacted to (e.g., POST or COMMENT) defined in {@link ObjectType}.</li>
 *   <li>{@code feedItem} - The feed item (post or comment) the reaction is associated with.</li>
 *   <li>{@code reactedAt} - Timestamp of when the reaction occurred.</li>
 * </ul>
 * </p>
 *
 * <p>This class is typically used to track user engagement and audit reactions across the platform.</p>
 *
 * @author
 */
@Entity
@Table(name = "user_reaction")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserReaction {
    /**
     * Unique identifier for the user reaction.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who made the reaction.
     */
    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;

    /**
     * The type of reaction (LIKE, DISLIKE, REPORT, SHARE).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type")
    private ReactionType reactionType;

    /**
     * The object type (POST or COMMENT) that the reaction is related to.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "object_type")
    private ObjectType objectType;

    /**
     * The feed item (post or comment) that received the reaction.
     */
    @ManyToOne
    @JoinColumn(name = "feed_item_id")
    private FeedItem feedItem;

    /**
     * The timestamp when the user made the reaction.
     */
    @Column(name = "reacted_at")
    private LocalDateTime reactedAt;
}