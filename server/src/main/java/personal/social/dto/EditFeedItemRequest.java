package personal.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a request to edit an existing feed item, containing the updated feed item data and reaction information.
 * This class is used when a user wants to modify the details of a feed item (e.g., content, type, status)
 * and optionally update their reaction (like, dislike, share, etc.) associated with the feed item.
 *
 * @Data Automatically generates getters, setters, equals, hashCode, and toString methods.
 * @AllArgsConstructor Generates a constructor with all fields.
 * @NoArgsConstructor Generates a default constructor (no arguments).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditFeedItemRequest {
    /**
     * The FeedItemDTO object containing the updated information for the feed item.
     * It includes details like feed item ID, content, type, status, etc.
     */
    private FeedItemDTO feedItemDTO;

    /**
     * The ReactionRequest object representing the user's reaction update for the feed item.
     * This can be a reaction like "LIKE", "DISLIKE", "REPORT", etc.
     */
    private ReactionRequest reactionRequest;
}
