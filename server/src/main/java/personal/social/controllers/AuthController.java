package personal.social.controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import personal.social.dto.AuthDTO;
import personal.social.model.Users;
import personal.social.repository.UserRepository;
import personal.social.services.UserService;

/**
 * Controller for handling authentication operations
 * Provides endpoints for user registration and login
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepo;
    
    @Autowired
    public AuthController(UserService userService, UserRepository userRepo) {
        this.userService = userService;
        this.userRepo = userRepo;
    }
    
    /**
     * Registers a new user
     * 
     * @param user The user data for registration
     * @return ResponseEntity containing the registered user or error message
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Users user) {
        try {
            return ResponseEntity.ok(userService.register(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Authenticates a user and generates a JWT token
     * 
     * @param userDTO The user credentials for login
     * @return ResponseEntity containing the authentication token or error message
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthDTO userDTO) {
        try {
            return ResponseEntity.ok(userService.login(userDTO));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
