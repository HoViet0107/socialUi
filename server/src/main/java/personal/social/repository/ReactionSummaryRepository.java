package personal.social.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import personal.social.model.ReactionSummary;

/**
 * Repository interface for managing reaction summaries
 * Provides methods to toggle reactions and find summaries by feed item
 */
@Repository
public interface ReactionSummaryRepository extends JpaRepository<ReactionSummary, Long> {
    /**
     * Toggles a reaction (like/unlike, dislike/undislike) for a feed item
     *
     * @param userId       The user ID
     * @param objectType   The object type (POST, COMMENT)
     * @param objectId     The object ID
     * @param reactionType The reaction type (LIKE, DISLIKE, etc.)
     */
    @Modifying
    @Transactional
    @Query(value = "CALL ToggleReaction(:p_user_id, :p_object_type, :p_object_id, :p_reaction_type)", nativeQuery = true)
    void toggleReaction(
            @Param("p_user_id") Long userId,
            @Param("p_object_type") String objectType,
            @Param("p_object_id") Long objectId,
            @Param("p_reaction_type") String reactionType
    );

    /**
     * Finds a reaction summary by feed item ID
     *
     * @param feedItemId The feed item ID
     * @return The reaction summary if found
     */
    @Query(value = "SELECT rs FROM ReactionSummary rs WHERE rs.feedItem.id =:feedItemId")
    ReactionSummary findByFeedItemId(Long feedItemId);
}
