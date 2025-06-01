package personal.social.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import personal.social.config.JwtUtil;
import personal.social.helper.CommonHelpers;
import personal.social.model.Users;
import personal.social.repository.UserRepository;
import personal.social.services.UserService;

import java.util.Objects;

/**
 * Controller for handling user operations
 * Provides endpoints for retrieving and updating user information
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepos;
    private final CommonHelpers commonHelpers;

    @Autowired
    public UserController(UserService userService, JwtUtil jwtUtil, UserRepository userRepos, CommonHelpers commonHelpers){
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.userRepos = userRepos;
        this.commonHelpers = commonHelpers;
    }

    /**
     * Retrieves a user by ID
     * 
     * @param userId The user ID
     * @return ResponseEntity containing the user or error message
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId){
        try{
            return ResponseEntity.ok(userService.getUserById(userId));
        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Retrieves the current user's profile
     * Requires authentication
     * 
     * @param request The HTTP request containing authentication
     * @return ResponseEntity containing the user profile or error message
     */
    @Operation(summary = "get user profile", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/profile")
    public ResponseEntity<?> getUserDetails(HttpServletRequest request){
        try{
            Users existedUser = commonHelpers.extractToken(request);
            return ResponseEntity.ok(userService.getUserByEmail(existedUser));
        }catch (RuntimeException e){
            return ResponseEntity.badRequest().body(e);
        }
    }

    /**
     * Updates a user's profile
     * Requires authentication and authorization
     * 
     * @param userId The user ID to update
     * @param user The updated user data
     * @param request The HTTP request containing authentication
     * @return ResponseEntity containing the updated user or error message
     */
    @Operation(summary = "edit user profile", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{userId}")
    public ResponseEntity<?> editUser(
            @PathVariable Long userId,
            @RequestBody Users user,
            HttpServletRequest request
    ){
        Users existedUser = commonHelpers.extractToken(request);
        if(existedUser == null){
            throw new RuntimeException("User not existed!");
        }

        if(!Objects.equals(existedUser.getId(), userId)){
            return ResponseEntity.status(403).body("You don't have permission to edit this user");
        }

        try{
            return ResponseEntity.ok(userService.editUser(existedUser, user));
        }catch (Exception e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
