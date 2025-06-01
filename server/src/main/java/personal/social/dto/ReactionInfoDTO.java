package personal.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the information about reactions for a particular feed item.
 * This DTO stores counts and status flags for different types of reactions (likes, dislikes, shares, reports) for a feed item.
 *
 * @Data Automatically generates getters, setters, equals, hashCode, and toString methods.
 * @AllArgsConstructor Generates a constructor with all fields.
 * @NoArgsConstructor Generates a default constructor (no arguments).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReactionInfoDTO {
    /**
     * The total number of "likes" reactions on the feed item.
     */
    private long likes;

    /**
     * Flag indicating whether the current user has "liked" the feed item.
     */
    private boolean userLiked;

    /**
     * The total number of "dislikes" reactions on the feed item.
     */
    private long dislikes;

    /**
     * Flag indicating whether the current user has "disliked" the feed item.
     */
    private boolean userDisliked;

    /**
     * The total number of "shares" reactions on the feed item.
     */
    private long shares;

    /**
     * Flag indicating whether the current user has "shared" the feed item.
     */
    private boolean userShared;

    /**
     * The total number of "reports" reactions on the feed item.
     */
    private long reports;

    /**
     * Flag indicating whether the current user has "reported" the feed item.
     */
    private boolean userReported;
}