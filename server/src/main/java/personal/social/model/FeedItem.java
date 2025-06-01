package personal.social.model;

import jakarta.persistence.*;
import lombok.*;
import personal.social.enums.ObjectType;
import personal.social.enums.Status;

import java.time.LocalDateTime;

/**
 * Represents an item in the social feed system.
 * A feed item can be a {@code POST}, {@code COMMENT}, or {@code REPLY}.
 *
 * <p>This entity captures hierarchical relationships between feed items, enabling
 * threaded discussions and post-comment-reply structures.</p>
 *
 * <p>Key attributes include:
 * <ul>
 *   <li>{@code itemType}: Defines whether the item is a post, comment, or reply.</li>
 *   <li>{@code parentFItem}: Points to the immediate parent (e.g., a comment or another reply).</li>
 *   <li>{@code postId}: Associates the item with its root post (for grouping).</li>
 *   <li>{@code replyTo}: Refers to the specific comment/reply this item is replying to.</li>
 * </ul>
 * </p>
 *
 * <p>The entity also tracks creation and update timestamps, status, and the user who created it.</p>
 *
 * @author
 */
@Entity
@Data
@Table(name = "feed_item")
@NoArgsConstructor
@AllArgsConstructor
public class FeedItem {
    /**
     * The unique identifier for the feed item (post, comment, or reply).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_item_id")
    private Long id;

    /**
     * The timestamp when this feed item was created.
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * The timestamp when this feed item was last updated.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * The current status of the feed item (e.g., ACTIVE, DELETED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "f_status")
    private Status fstatus;

    /**
     * The type of item this is: POST, COMMENT, or REPLY.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type")
    private ObjectType itemType;

    /**
     * The user who created this feed item.
     */
    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;

    /**
     * ID of the immediate parent feed item.
     * <ul>
     *   <li>null if this item is a POST</li>
     *   <li>not null if this item is a COMMENT or REPLY</li>
     * </ul>
     */
    @Column(name = "parent_item_id")
    private Long parentFItem;

    /**
     * ID of the root post this item belongs to.
     * Useful for quickly querying all comments and replies under a specific post.
     */
    @Column(name = "post_id")
    private Long postId;

    /**
     * ID of the user being replied to (only applicable for REPLY type).
     */
    @Column(name = "reply_to")
    private Long replyTo;

    public FeedItem(LocalDateTime createdAt, LocalDateTime updatedAt, Status fstatus, ObjectType itemType, Users user, Long parentFItem, Long postId, Long replyTo) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.fstatus = fstatus;
        this.itemType = itemType;
        this.user = user;
        this.parentFItem = parentFItem;
        this.postId = postId;
        this.replyTo = replyTo;
    }
}
