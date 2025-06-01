package personal.social.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import personal.social.enums.RoleEnum;
import personal.social.model.Roles;

/**
 * Repository interface for managing user roles
 * Provides methods to find roles by role enum
 */
@Repository
public interface RolesRepository extends JpaRepository<Roles, Long> {
    /**
     * Finds a role by role enum
     * 
     * @param roleEnum The role enum (ADMIN, USER, etc.)
     * @return The role if found
     */
    @Query(value = "SELECT r FROM Roles r WHERE r.role =:roleEnum")
    Roles findByRole(RoleEnum roleEnum);
}
