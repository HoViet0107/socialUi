package personal.social.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import personal.social.config.JwtUtil;
import personal.social.dto.AuthResponse;
import personal.social.dto.AuthDTO;
import personal.social.enums.RoleEnum;
import personal.social.model.Users;
import personal.social.repository.RolesRepository;
import personal.social.repository.UserRepository;
import personal.social.services.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the UserService interface.
 * Handles user authentication, registration, and profile management operations.
 */
@Service
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepo;
    private final RolesRepository roleRepos;
    private final JwtUtil jwtUtil;

    /**
     * Constructor for UserServiceImpl with dependency injection.
     *
     * @param passwordEncoder For encrypting and verifying passwords
     * @param userRepo Repository for user data access
     * @param jwtUtil Utility for JWT token generation and validation
     * @param roleRepos Repository for user roles
     */
    @Autowired
    public UserServiceImpl(PasswordEncoder passwordEncoder, UserRepository userRepo, JwtUtil jwtUtil, RolesRepository roleRepos) {
        this.passwordEncoder = passwordEncoder;
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
        this.roleRepos = roleRepos;
    }

    /**
     * {@inheritDoc}
     * Authenticates a user and generates a JWT token.
     */
    @Override
    public AuthResponse login(AuthDTO userDTO) {
        Users existedUser = userRepo.findByEmail(userDTO.getEmail());
        if(existedUser == null) {
            throw new RuntimeException("User not found!");
        }
        if(!passwordEncoder.matches(userDTO.getPassword(), existedUser.getPassword())) {
            throw new RuntimeException("Invalid password!");
        }

        List<RoleEnum> roleNames = new ArrayList<>();
        roleNames.add(existedUser.getRoles().getRole());
        // generate token
        String token = jwtUtil.generateToken(userDTO.getEmail(), roleNames);
        AuthResponse authResponse = new AuthResponse(token, "Login successfully!");
        return authResponse;
    }

    /**
     * {@inheritDoc}
     * Retrieves a user by their unique ID.
     */
    @Override
    public Optional<Users> getUserById(Long userId) {
        return userRepo.findById(userId);
    }

    /**
     * {@inheritDoc}
     * Retrieves a user by their email address.
     */
    @Override
    public Users getUserByEmail(Users user) {
        return userRepo.findByEmail(user.getEmail());
    }

    /**
     * {@inheritDoc}
     * Registers a new user in the system with validation.
     */
    @Override
    public Users register(Users user) {
        if(userRepo.findByEmail(user.getEmail()) != null) {
            throw new RuntimeException("Email already existed!");
        }

        if (userRepo.findByPhone(user.getPhone()) != null) {
            throw new RuntimeException("Phone number already existed!");
        }

        if(!validatePassword(user)) {
            throw new RuntimeException("Invalid password!");
        }
        // encrypt password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        // set role
        user.setRoles(roleRepos.findByRole(RoleEnum.USER));
        // save user
        return userRepo.save(user);
    }

    /**
     * {@inheritDoc}
     * Updates an existing user's profile information.
     */
    @Override
    public Users editUser(Users existedUser, Users user) {
        // Check if password is being updated
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            // Validate the new password
            if (!validatePassword(user)) {
                throw new RuntimeException("Invalid password!");
            }
            // Encrypt the new password
            existedUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Check if phone is being changed and validate it
        if (user.getPhone() != null && !user.getPhone().equals(existedUser.getPhone())) {
            existedUser.setPhone(user.getPhone());
        }

        // Update other user information
        if (user.getFirstName() != null && !user.getFirstName().equals(existedUser.getFirstName())) {
            existedUser.setFirstName(user.getFirstName());
        }
        if (user.getSurname() != null  && !user.getSurname().equals(existedUser.getSurname())) {
            existedUser.setSurname(user.getSurname());
        }
        if (user.getLastName() != null && !user.getLastName().equals(existedUser.getLastName())) {
            existedUser.setLastName(user.getLastName());
        }
        if (user.getDob() != null && !user.getDob().equals(existedUser.getDob())) {
            existedUser.setDob(user.getDob());
        }

        return userRepo.save(existedUser);
    }

    /**
     * Validates a user's password against security requirements.
     * Password must:
     * 1. Be at least 8 characters long
     * 2. Not exceed 16 characters
     * 3. Contain at least 1 number
     * 4. Contain at least 1 uppercase letter
     * 5. Contain at least 1 lowercase letter
     * 6. Contain at least 1 special character
     *
     * @param user The user object containing the password to validate
     * @return true if password meets all requirements
     * @throws RuntimeException if any validation rule fails
     */
    private boolean validatePassword(Users user) {
        if (user.getPassword().length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters!");
        }
        if (user.getPassword().length() > 16) {
            throw new RuntimeException("Password cannot be longer than 16 characters!");
        }
        if(!user.getPassword().matches(".*[0-9].*")) {
            throw new RuntimeException("Password must contain at least 1 number!");
        }
        if(!user.getPassword().matches(".*[A-Z].*")) {
            throw new RuntimeException("Password must contain at least 1 uppercase character!");
        }
        if(!user.getPassword().matches(".*[a-z].*")) {
            throw new RuntimeException("Password must contain at least 1 lowercase character!");
        }
        if(!user.getPassword().matches(".*[!@#$%^&*()].*")) {
            throw new RuntimeException("Password must contain at least 1 special character!");
        }
        return true;
    }
}
