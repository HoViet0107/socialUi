package personal.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the data transfer object for a user, containing essential user information.
 * This DTO is used for transmitting user-related information, such as email and password, in API requests.
 *
 * @Data Automatically generates getters, setters, equals, hashCode, and toString methods.
 * @AllArgsConstructor Generates a constructor with all fields.
 * @NoArgsConstructor Generates a default constructor (no arguments).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthDTO {
    /**
     * The email address of the user.
     */
    private String email;

    /**
     * The password of the user.
     */
    private String password;
}

