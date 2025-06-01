package personal.social.controllers;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import personal.social.dto.*;
import personal.social.dto.conversation.ConversationRequest;
import personal.social.dto.conversation.CreateConversationRequest;
import personal.social.dto.conversation.EditConversationRequest;
import personal.social.helper.CommonHelpers;
import personal.social.model.Users;
import personal.social.services.ConversationService;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {
    private final CommonHelpers helpers;
    private final ConversationService conversationService;

    @Autowired
    public ConversationController(CommonHelpers helpers, ConversationService conversationService) {
        this.helpers = helpers;
        this.conversationService = conversationService;
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "Successfully retrieved conversations")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "No conversation found")
    public ResponseEntity<CursorResponse<ConversationRequest>> getUserConversations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursor,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) throws AccessDeniedException {
        Users user = helpers.extractToken(request);
        CursorResponse<ConversationRequest> response = conversationService.getConversationsForUser(user, cursor, size);
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/{conversationId}")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved conversation")
    @ApiResponse(responseCode = "403", description = "Access denied to conversation")
    @ApiResponse(responseCode = "404", description = "Conversation not found")
    public ResponseEntity<ConversationRequest> getConversation(
            @PathVariable Long conversationId,
            HttpServletRequest request) throws AccessDeniedException {
        Users user = helpers.extractToken(request);
        ConversationRequest response = conversationService.getConversationById(conversationId, user.getId());
        return ResponseEntity.ok(response);
    }

    // tao đoạn chat
    @PostMapping
    public ResponseEntity<ConversationRequest> createConversation(
            @RequestBody CreateConversationRequest request,
            HttpServletRequest httpServletRequest) throws AccessDeniedException {
        Users user = helpers.extractToken(httpServletRequest);
        ConversationRequest response = conversationService.createConversation(request);
        return ResponseEntity.ok(response);
    }

    // sửa thông tin đoạn chat
    @PutMapping("/{conversationId}")
    public ResponseEntity<String> editConversation(
            @PathVariable Long conversationId,
            @RequestBody EditConversationRequest editRequest,
            HttpServletRequest request) throws AccessDeniedException, IllegalStateException {
        Users user = helpers.extractToken(request);
        return ResponseEntity.ok(conversationService.editConversation(conversationId, editRequest, user.getId()));
    }

    // xóa đoạn chat
    @PutMapping("/{conversationId}/delete")
    public ResponseEntity<String> deleteConversation(
            @PathVariable Long conversationId,
            HttpServletRequest request) throws AccessDeniedException {
        Users user = helpers.extractToken(request);
        return ResponseEntity.ok(conversationService.deleteConversation(conversationId, user.getId()));
    }

    // thêm người dùng vào đoạn chat
    @PostMapping("/{conversationId}/participants")
    public ResponseEntity<String> addParticipantsToConversation(
            @PathVariable Long conversationId,
            @RequestBody List<Long> participantIds,
            HttpServletRequest request) throws AccessDeniedException {
        Users user = helpers.extractToken(request);
        return ResponseEntity.ok(conversationService.addParticipant(conversationId, user.getId(), participantIds));
    }

    // người dùng rời khỏi đoạn chat
    @PutMapping("/{conversationId}/participants/leave")
    public ResponseEntity<String> leaveConversation(
            @PathVariable Long conversationId,
            HttpServletRequest request) {
        Users actor = helpers.extractToken(request);
        return ResponseEntity.ok(conversationService.leaveConversation(conversationId, actor));
    }

    // xóa người dùng khỏi đoạn chat
    @PutMapping("/{conversationId}/participants/remove")
    public ResponseEntity<Map<String, Object>> removeParticipantsFromConversation(
            @PathVariable Long conversationId,
            @RequestBody List<Long> removedUserId,
            HttpServletRequest request) throws AccessDeniedException, IllegalStateException {
        Users user = helpers.extractToken(request);
        Map<String, Object> response = conversationService.removeParticipantsFromConversation(conversationId, user.getId(), removedUserId);
        return ResponseEntity.ok(response);
    }

    // TODO: mark conversation as read

    // tìm kiếm người dùng để thêm vào đoạn chat
    @GetMapping("/{conversationId}/participants/search")
    public ResponseEntity<CursorResponse<UserDTO>> searchUsersToAddToConversation(
            @PathVariable Long conversationId,
            @RequestParam String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursor,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) throws AccessDeniedException, IllegalStateException {
        Users user = helpers.extractToken(request);
        CursorResponse<UserDTO> response = conversationService.searchUsersToAddToConversation(conversationId, user.getId(), keyword, cursor, size);
        return ResponseEntity.ok(response);
    }

    // lấy danh sách người dùng trong đoạn chat
    @GetMapping("/{conversationId}/participants")
    public ResponseEntity<List<UserDTO>> getParticipantsInConversation(
            @PathVariable Long conversationId,
            HttpServletRequest request) throws AccessDeniedException {
        Users user = helpers.extractToken(request);
        List<UserDTO> response = conversationService.getParticipantsInConversation(conversationId, user.getId());
        return ResponseEntity.ok(response);
    }
}
