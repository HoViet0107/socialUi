package personal.social.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import personal.social.dto.ReactionInfoDTO;
import personal.social.model.FeedItem;

import java.util.List;

/**
 * Repository interface for managing feed items (posts, comments)
 * Provides methods to retrieve feed items with pagination and reaction information
 */
@Repository
public interface FeedItemRepository extends JpaRepository<FeedItem, Long> {
    /**
     * Retrieves top-level feed items(post) with pagination
     *
     * @param pageNumber The page number
     * @param pageSize   The page size
     * @return List of feed items as Object arrays
     */
    @Query(value = "CALL GetTopLevelFeedItems(:pageNumber, :pageSize)", nativeQuery = true)
    List<Object[]> findAllFeedItems(
            @Param("pageNumber") Integer pageNumber,
            @Param("pageSize") Integer pageSize
    );

    /**
     * Counts all feed items of a specific type
     *
     * @param fItemType The feed item type (POST, COMMENT)
     * @return The total count of feed items
     */
    @Query(value = "CALL GetTopLevelFeedItemsCount(:fItemType)", nativeQuery = true)
    long countAllFeedItems(@Param("fItemType") String fItemType);

    /**
     * Retrieves comments for a specific feed item with pagination
     *
     * @param pageNumber The page number
     * @param pageSize The page size
     * @param parentItemId The parent item ID
     * @return List of comments as Object arrays
     */
    @Query(value = "CALL GetComments(:itemType,:postId, :parentItemId, :pageNumber, :pageSize)", nativeQuery = true)
    List<Object[]> findAllCommentsByParentId(
            @Param("itemType") String itemType,
            @Param("postId") Long postId,
            @Param("parentItemId") Long parentItemId,
            @Param("pageNumber") Integer pageNumber,
            @Param("pageSize") Integer pageSize
    );

    /**
     * Counts comments for a specific feed item(parent feed item)
     *
     * @param parentItemId The parent item ID
     * @return The total count of comments
     */
    @Query(value = "CALL CountParentItemComments(:parentItemId)", nativeQuery = true)
    long countCommentsByParentId(@Param("parentItemId") Long parentItemId);

    /**
     * Retrieves reaction information for a feed item
     *
     * @param feedItemId The feed item ID
     * @return Reaction information DTO
     */
    @Query(value = "CALL GetFeedItemReactionInfo(:userId, :feedItemId)", nativeQuery = true)
    ReactionInfoDTO findFeedItemReactionInfo(
            @Param("userId") Long userId,
            @Param("feedItemId") Long feedItemId
    );

    /**
     * Finds a feed item by its ID
     *
     * @param feedItemId The feed item ID
     * @return The feed item if found
     */
    @Query("SELECT fi FROM FeedItem fi WHERE fi.id =:feedItemId")
    FeedItem findByFeedItemId(Long feedItemId);
}
