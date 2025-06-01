package personal.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import personal.social.enums.ObjectType;
import personal.social.enums.Status;

import java.time.LocalDateTime;

/**
 * Represents the data transfer object for a feed item, encapsulating the feed item's details.
 * This DTO is used for transmitting feed item information, such as its content, type, status, user ID,
 * and timestamp information (created and updated times).
 *
 * @Data Automatically generates getters, setters, equals, hashCode, and toString methods.
 * @AllArgsConstructor Generates a constructor with all fields.
 * @NoArgsConstructor Generates a default constructor (no arguments).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedItemDTO {
    /**
     * The unique identifier for the feed item.
     */
    private Long feedItemId;

    /**
     * The creation timestamp of the feed item.
     */
    private LocalDateTime createdAt;

    /**
     * The last updated timestamp of the feed item.
     */
    private LocalDateTime updatedAt;

    /**
     * The status of the feed item (e.g., ACTIVE, INACTIVE, etc.).
     */
    private Status fstatus;

    /**
     * The type of the feed item (e.g., POST, COMMENT).
     */
    private ObjectType itemType;

    /**
     * The content of the feed item (e.g., text associated with the post/comment).
     */
    private String content;

    /**
     * The ID of the user who created the feed item.
     */
    private Long userId;

    /**
     * The ID of the parent feed item if it's a comment (null for posts).
     */
    private Long parentFItem;

    /**
     * The ID of the feed item if it's a comment or reply (null for posts).
     */
    private Long postId;

    /**
     *
     */
    private Long replyTo;
}
