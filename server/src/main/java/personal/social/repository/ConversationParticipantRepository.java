package personal.social.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import personal.social.model.ConversationParticipants;

import java.util.List;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipants, Long> {
    @Query(value = "SELECT p " +
            "FROM ConversationParticipants p " +
            "WHERE p.user.id = :userId AND p.conversation.id = :convId")
    ConversationParticipants findByUserInConversation(@Param("userId") Long userId, @Param("convId") Long convId);

    @Query(value = """
                SELECT cp.user_id, u.email, u.first_name, u.surname, u.last_name, u.avatar_url, cp.participant_role,
                         cp.joined_at
                FROM conversation_participants cp
                JOIN users u ON cp.user_id = u.id
                WHERE cp.conversation_id =:convId
                AND cp.participant_role != 'NOT_PARTICIPANT'
            """, nativeQuery = true)
    List<Object[]> findParticipantsByConversationId(@Param("convId") Long convId);

    @Modifying
    @Query("UPDATE ConversationParticipants cp SET cp.participantRole = 'NOT_PARTICIPANT' " +
            "WHERE cp.conversation.id = :conversationId " +
            "AND cp.user.id IN :userIds " +
            "AND (cp.participantRole = 'PARTICIPANT' " +
            "       OR cp.participantRole = 'VICE_ADMIN' " +
            "       OR cp.participantRole = 'ADMIN')")
    void softRemoveParticipantInConversation(@Param("conversationId") Long conversationId,
                                             @Param("userIds") List<Long> userIds
    );

    @Modifying
    @Query("UPDATE ConversationParticipants cp SET cp.participantRole = 'NOT_PARTICIPANT' " +
            "WHERE cp.conversation.id = :convId AND cp.participantRole != 'NOT_PARTICIPANT'")
    void softRemoveAllParticipantsInConversation(@Param("convId") Long convId);

    @Query(value = """
                SELECT COALESCE(COUNT(cp.conversation_id),0) as num_of_user
                FROM conversation_participants cp
                WHERE cp.conversation_id =:convId AND cp.participant_role != 'NOT_PARTICIPANT'
            """, nativeQuery = true)
    int countUserInConversation(@Param("convId") Long convId);

    @Query(value = """
                SELECT cp FROM ConversationParticipants cp 
                WHERE cp.conversation.id = :conversationId 
                AND cp.participantRole != 'NOT_PARTICIPANT'
                AND cp.user.id != :actorId
            """)
    List<ConversationParticipants> findRemainingParticipantByConversationId(
            @Param("conversationId") Long conversationId,
            @Param("actorId") Long actorId
    );
}
