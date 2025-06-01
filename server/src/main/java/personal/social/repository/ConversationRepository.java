package personal.social.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import personal.social.model.Conversations;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversations, Long> {
    @Query(value = "SELECT c FROM Conversations c WHERE c.id =:convId")
    Conversations findByConvId(@Param("convId") Long convId);

    @Query(value = """
            SELECT c.id, c.title, c.avatar_url, c.is_group_chat, c.create_at, c.edited_at,
                   m.id, m.message_content, m.send_at, m.message_status, m.sender_id,
                   u.first_name, u.surname, u.last_name, m.is_deleted,
                   (SELECT COALESCE(COUNT(m2.id),0) as unread_count
                    FROM messages m2
                    LEFT JOIN message_read_status mrs ON m2.id = mrs.message_id AND mrs.user_id = :userId
                    WHERE m2.conversation_id = c.id
                      AND m2.sender_id != :userId
                      AND mrs.read_at IS NULL) as unread_count
            FROM conversations c
            JOIN conversation_participants cp ON cp.conversation_id = c.id
            LEFT JOIN messages m ON m.id = c.last_message_id
            LEFT JOIN users u ON m.sender_id = u.id
            WHERE cp.user_id = :userId 
                AND cp.participant_role != 'NOT_PARTICIPANT'
                AND (:cursor IS NULL OR COALESCE(c.edited_at, c.create_at) < :cursor)
                ORDER BY COALESCE(c.edited_at, c.create_at) DESC
            LIMIT :size
            """, nativeQuery = true)
    List<Object[]> findUserConversationsWithCursor(
            @Param("userId") Long userId,
            @Param("cursor") Timestamp cursor,
            @Param("size") int size
    );

    /**
     * Tìm cuộc trò chuyện trực tiếp giữa hai người dùng
     *
     * @param userId1 ID của người dùng thứ nhất
     * @param userId2 ID của người dùng thứ hai
     * @return Danh sách cuộc trò chuyện (thường chỉ có 0 hoặc 1 kết quả)
     */
    @Query("SELECT DISTINCT c FROM Conversations c " +
            "WHERE c.isGroupChat = false " +
            "AND EXISTS (SELECT 1 FROM ConversationParticipants cp1 WHERE cp1.conversation = c AND cp1.user.id = :userId1) " +
            "AND EXISTS (SELECT 1 FROM ConversationParticipants cp2 WHERE cp2.conversation = c AND cp2.user.id = :userId2) " +
            "AND (SELECT COUNT(cp3) FROM ConversationParticipants cp3 WHERE cp3.conversation = c) = 2")
    Optional<Conversations> findOneDirectConversationBetweenUsers(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2);
}
