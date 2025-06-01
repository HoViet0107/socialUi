package personal.social.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import personal.social.enums.ReactionType;
import personal.social.model.UserReaction;

/**
 * Repository interface for managing user reactions (likes, dislikes, etc.)
 * Provides methods to find reactions by feed item and reaction type
 */
@Repository
public interface UserReactionRepository extends JpaRepository<UserReaction, Long> {
    /**
     * Finds a user reaction by feed item ID and reaction type
     * 
     * @param feedItemId The ID of the feed item
     * @param reactionType The type of reaction (LIKE, DISLIKE, etc.)
     * @return The user reaction if found
     */
    @Query(value = "SELECT ur FROM UserReaction ur " +
            "WHERE ur.feedItem.id =:feedItemId " +
            "AND ur.reactionType =:reactionType")
    UserReaction findByFeedItemId (@Param("feedItemId") Long feedItemId, @Param("reactionType") ReactionType reactionType);
}
