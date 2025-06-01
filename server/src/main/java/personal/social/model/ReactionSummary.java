package personal.social.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import personal.social.enums.ObjectType;

/**
 * Represents the aggregate summary of reactions (like, dislike, report, share)
 * for a specific {@link FeedItem} (either a post or a comment).
 *
 * <p>This entity is used to efficiently store and query total reaction counts
 * for each feed item without having to compute them dynamically from individual user reactions.</p>
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code itemType} - The type of the associated item: POST or COMMENT.</li>
 *   <li>{@code feedItem} - The associated {@link FeedItem} (either post or comment) to which the reactions apply.</li>
 *   <li>{@code likes} - Total number of "like" reactions.</li>
 *   <li>{@code dislikes} - Total number of "dislike" reactions.</li>
 *   <li>{@code reports} - Total number of "report" reactions (usually for abuse or inappropriate content).</li>
 *   <li>{@code shares} - Total number of times the content has been shared.</li>
 * </ul>
 * </p>
 *
 * <p>This class is mapped to the database table {@code reaction_aggregate}.</p>
 *
 * @author
 */
@Entity
@Table(name = "reaction_aggregate")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactionSummary {
    /**
     * The unique identifier for this reaction summary record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The type of the object that received reactions.
     * Can be POST, COMMENT, etc., as defined in {@link ObjectType}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type")
    private ObjectType itemType;

    /**
     * The feed item (either a post or a comment) to which these reaction counts are associated.
     * This is a one-to-one mapping and is non-nullable.
     */
    @OneToOne
    @JoinColumn(name = "feed_item_id", referencedColumnName = "feed_item_id", nullable = false)
    private FeedItem feedItem;

    /**
     * The total number of "like" reactions for the associated feed item.
     * Initialized to 0 by default.
     */
    private Long likes = 0L;

    /**
     * The total number of "dislike" reactions for the associated feed item.
     * Initialized to 0 by default.
     */
    private Long dislikes = 0L;

    /**
     * The total number of "report" actions indicating inappropriate or abusive content.
     * Initialized to 0 by default.
     */
    private Long reports = 0L;

    /**
     * The total number of "share" actions for the associated feed item.
     * Initialized to 0 by default.
     */
    private Long shares = 0L;

    public ReactionSummary(ObjectType itemType, FeedItem feedItem, Long likes, Long dislikes, Long reports, Long shares) {
        this.itemType = itemType;
        this.feedItem = feedItem;
        this.likes = likes;
        this.dislikes = dislikes;
        this.reports = reports;
        this.shares = shares;
    }
}