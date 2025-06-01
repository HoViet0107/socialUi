package personal.social.controllers.websocket;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import personal.social.config.socket.WebSocketSessionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * WebSocket Controller
 * REST endpoints để quản lý WebSocket connections và monitoring
 * Tích hợp với existing code structure
 */
@RestController
@RequestMapping("/api/v1/websocket")
@Slf4j
public class WebSocketController {

    @Autowired
    private WebSocketSessionManager sessionManager;

    /**
     * Sends a message to a specific user via WebSocket.
     *
     * @param request Map containing email (required), type (required), and payload (optional)
     * @return ResponseEntity with success status, email, and messageType on success,
     * or error message on validation failure, or 500 on internal error
     */

    @PostMapping("/send-to-user")
    public ResponseEntity<Map<String, Object>> sendMessageToUser(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            String messageType = (String) request.get("type");
            Object payload = request.get("payload");

            if (email == null || messageType == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email and type are required"));
            }

            Map<String, Object> message = Map.of(
                    "type", messageType,
                    "payload", payload != null ? payload : Map.of(),
                    "timestamp", System.currentTimeMillis()
            );

            sessionManager.sendToUser(email, message);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "email", email,
                    "messageType", messageType
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending message to user: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Broadcast message to conversation
     */
    @PostMapping("/broadcast-to-conversation")
    public ResponseEntity<Map<String, Object>> broadcastToConversation(@RequestBody Map<String, Object> request) {
        try {
            Long conversationId = Long.valueOf(request.get("conversationId").toString());
            String messageType = (String) request.get("type");
            Object payload = request.get("payload");

            if (conversationId == null || messageType == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "ConversationId and type are required"));
            }

            Map<String, Object> message = Map.of(
                    "type", messageType,
                    "conversationId", conversationId,
                    "payload", payload != null ? payload : Map.of(),
                    "timestamp", System.currentTimeMillis()
            );

            sessionManager.broadcastToConversation(conversationId, message);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "conversationId", conversationId,
                    "messageType", messageType
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error broadcasting to conversation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Force disconnect user sessions
     */
    @PostMapping("/disconnect-user")
    public ResponseEntity<Map<String, Object>> disconnectUser(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            String reason = (String) request.get("reason");

            if (email == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }

            Set<org.springframework.web.socket.WebSocketSession> userSessions = sessionManager.getUserSessions(email);
            int disconnectedCount = 0;

            for (org.springframework.web.socket.WebSocketSession session : userSessions) {
                try {
                    if (session.isOpen()) {
                        // Send disconnect notification before closing
                        Map<String, Object> disconnectMessage = Map.of(
                                "type", "FORCE_DISCONNECT",
                                "reason", reason != null ? reason : "Administrative action",
                                "timestamp", System.currentTimeMillis()
                        );

                        session.sendMessage(new org.springframework.web.socket.TextMessage(
                                new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(disconnectMessage)
                        ));

                        // Close session
                        session.close(org.springframework.web.socket.CloseStatus.NORMAL.withReason(reason));
                        disconnectedCount++;
                    }
                } catch (Exception e) {
                    log.error("Error disconnecting session: {}", e.getMessage(), e);
                }
            }

            Map<String, Object> response = Map.of(
                    "success", true,
                    "email", email,
                    "disconnectedSessions", disconnectedCount
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error disconnecting user: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Cleanup expired sessions manually
     */
    @PostMapping("/cleanup-sessions")
    public ResponseEntity<Map<String, Object>> cleanupSessions() {
        try {
            sessionManager.cleanupExpiredSessions();

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Session cleanup completed",
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error cleaning up sessions: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get WebSocket connection statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getConnectionStats() {
        try {
            long onlineUsers = sessionManager.getOnlineUserCount();
            Set<String> users = sessionManager.getOnlineUsers();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalOnlineUsers", onlineUsers);
            stats.put("onlineUsers", users);
            stats.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting connection stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint for WebSocket service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = Map.of(
                    "status", "UP",
                    "service", "WebSocket",
                    "onlineUsers", sessionManager.getOnlineUserCount(),
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Error in WebSocket health check: {}", e.getMessage(), e);
            return ResponseEntity.status(503).body(Map.of(
                    "status", "DOWN",
                    "error", e.getMessage()
            ));
        }
    }
}
