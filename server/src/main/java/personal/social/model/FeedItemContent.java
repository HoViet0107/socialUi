package personal.social.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents the textual content associated with a {@link FeedItem}.
 *
 * <p>This entity separates the content from the core {@code FeedItem} structure
 * to support features like versioning, content auditing, or soft edits in the future.</p>
 *
 * <p>Each {@code FeedItemContent} is linked to one {@code FeedItem}, and stores:
 * <ul>
 *   <li>{@code content}: The actual text/content of the feed item (post, comment, or reply).</li>
 *   <li>{@code updatedAt}: Timestamp of the most recent update to the content.</li>
 * </ul>
 * </p>
 *
 * <p>The {@code fItemId} is the primary key of this entity and is auto-generated.</p>
 *
 * @author
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeedItemContent {
    /**
     * The unique identifier for this content record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The main text content of the feed item (e.g., post, comment, reply).
     * Cannot be null. Stored as a TEXT type in the database to support long inputs.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * The timestamp when this content was last updated.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * The feed item to which this content belongs.
     * This field establishes a many-to-one relationship to the FeedItem entity.
     */
    @ManyToOne
    @JoinColumn(name = "feed_item_id", nullable = false)
    private FeedItem feedItem;

    public FeedItemContent(String content, LocalDateTime updatedAt, FeedItem feedItem) {
        this.content = content;
        this.updatedAt = updatedAt;
        this.feedItem = feedItem;
    }
}