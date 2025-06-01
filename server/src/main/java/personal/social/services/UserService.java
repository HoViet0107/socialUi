package personal.social.services;

import personal.social.dto.AuthResponse;
import personal.social.dto.AuthDTO;
import personal.social.model.Users;

import java.util.Optional;

/**
 * Service interface for managing user-related operations.
 * Provides methods for user authentication, registration, and profile management.
 */
public interface UserService {
    /**
     * Registers a new user in the system.
     *
     * @param user The user object containing registration details
     * @return The registered user with generated ID and encrypted password
     * @throws RuntimeException if email already exists, phone already exists, or password validation fails
     */
    Users register(Users user);

    /**
     * Authenticates a user and generates a JWT token.
     *
     * @param userDTO The user credentials (email and password)
     * @return AuthResponse containing JWT token and success message
     * @throws RuntimeException if user not found or password is invalid
     */
    AuthResponse login(AuthDTO userDTO);

    /**
     * Retrieves a user by their unique ID.
     *
     * @param userId The ID of the user to retrieve
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<Users> getUserById(Long userId);

    /**
     * Retrieves a user by their email address.
     *
     * @param user User object containing the email to search for
     * @return The user with the specified email or null if not found
     */
    Users getUserByEmail(Users user);

    /**
     * Updates an existing user's profile information.
     *
     * @param existedUser The current user object from the database
     * @param user The user object containing updated information
     * @return The updated user object
     * @throws RuntimeException if password validation fails
     */
    Users editUser(Users existedUser, Users user);
}
