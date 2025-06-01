package personal.social.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import personal.social.model.MessageReaction;
import personal.social.model.MessageReadStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Repository interface for managing message read statuses
 * Provides methods to find read statuses by message ID and user ID
 */
@Repository
public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, Long> {
    @Query("SELECT mrs FROM MessageReadStatus mrs WHERE mrs.message.id = :messageId AND mrs.user.id = :userId")
    MessageReadStatus findByMessageIdAndUserId(
            @Param("messageId") Long messageIds,
            @Param("userId") Long userId);

    /**
     * Bulk upsert to mark messages as read using is_read boolean field.
     */
    @Modifying
    @Query(value = """
        UPDATE message_read_status 
        SET is_read = true 
        WHERE message_id IN :messageIds 
        AND user_id = :userId 
        AND is_read = false
        """, nativeQuery = true)
    void bulkUpsertReadStatus(
            @Param("messageIds") Long[] messageIds,
            @Param("userId") Long userId
    );

    // In MessageReadStatusRepository
    @Query(value = """
            SELECT mrs.message_id 
            FROM message_read_status mrs 
            WHERE mrs.message_id IN :messageIds 
            AND mrs.user_id = :userId 
            AND mrs.read_at IS NOT NULL
            """, nativeQuery = true)
    Set<Long> findReadMessageIdsByUserAndMessages(@Param("messageIds") List<Long> messageIds, @Param("userId") Long userId);
}