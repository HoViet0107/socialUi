package personal.social.helper;

import lombok.extern.slf4j.Slf4j;
import personal.social.config.socket.WebSocketSessionManager;
import personal.social.dto.UserDTO;
import personal.social.repository.ConversationParticipantRepository;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ConversationHelper {
    public static List<UserDTO> getParticipantsForConversation(Long conversationId, ConversationParticipantRepository participantRepository) {
        // Lấy danh sách người tham gia cuộc trò chuyện
        List<Object[]> participants = participantRepository.findParticipantsByConversationId(conversationId);

        if (participants.isEmpty()) {
            return List.of();
        }

        return participants.stream().map(row -> UserDTO.builder()
                .userId(((Number) row[0]).longValue())
                .email((String) row[1])
                .fullName(Utilities.buildFullName((String) row[2], (String) row[3], (String) row[4]))
                .avatarUrl((String) row[5])
                .isOnline(false)
                .participantRole((String) row[6])
                .joinedAt(((Timestamp) row[7]).toLocalDateTime())
                .build()).collect(Collectors.toList());
    }

    public static void broadcastParticipantsLeaveConversation(Long conversationId, String userName, WebSocketSessionManager sessionManager) {
        // TODO: Implement broadcastParticipantsLeaveConversation
        try {
            Map<String, Object> broadcastMessage = new HashMap<>();
            broadcastMessage.put("type", "PARTICIPANTS_LEAVE_CONVERSATION");
            broadcastMessage.put("conversationId", conversationId);
            broadcastMessage.put("userName", userName);
            broadcastMessage.put("timestamp", System.currentTimeMillis());

            // Broadcast to all participants in conversation
            sessionManager.broadcastToConversation(conversationId, broadcastMessage);

            log.info("Broadcasted PARTICIPANTS_LEAVE_CONVERSATION to conversation {} for user {}",
                    userName, conversationId);
        } catch (Exception e) {
            log.error("Error broadcasting new message: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }
}
