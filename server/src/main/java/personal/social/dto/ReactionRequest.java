package personal.social.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import personal.social.enums.ObjectType;
import personal.social.enums.ReactionType;

/**
 * Represents a request to add or modify a reaction on a feed item.
 * This class is used to capture the type of reaction (like, dislike, report, etc.)
 * a user wants to perform on a particular feed item, along with the feed item ID and the type of the feed item.
 *
 * @Data Automatically generates getters, setters, equals, hashCode, and toString methods.
 * @AllArgsConstructor Generates a constructor with all fields.
 * @NoArgsConstructor Generates a default constructor (no arguments).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactionRequest {
    /**
     * The type of the object (feed item) being reacted to (e.g., POST, COMMENT).
     */
    private ObjectType itemType;

    /**
     * The type of reaction being performed (e.g., LIKE, DISLIKE, REPORT, etc.).
     */
    private ReactionType reactionType;

    /**
     * The ID of the feed item being reacted to.
     */
    private Long itemId;
}

