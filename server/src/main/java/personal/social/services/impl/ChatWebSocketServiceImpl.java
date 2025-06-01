package personal.social.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import personal.social.dto.message.ChatMessageDTO;
import personal.social.enums.MessageType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class ChatWebSocketServiceImpl implements personal.social.services.ChatWebSocketService {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void notifyMessageSent(ChatMessageDTO message) {
        // Gửi tin nhắn đến tất cả người tham gia cuộc trò chuyện
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + message.getConversationId(), message);
    }

    @Override
    public void notifyTyping(Long conversationId, String username, boolean isTyping) {
        // Thông báo có người đang gõ
        Map<String, Object> payload = new HashMap<>();
        payload.put("conversationId", conversationId);
        payload.put("username", username);
        payload.put("isTyping", isTyping);
        payload.put("type", MessageType.USER_TYPING);

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId + "/typing", payload);
    }

    @Override
    public void notifyMessageRead(Long conversationId, Long messageId, Long userId) {
        // Thông báo tin nhắn đã đọc
        Map<String, Object> payload = new HashMap<>();
        payload.put("conversationId", conversationId);
        payload.put("messageId", messageId);
        payload.put("userId", userId);
        payload.put("type", MessageType.MESSAGE_READ);

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId + "/read", payload);
    }

    @Override
    public void notifyUserOnlineStatus(Long userId, boolean isOnline) {
        // Thông báo trạng thái online/offline
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("isOnline", isOnline);
        payload.put("lastActive", LocalDateTime.now());
        payload.put("type", isOnline ? MessageType.USER_ONLINE : MessageType.USER_OFFLINE);

        messagingTemplate.convertAndSend("/topic/user/" + userId + "/status", payload);
    }
}
