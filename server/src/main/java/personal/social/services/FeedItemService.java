package personal.social.services;

import org.springframework.data.domain.Page;
import personal.social.dto.FeedItemDTO;
import personal.social.dto.FeedItemRequest;
import personal.social.dto.ReactionInfoDTO;
import personal.social.dto.ReactionRequest;
import personal.social.enums.ObjectType;
import personal.social.model.Users;

import java.nio.file.AccessDeniedException;

/**
 * Service interface for managing feed items (posts, comments, replies).
 * Provides methods for creating, retrieving, and modifying feed content.
 */
public interface FeedItemService {
    /**
     * Retrieves a paginated list of feed items based on type and request parameters.
     * 
     * @param pageNumber The page number to retrieve (1-based)
     * @param pageSize Number of items per page
     * @param fItemType Type of feed items to retrieve (POST, COMMENT, REPLY)
     * @param feedItemRequest Additional request parameters (postId, parentItemId)
     * @return Page of FeedItemDTO objects
     * @throws Exception if results are empty or other errors occur
     */
    public Page<FeedItemDTO> getAllFeedItem(Integer pageNumber, Integer pageSize, ObjectType fItemType, FeedItemRequest feedItemRequest) throws Exception;

    /**
     * Retrieves reaction information for a specific feed item.
     * 
     * @param user The user requesting the information
     * @param feedItemId ID of the feed item
     * @return ReactionInfoDTO containing reaction counts and user's reaction
     */
    ReactionInfoDTO getFeedItemReactionInfo(Users user, Long feedItemId);

    /**
     * Creates a new feed item (post, comment, or reply).
     * 
     * @param feedItemDTO The feed item data to create
     * @param user The user creating the feed item
     * @return The created feed item with generated ID and timestamps
     */
    FeedItemDTO createFeedItem(FeedItemDTO feedItemDTO, Users user);

    /**
     * Edits an existing feed item or processes a reaction to it.
     * 
     * @param feedItemDTO The updated feed item data
     * @param reactionRequest Reaction information if user is reacting to the item
     * @param user The user performing the edit or reaction
     * @return Success message
     * @throws AccessDeniedException if user doesn't have permission to edit
     */
    String editFeedItem(FeedItemDTO feedItemDTO, ReactionRequest reactionRequest, Users user) throws AccessDeniedException;
}
