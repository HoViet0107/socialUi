package personal.social.config.socket;

import personal.social.config.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket Handshake Interceptor
 * Xử lý authentication cho WebSocket connections
 * Sử dụng JwtUtil đã có sẵn để validate token
 */
@Component
@Slf4j
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Before handshake - Xác thực user trước khi establish connection
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {
            log.info("=== WebSocket Handshake Debug ===");
            log.info("Request URI: {}", request.getURI());
            log.info("Request Headers: {}", request.getHeaders());
            log.info("Remote Address: {}", request.getRemoteAddress());

            // Extract JWT token from request
            String token = extractTokenFromRequest(request);
            log.info("Extracted token: {}", token != null ? "Present (length: " + token.length() + ")" : "NULL");

            if (token == null || token.isEmpty()) {
                log.warn("No JWT token found in WebSocket handshake request");
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }

            // Validate JWT token using existing JwtUtil
            try {
                if (jwtUtil.isTokenExpired(token)) {
                    log.warn("JWT token is expired in WebSocket handshake request");
                    response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                    return false;
                }
            } catch (Exception e) {
                log.error("Error validating JWT token: {}", e.getMessage(), e);
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }

            // Extract user information from token
            String email;
            try {
                email = jwtUtil.extractEmail(token);
                log.info("Extracted email from token: {}", email);
            } catch (Exception e) {
                log.error("Error extracting email from token: {}", e.getMessage(), e);
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }

            if (email == null || email.isEmpty()) {
                log.warn("Unable to extract email from JWT token");
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }

            // Store user information in WebSocket session attributes
            attributes.put("email", email);
            attributes.put("token", token);
            attributes.put("connectedAt", System.currentTimeMillis());
            attributes.put("remoteAddress", request.getRemoteAddress());
            attributes.put("userAgent", extractUserAgent(request));
            attributes.put("origin", extractOrigin(request));

            log.info("WebSocket handshake successful for user: {}", email);
            log.info("=== End WebSocket Handshake Debug ===");
            return true;

        } catch (Exception e) {
            log.error("Error during WebSocket handshake: {}", e.getMessage(), e);
            response.setStatusCode(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }
    }

    /**
     * After handshake - Cleanup sau khi handshake complete
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        try {
            if (exception != null) {
                log.error("WebSocket handshake failed: {}", exception.getMessage(), exception);
            } else {
                log.debug("WebSocket handshake completed successfully");
            }
        } catch (Exception e) {
            log.error("Error in WebSocket afterHandshake: {}", e.getMessage(), e);
        }
    }

    /**
     * <p>Extract JWT token từ request</>
     * Hỗ trợ nhiều cách truyền token:
     * <li>Query parameter: ?token=xxx</>
     * <li>Authorization header: Bearer xxx</>
     * <li>Cookie: jwt=xxx</>
     */
    private String extractTokenFromRequest(ServerHttpRequest request) {
        try {
            // Method 1: Extract from query parameter
            URI uri = request.getURI();
            String token = UriComponentsBuilder.fromUri(uri)
                    .build()
                    .getQueryParams()
                    .getFirst("token");

            if (token != null && !token.isEmpty()) {
                log.debug("Token extracted from query parameter");
                return token;
            }

            // Method 2: Extract from Authorization header
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                log.debug("Token extracted from Authorization header");
                return token;
            }

            // Method 3: Extract from Cookie
            String cookieHeader = request.getHeaders().getFirst("Cookie");
            if (cookieHeader != null) {
                token = extractTokenFromCookie(cookieHeader);
                if (token != null) {
                    log.debug("Token extracted from Cookie");
                    return token;
                }
            }

            log.debug("No token found in request");
            return null;

        } catch (Exception e) {
            log.error("Error extracting token from request: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extract token từ cookie string
     */
    private String extractTokenFromCookie(String cookieHeader) {
        try {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                cookie = cookie.trim();
                if (cookie.startsWith("jwt=")) {
                    return cookie.substring(4);
                }
                if (cookie.startsWith("access_token=")) {
                    return cookie.substring(13);
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error extracting token from cookie: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extract User-Agent từ request headers
     */
    private String extractUserAgent(ServerHttpRequest request) {
        try {
            String userAgent = request.getHeaders().getFirst("User-Agent");
            return userAgent != null ? userAgent : "Unknown";
        } catch (Exception e) {
            log.error("Error extracting user agent: {}", e.getMessage(), e);
            return "Unknown";
        }
    }

    /**
     * Extract Origin từ request headers
     */
    private String extractOrigin(ServerHttpRequest request) {
        try {
            String origin = request.getHeaders().getFirst("Origin");
            if (origin == null) {
                origin = request.getHeaders().getFirst("Referer");
            }
            return origin != null ? origin : "Unknown";
        } catch (Exception e) {
            log.error("Error extracting origin: {}", e.getMessage(), e);
            return "Unknown";
        }
    }
}
