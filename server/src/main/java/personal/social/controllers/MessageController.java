package personal.social.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import personal.social.dto.message.ChatMessageDTO;
import personal.social.dto.CursorResponse;
import personal.social.enums.MessageStatus;
import personal.social.enums.MessageType;
import personal.social.helper.CommonHelpers;
import personal.social.model.Users;
import personal.social.services.FileStorageService;
import personal.social.services.MessageService;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {
    private final CommonHelpers helpers;
    private final FileStorageService fileStorageService;
    private final MessageService messageService;

    // lấy toàn bộ tin nhắn của 1 đoạn chat
    @GetMapping("/{conversationId}")
    public ResponseEntity<CursorResponse<ChatMessageDTO>> getMessagesForConversation(
            @PathVariable Long conversationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursor,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) throws AccessDeniedException {
        Users user = helpers.extractToken(request);
        return ResponseEntity.ok(messageService.getMessagesForConversation(conversationId, user.getId(), cursor, size));
    }

    // gửi tin nhắn văn bản(có thể có file đính kèm)
    @PostMapping(value = "/{conversationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatMessageDTO> sendMessage(
            @PathVariable Long conversationId,
            @RequestParam("content") String content,
            @RequestParam("status") String status,
            @RequestParam(value = "replyToMessageId", required = false) Long replyToMessageId,
            @RequestParam("mediaType") String mediaType,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) throws AccessDeniedException {
        Users sender = helpers.extractToken(request);

        ChatMessageDTO messageDTO = ChatMessageDTO.builder()
                .conversationId(conversationId)
                .content(content)
                .senderId(sender.getId())
                .type(MessageType.valueOf(mediaType))
                .status(MessageStatus.valueOf(status))
                .replyToMessageId(replyToMessageId)
                .isRead(false).isDeleted(false)
                .build();

        return ResponseEntity.ok(messageService.sendMessage(messageDTO, sender, file));
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<ChatMessageDTO> editMessage(
            @PathVariable Long messageId,
            @RequestParam String newContent,
            HttpServletRequest request) throws AccessDeniedException {
        Users user = helpers.extractToken(request);
        return ResponseEntity.ok(messageService.editMessage(messageId, newContent, user.getId()));
    }

    @PutMapping("/{messageId}/delete")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            HttpServletRequest request) throws AccessDeniedException {
        Users user = helpers.extractToken(request);
        messageService.deleteMessage(messageId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<Void> addReaction(
            @PathVariable Long messageId,
            @RequestParam String reactionType,
            HttpServletRequest request) {
        Users user = helpers.extractToken(request);
        messageService.addReaction(messageId, reactionType, user.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{messageId}/reactions")
    public ResponseEntity<Void> removeReaction(
            @PathVariable Long messageId,
            HttpServletRequest request) {
        Users user = helpers.extractToken(request);
        messageService.removeReaction(messageId, user.getId());
        return ResponseEntity.ok().build();
    }
}
