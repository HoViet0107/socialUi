package personal.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the response for authentication, typically returned after a user successfully logs in.
 * This class contains the authentication token that will be used for subsequent requests and a message
 * providing additional information, such as success or failure details.
 *
 * @Data Automatically generates getters, setters, equals, hashCode, and toString methods.
 * @AllArgsConstructor Generates a constructor with all fields.
 * @NoArgsConstructor Generates a default constructor (no arguments).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    /**
     * The authentication token (e.g., JWT token) to be used in subsequent requests for authorization.
     */
    private String token;

    /**
     * A message providing additional details, typically indicating the status of authentication (e.g., success or error).
     */
    private String message;
}
