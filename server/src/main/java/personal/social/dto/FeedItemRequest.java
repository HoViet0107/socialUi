package personal.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
import personal.social.enums.ObjectType;

/**
 * Request DTO used for retrieving feed items based on specific criteria.
 * This class is typically used in POST-based fetch/search/filter APIs when
 * querying feed items by type or their parent item (e.g., fetching comments of a post).
 * <p>
 * Fields:
 * - itemType: Type of the feed item (e.g., POST, COMMENT). Determines the kind of content to fetch.
 * - parentItemId: The ID of the parent feed item (e.g., the post ID when fetching its comments).
 * This is optional depending on the item type.
 * <p>
 * Example use-case:
 * - Fetching all comments for a specific post:
 * {
 * "itemType": "comment",
 * "parentItemId": 123
 * }
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedItemRequest {
    /**
     * Type of the feed item.
     * Possible values: POST, COMMENT, REPLY.
     */
    private String itemType;

    /**
     * ID of the parent feed item.
     * - Null if the item is a post.
     * - Non-null if the item is a comment or reply, representing the parent item ID.
     */
    private Long parentItemId;

    /**
     * ID of the post associated with this feed item.
     */
    private Long postId;

    /**
     * Page number for pagination.
     * Starts from 0 or 1 depending on implementation.
     */
    private int pageNumber;

    /**
     * Number of items per page for pagination.
     */
    private int pageSize;
}
