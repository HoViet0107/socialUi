package personal.social.helper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import personal.social.config.JwtUtil;
import personal.social.model.Users;
import personal.social.repository.UserRepository;

import java.util.List;

/**
 * Utility class providing common helper methods used across the application
 * Contains methods for token extraction, user authentication, and entity operations
 */
@Component
@Slf4j
public class CommonHelpers {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepos;

    @Autowired
    public CommonHelpers(JwtUtil jwtUtil, UserRepository userRepos) {
        this.jwtUtil = jwtUtil;
        this.userRepos = userRepos;
    }

    /**
     * Extracts and validates JWT token from HTTP request header
     * Retrieves the associated user from the database
     *
     * @param request The HTTP request containing the Authorization header
     * @return The authenticated user
     * @throws RuntimeException if token is missing, invalid, expired, or user not found
     */
    public Users extractToken(HttpServletRequest request) {
        // extract token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // get token without "Bearer "
        } else {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        if (jwtUtil.isTokenExpired(token)) {
            throw new RuntimeException("Token expired");
        } // end extract token

        Users existedUser = userRepos.findByEmail(jwtUtil.extractEmail(token));
        if (existedUser == null) {
            throw new RuntimeException("User not existed!");
        }

        return existedUser;
    }

    /**
     * 💾 Save entity and return saved instance with generated ID
     */
    public <T> T saveEntity(T entity, JpaRepository<T, ?> repository, String entityDescription) {
        try {
            // IMPORTANT: Assign return value from save()
            T savedEntity = repository.save(entity);

            log.debug("Successfully saved {}: {}", entityDescription, savedEntity);
            return savedEntity;

        } catch (Exception e) {
            String errorMessage = String.format("Failed to save %s: %s", entityDescription, e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    /**
     * Persists a collection of entities to the database in a single batch operation
     *
     * @param <T> The type of entities to be saved
     * @param entities List of entities to persist
     * @param repository JPA repository for the entity type
     * @param entityDescription Human-readable description of the entity type for logging
     * @return List of saved entities with generated IDs and updated fields
     * @throws RuntimeException if the batch save operation fails
     */
    public <T> List<T> saveAllEntities(List<T> entities, JpaRepository<T, ?> repository, String entityDescription) {
        try {
            List<T> savedEntities = repository.saveAll(entities);

            log.debug("Successfully saved {} {}: {}", savedEntities.size(), entityDescription, savedEntities);
            return savedEntities;
        } catch (Exception e) {
            String errorMessage = String.format("Failed to save %s list: %s", entityDescription, e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }
}
