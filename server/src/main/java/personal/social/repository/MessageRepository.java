package personal.social.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import personal.social.dto.message.MessageProjection;
import personal.social.model.Messages;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Messages, Long> {
        @Query(value = """
            SELECT m.id, m.conversation_id, m.send_at, m.edited_at, m.message_content, 
                   m.message_status, m.is_deleted, m.reply_to_message_id, m.sender_id,
                   u.first_name, u.surname, u.last_name, u.avatar_url as sender_avatar
            FROM messages m 
            JOIN users u ON m.sender_id = u.id 
            WHERE m.conversation_id = :conversationId 
            AND (:cursor IS NULL OR m.send_at < :cursor)
            AND m.is_deleted = false
            ORDER BY m.send_at DESC 
            LIMIT :size
            """, nativeQuery = true)
    List<MessageProjection> findMessagesWithDetailsForConversation(
            @Param("conversationId") Long conversationId,
            @Param("cursor") Timestamp cursor,
            @Param("size") int size);

    @Query(value = """
            SELECT COUNT(m.id)
            FROM messages m
            JOIN message_read_status mrs ON m.id = mrs.message_id
            WHERE m.conversation_id = :convId
              AND m.sender_id != :userId
              AND mrs.user_id = :userId
              AND mrs.read_at IS NULL
            """, nativeQuery = true)
    int countUnreadMessagesInConversation(
            @Param("convId") Long convId,
            @Param("userId") Long userId
    );

    @Query(value = """
            SELECT m.id, m.send_at, m.edited_at, m.message_content, m.is_deleted,
            m.message_status, m.reply_to_message_id, m.is_read, m.sender_id, 
            m.conversation_id
            FROM messages m
            JOIN users u ON m.sender_id = u.id
            JOIN message_read_status mrs ON m.id = mrs.message_id
            WHERE m.conversation_id = :convId
              AND m.sender_id != :senderId
              AND mrs.user_id = :senderId
              AND mrs.read_at IS NULL
            """, nativeQuery = true)
    List<Messages> findUnreadMessagesInConversation(
            @Param("senderId") Long senderId,
            @Param("convId") Long convId
    );
}
