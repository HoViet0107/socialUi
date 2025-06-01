package personal.social.config.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import personal.social.services.impl.ConversationServiceImpl;
import personal.social.services.impl.MessageServiceImpl;

import java.util.Map;

/**
 * Chat WebSocket Handler
 * Xử lý WebSocket connections và messages cho chat
 * Tích hợp với existing services
 */
@Component
@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {

    @Autowired
    private MessageServiceImpl messageService;

    @Autowired
    private ConversationServiceImpl conversationService;

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * When connection is established
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {

            // get email from session attributes(set by interceptor)
            String email = (String) session.getAttributes().get("email");

            if (email == null) {
                log.warn("No email found in session attributes, closing connection");
                session.close(CloseStatus.BAD_DATA.withReason("Authentication required"));
                return;
            }

            // Add session to manager
            sessionManager.addSession(session.getId(), session, email);

            // Send connection confirmation
            Map<String, Object> confirmMessage = Map.of(
                    "type", "CONNECTION_ESTABLISHED",
                    "sessionId", session.getId(),
                    "email", email,
                    "timestamp", System.currentTimeMillis()
            );

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(confirmMessage)));

            log.info("WebSocket connection established for user: {} with session: {}", email, session.getId());

        } catch (Exception e) {
            log.error("Error establishing WebSocket connection: {}", e.getMessage(), e);
            session.close(CloseStatus.SERVER_ERROR.withReason("Internal server error"));
        }
    }

    /**
     * Xử lý messages từ client
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            // Parse message
            String payload = message.getPayload().toString();
            @SuppressWarnings("unchecked")
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);

            // Validate session
            String email = sessionManager.getUserEmail(session.getId());
            if (email == null) {
                log.warn("Invalid session: {}", session.getId());
                session.close(CloseStatus.BAD_DATA.withReason("Invalid session"));
                return;
            }

            // Set sender info
            messageData.put("senderEmail", email);
            messageData.put("sessionId", session.getId());
            messageData.put("timestamp", System.currentTimeMillis());

            // Dispatch message based on type
            String messageType = (String) messageData.get("type");
            handleMessageByType(messageType, messageData, session);

            log.debug("Handled message type: {} from user: {}", messageType, email);

        } catch (Exception e) {
            log.error("Error handling WebSocket message: {}", e.getMessage(), e);

            // Send error response
            Map<String, Object> errorMessage = Map.of(
                    "type", "ERROR",
                    "error", "MESSAGE_PROCESSING_ERROR",
                    "message", e.getMessage()
            );

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMessage)));
        }
    }

    /**
     * Xử lý message theo type
     */
    private void handleMessageByType(String type, Map<String, Object> messageData, WebSocketSession session) {
        try {
            switch (type) {
                case "SEND_MESSAGE":
                    handleSendMessage(messageData, session);
                    break;
                case "JOIN_CONVERSATION":
                    handleJoinConversation(messageData, session);
                    break;
                case "LEAVE_CONVERSATION":
                    handleLeaveConversation(messageData, session);
                    break;
                case "TYPING_START":
                    handleTypingStart(messageData, session);
                    break;
                case "TYPING_STOP":
                    handleTypingStop(messageData, session);
                    break;
                default:
                    log.warn("Unknown message type: {}", type);
                    sendErrorToSession(session, "UNKNOWN_MESSAGE_TYPE", "Unknown message type: " + type);
            }
        } catch (Exception e) {
            log.error("Error handling message type {}: {}", type, e.getMessage(), e);
            sendErrorToSession(session, "MESSAGE_PROCESSING_ERROR", e.getMessage());
        }
    }

    /**
     * Handle SEND_MESSAGE
     */
    private void handleSendMessage(Map<String, Object> messageData, WebSocketSession session) {
        try {
            String content = (String) messageData.get("content");
            Long conversationId = Long.valueOf(messageData.get("conversationId").toString());
            String senderEmail = (String) messageData.get("senderEmail");

            // Validate access to conversation (sử dụng existing service)
            // Note: Cần thêm method hasAccess trong ConversationServiceImpl nếu chưa có

            // Save message using existing service
            // Note: Cần adapt method signature của MessageServiceImpl

            // Broadcast to conversation participants
            Map<String, Object> broadcastMessage = Map.of(
                    "type", "NEW_MESSAGE",
                    "conversationId", conversationId,
                    "content", content,
                    "senderEmail", senderEmail,
                    "timestamp", System.currentTimeMillis()
            );

            sessionManager.broadcastToConversation(conversationId, broadcastMessage);

            // Send delivery confirmation to sender
            Map<String, Object> confirmMessage = Map.of(
                    "type", "MESSAGE_DELIVERED",
                    "conversationId", conversationId,
                    "timestamp", System.currentTimeMillis()
            );

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(confirmMessage)));

            log.debug("Message sent successfully from: {}", senderEmail);

        } catch (Exception e) {
            log.error("Error handling send message: {}", e.getMessage(), e);
            sendErrorToSession(session, "SEND_MESSAGE_ERROR", e.getMessage());
        }
    }

    /**
     * Handle JOIN_CONVERSATION
     */
    private void handleJoinConversation(Map<String, Object> messageData, WebSocketSession session) {
        try {
            Long conversationId = Long.valueOf(messageData.get("conversationId").toString());
            String email = (String) messageData.get("senderEmail");

            // Validate conversation access using existing service
            // Note: Cần thêm method hasAccess trong ConversationServiceImpl nếu chưa có

            // Join conversation room
            sessionManager.joinConversation(session.getId(), conversationId);

            // Send confirmation
            Map<String, Object> confirmMessage = Map.of(
                    "type", "CONVERSATION_JOINED",
                    "conversationId", conversationId,
                    "timestamp", System.currentTimeMillis()
            );

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(confirmMessage)));

            log.debug("User {} joined conversation {}", email, conversationId);

        } catch (Exception e) {
            log.error("Error handling join conversation: {}", e.getMessage(), e);
            sendErrorToSession(session, "JOIN_CONVERSATION_ERROR", e.getMessage());
        }
    }

    /**
     * Handle LEAVE_CONVERSATION
     */
    private void handleLeaveConversation(Map<String, Object> messageData, WebSocketSession session) {
        try {
            Long conversationId = Long.valueOf(messageData.get("conversationId").toString());
            String email = (String) messageData.get("senderEmail");

            // Leave conversation room
            sessionManager.leaveConversation(session.getId(), conversationId);

            // Send confirmation
            Map<String, Object> confirmMessage = Map.of(
                    "type", "CONVERSATION_LEFT",
                    "conversationId", conversationId,
                    "timestamp", System.currentTimeMillis()
            );

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(confirmMessage)));

            log.debug("User {} left conversation {}", email, conversationId);

        } catch (Exception e) {
            log.error("Error handling leave conversation: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle TYPING_START
     */
    private void handleTypingStart(Map<String, Object> messageData, WebSocketSession session) {
        try {
            Long conversationId = Long.valueOf(messageData.get("conversationId").toString());
            String email = (String) messageData.get("senderEmail");

            // Create typing message
            Map<String, Object> typingMessage = Map.of(
                    "type", "USER_TYPING",
                    "conversationId", conversationId,
                    "userEmail", email,
                    "isTyping", true,
                    "timestamp", System.currentTimeMillis()
            );

            // Broadcast to other participants in conversation (exclude sender)
            sessionManager.broadcastToConversationExcept(conversationId, typingMessage, email);

        } catch (Exception e) {
            log.error("Error handling typing start: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle TYPING_STOP
     */
    private void handleTypingStop(Map<String, Object> messageData, WebSocketSession session) {
        try {
            Long conversationId = Long.valueOf(messageData.get("conversationId").toString());
            String email = (String) messageData.get("senderEmail");

            // Create stop typing message
            Map<String, Object> typingMessage = Map.of(
                    "type", "USER_TYPING",
                    "conversationId", conversationId,
                    "userEmail", email,
                    "isTyping", false,
                    "timestamp", System.currentTimeMillis()
            );

            // Broadcast to other participants in conversation (exclude sender)
            sessionManager.broadcastToConversationExcept(conversationId, typingMessage, email);

        } catch (Exception e) {
            log.error("Error handling typing stop: {}", e.getMessage(), e);
        }
    }

    /**
     * Xử lý transport errors
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage(), exception);

        // Try to notify user about error
        try {
            if (session.isOpen()) {
                Map<String, Object> errorMessage = Map.of(
                        "type", "TRANSPORT_ERROR",
                        "error", "CONNECTION_ERROR",
                        "message", "Connection error occurred"
                );

                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMessage)));
            }
        } catch (Exception e) {
            log.error("Failed to send error message: {}", e.getMessage());
        }
    }

    /**
     * Khi connection bị đóng
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        try {
            String email = sessionManager.getUserEmail(session.getId());

            // Remove session
            sessionManager.removeSession(session.getId());

            log.info("WebSocket connection closed for user: {} with session: {}, status: {}",
                    email, session.getId(), closeStatus);

        } catch (Exception e) {
            log.error("Error handling connection close: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if handler supports partial messages
     */
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * Send error message to session
     */
    private void sendErrorToSession(WebSocketSession session, String errorCode, String errorMessage) {
        try {
            Map<String, Object> error = Map.of(
                    "type", "ERROR",
                    "errorCode", errorCode,
                    "message", errorMessage,
                    "timestamp", System.currentTimeMillis()
            );

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        } catch (Exception e) {
            log.error("Error sending error to session: {}", e.getMessage(), e);
        }
    }
}
