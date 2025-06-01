package personal.social.config.socket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket Configuration
 * Cấu hình WebSocket endpoints và interceptors
 * Tích hợp với existing JwtUtil và SecurityConfig
 */
@Slf4j
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("=== REGISTERING WEBSOCKET HANDLERS ===");

        // Native WebSocket without SockJS
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(webSocketHandshakeInterceptor)
                .setAllowedOriginPatterns("*"); // Remove .withSockJS()

        log.info("Native WebSocket endpoint registered at: /ws/chat");
        log.info("=== WEBSOCKET REGISTRATION COMPLETE ===");
    }
}
