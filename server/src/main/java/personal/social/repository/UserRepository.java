package personal.social.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import personal.social.model.Users;

/**
 * Repository interface for managing users
 * Provides methods to find users by email, phone, and ID
 */
@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    /**
     * Finds a user by email
     * 
     * @param email The user's email
     * @return The user if found
     */
    Users findByEmail(String email);
    
    /**
     * Finds a user by phone number
     * 
     * @param phone The user's phone number
     * @return The user if found
     */
    Users findByPhone(String phone);
}
