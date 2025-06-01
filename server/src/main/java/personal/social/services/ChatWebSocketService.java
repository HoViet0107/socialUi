package personal.social.services;

import org.springframework.stereotype.Service;
import personal.social.dto.message.ChatMessageDTO;

@Service
public interface ChatWebSocketService {
    void notifyMessageSent(ChatMessageDTO message);
    void notifyTyping(Long conversationId, String username, boolean isTyping);
    void notifyMessageRead(Long conversationId, Long messageId, Long userId);
    void notifyUserOnlineStatus(Long userId, boolean isOnline);
}
