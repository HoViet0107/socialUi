package personal.social.config.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Session Manager
 * Quản lý sessions, user mappings và rooms
 * Phiên bản đơn giản để tích hợp với existing code
 */
@Component
@Slf4j
public class WebSocketSessionManager {

    @Autowired
    private ObjectMapper objectMapper;

    // In-memory session storage
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>(); // sessionId -> email
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>(); // email -> sessionIds
    private final Map<Long, Set<String>> conversationSessions = new ConcurrentHashMap<>(); // conversationId -> sessionIds

    /**
     * Thêm session mới
     */
    public void addSession(String sessionId, WebSocketSession session, String email) {
        try {
            // Local storage
            sessions.put(sessionId, session);
            sessionUserMap.put(sessionId, email);

            userSessions.computeIfAbsent(email, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

            log.info("Added session {} for user {}", sessionId, email);
        } catch (Exception e) {
            log.error("Error adding session: {}", e.getMessage(), e);
        }
    }

    /**
     * Xóa session
     */
    public void removeSession(String sessionId) {
        try {
            // Get user email
            String email = sessionUserMap.get(sessionId);

            if (email != null) {
                // Local storage
                sessions.remove(sessionId);
                sessionUserMap.remove(sessionId);

                Set<String> userSessionSet = userSessions.get(email);
                if (userSessionSet != null) {
                    userSessionSet.remove(sessionId);
                    if (userSessionSet.isEmpty()) {
                        userSessions.remove(email);
                    }
                }

                // Remove from all conversation rooms
                removeFromAllConversations(sessionId);

                log.info("Removed session {} for user {}", sessionId, email);
            }
        } catch (Exception e) {
            log.error("Error removing session: {}", e.getMessage(), e);
        }
    }

    /**
     * Lấy session theo ID
     */
    public WebSocketSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Lấy tất cả sessions của user
     */
    public Set<WebSocketSession> getUserSessions(String email) {
        Set<String> sessionIds = userSessions.get(email);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<WebSocketSession> result = new HashSet<>();
        for (String sessionId : sessionIds) {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                result.add(session);
            }
        }
        return result;
    }

    /**
     * Lấy user email từ session
     */
    public String getUserEmail(String sessionId) {
        return sessionUserMap.get(sessionId);
    }

    /**
     * Join conversation room
     */
    public void joinConversation(String sessionId, Long conversationId) {
        try {
            conversationSessions.computeIfAbsent(conversationId, k -> ConcurrentHashMap.newKeySet())
                    .add(sessionId);

            log.info("Session {} joined conversation {}", sessionId, conversationId);
        } catch (Exception e) {
            log.error("Error joining conversation: {}", e.getMessage(), e);
        }
    }

    /**
     * Leave conversation room
     */
    public void leaveConversation(String sessionId, Long conversationId) {
        try {
            Set<String> sessions = conversationSessions.get(conversationId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    conversationSessions.remove(conversationId);
                }
            }

            log.info("Session {} left conversation {}", sessionId, conversationId);
        } catch (Exception e) {
            log.error("Error leaving conversation: {}", e.getMessage(), e);
        }
    }

    /**
     * Lấy tất cả sessions trong conversation
     */
    public Set<WebSocketSession> getConversationSessions(Long conversationId) {
        Set<String> sessionIds = conversationSessions.get(conversationId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<WebSocketSession> result = new HashSet<>();
        for (String sessionId : sessionIds) {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                result.add(session);
            }
        }
        return result;
    }

    /**
     * Broadcast message to all participants in a conversation
     */
    public void broadcastToConversation(Long conversationId, Map<String, Object> message) {
        try {
            Set<WebSocketSession> sessions = getConversationSessions(conversationId);
            String messageJson = objectMapper.writeValueAsString(message);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(messageJson));
                }
            }

            log.debug("Broadcasted message to {} sessions in conversation {}", sessions.size(), conversationId);

        } catch (Exception e) {
            log.error("Error broadcasting to conversation: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast message to conversation participants except specified user
     */
    public void broadcastToConversationExcept(Long conversationId, Map<String, Object> message, String excludeEmail) {
        try {
            Set<WebSocketSession> sessions = getConversationSessions(conversationId);
            String messageJson = objectMapper.writeValueAsString(message);

            for (WebSocketSession session : sessions) {
                String sessionEmail = getUserEmail(session.getId());
                if (session.isOpen() && !excludeEmail.equals(sessionEmail)) {
                    session.sendMessage(new TextMessage(messageJson));
                }
            }

        } catch (Exception e) {
            log.error("Error broadcasting to conversation except user: {}", e.getMessage(), e);
        }
    }

    /**
     * Send message to specific user (all their sessions)
     */
    public void sendToUser(String email, Map<String, Object> message) {
        try {
            Set<WebSocketSession> sessions = getUserSessions(email);
            String messageJson = objectMapper.writeValueAsString(message);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(messageJson));
                }
            }

            log.debug("Sent message to user {} ({} sessions)", email, sessions.size());

        } catch (Exception e) {
            log.error("Error sending to user: {}", e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra user có online không
     */
    public boolean isUserOnline(String email) {
        Set<String> userSessionSet = userSessions.get(email);
        return userSessionSet != null && !userSessionSet.isEmpty();
    }

    /**
     * Lấy số lượng users online
     */
    public long getOnlineUserCount() {
        return userSessions.size();
    }

    /**
     * Lấy tất cả users online
     */
    public Set<String> getOnlineUsers() {
        return new HashSet<>(userSessions.keySet());
    }

    /**
     * Remove session from all conversations
     */
    private void removeFromAllConversations(String sessionId) {
        conversationSessions.values().forEach(sessions -> sessions.remove(sessionId));
    }

    /**
     * Cleanup expired sessions (có thể gọi định kỳ)
     */
    public void cleanupExpiredSessions() {
        try {
            sessions.entrySet().removeIf(entry -> !entry.getValue().isOpen());

            userSessions.entrySet().removeIf(entry -> {
                entry.getValue().removeIf(sessionId -> !sessions.containsKey(sessionId));
                return entry.getValue().isEmpty();
            });

            conversationSessions.values().forEach(sessionSet ->
                    sessionSet.removeIf(sessionId -> !sessions.containsKey(sessionId))
            );

            log.debug("Cleaned up expired sessions");
        } catch (Exception e) {
            log.error("Error during session cleanup: {}", e.getMessage());
        }
    }
}
