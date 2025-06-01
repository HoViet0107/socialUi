package personal.social.services.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.multipart.MultipartFile;
import personal.social.config.socket.WebSocketSessionManager;
import personal.social.dto.message.ChatMessageDTO;
import personal.social.dto.CursorResponse;
import personal.social.dto.MediaDTO;
import personal.social.dto.message.MessageProjection;
import personal.social.dto.message.ReactionDTO;
import personal.social.enums.MessageStatus;
import personal.social.helper.CommonHelpers;
import personal.social.helper.MessageHelper;
import personal.social.helper.Utilities;
import personal.social.model.*;
import personal.social.repository.*;
import personal.social.services.MessageService;

import java.nio.file.AccessDeniedException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ConversationRepository conversationRepository;
    private final MessageMediaRepository mediaRepository;
    private final MessageReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final MessageReadStatusRepository readStatusRepository;
    private final MessageAsyncService messageAsyncService;
    private final WebSocketSessionManager sessionManager;
    private final CommonHelpers helpers;

    @Autowired
    public MessageServiceImpl(MessageRepository messageRepository, ConversationParticipantRepository participantRepository, ConversationRepository conversationRepository, MessageMediaRepository mediaRepository, MessageReactionRepository reactionRepository, UserRepository userRepository, MessageReadStatusRepository readStatusRepository, MessageAsyncService messageAsyncService, WebSocketSessionManager sessionManager, CommonHelpers helpers) {
        this.messageRepository = messageRepository;
        this.participantRepository = participantRepository;
        this.conversationRepository = conversationRepository;
        this.mediaRepository = mediaRepository;
        this.reactionRepository = reactionRepository;
        this.userRepository = userRepository;
        this.readStatusRepository = readStatusRepository;
        this.messageAsyncService = messageAsyncService;
        this.sessionManager = sessionManager;
        this.helpers = helpers;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public CursorResponse<ChatMessageDTO> getMessagesForConversation(Long conversationId, Long userId, LocalDateTime cursor, int size) throws AccessDeniedException {
        // 1. VALIDATE USER ACCESS -> throw AccessDeniedException if user is not in conversation
        Utilities.isUserHasAccessToConversation(userId, conversationId, participantRepository);

        // 2. MAIN QUERY
        Timestamp cursorTimestamp = cursor != null ? Timestamp.valueOf(cursor) : null;
        List<MessageProjection> messageProjections = messageRepository.findMessagesWithDetailsForConversation(
                conversationId, cursorTimestamp, size);

        if (messageProjections.isEmpty()) {
            return new CursorResponse<>(Collections.emptyList(), null, false);
        }

        // 3. EXTRACT IDs for batch loading
        List<Long> messageIds = messageProjections.stream()
                .map(MessageProjection::getId)
                .toList();


        // 4. PARALLEL BATCH LOADING - Execute all queries concurrently
        CompletableFuture<Map<Long, List<MediaRequest>>> mediaFuture =
                CompletableFuture.supplyAsync(() -> MessageHelper.loadMediaForMessages(mediaRepository, messageIds));

        CompletableFuture<Map<Long, List<MessageReaction>>> reactionsFuture =
                CompletableFuture.supplyAsync(() -> MessageHelper.loadReactionsForMessages(reactionRepository, messageIds));

        CompletableFuture<Map<Long, Boolean>> readStatusFuture =
                CompletableFuture.supplyAsync(() -> MessageHelper.loadReadStatusForMessages(readStatusRepository, messageIds, userId));


        // 5. WAIT FOR ALL BATCH OPERATIONS
        try {
            Map<Long, List<MediaRequest>> mediaMap = mediaFuture.get();
            Map<Long, List<MessageReaction>> reactionMap = reactionsFuture.get();
            Map<Long, Boolean> readStatusMap = readStatusFuture.get();

            // 6. BUILD DTOs
            List<ChatMessageDTO> messageDTOList = messageProjections.stream()
                    .map(projection -> MessageHelper.buildChatMessageDTO(projection, mediaMap, reactionMap, readStatusMap))
                    .toList();

            // 7. ASYNC MARK AS READ
            // messageAsyncService.markMessagesAsReadAsync(readStatusRepository, userId, messageDTOList);

            // 8. RETURN RESPONSE
            LocalDateTime nextCursor = messageDTOList.getLast().getSendAt();
            return new CursorResponse<>(messageDTOList, nextCursor, messageDTOList.size() == size);

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to load message data", e);
        }
    }

    @Override
    @Transactional
    public ChatMessageDTO sendMessage(ChatMessageDTO request, Users sender, MultipartFile file) throws AccessDeniedException {
        // VALIDATE USER ACCESS
        Utilities.isUserHasAccessToConversation(sender.getId(), request.getConversationId(), participantRepository);

        // CREATE MESSAGE
        Messages message = new Messages();
        message.setSendAt(LocalDateTime.now());
        message.setMessageContent(request.getContent());
        message.setStatus(MessageStatus.SENT);
        message.setSender(sender);
        message.setConversation(conversationRepository.findById(request.getConversationId()).orElseThrow());

        if (request.getReplyToMessageId() != null) {
            message.setReplyToMessageId(request.getReplyToMessageId());
        }

        // SAVE MESSAGE
        message = helpers.saveEntity(message,messageRepository, "message");

        // SET CONVERSATION LAST MESSAGE
        Conversations conversation = message.getConversation();
        conversation.setLastMessageId(message.getId());
        conversation.setEditedAt(LocalDateTime.now());

        helpers.saveEntity(conversation, conversationRepository, "conversation");

        // HANDLE MEDIA
        List<MediaRequest> mediaList = new ArrayList<>();
        if (file != null && !file.isEmpty()) {
            // TODO: load file to s3 -> {file: url, status: DELETED/ACTIVE, tpye: IMAGE/VIDEO/FILE}
            List<String> mediaUrls = new ArrayList<>();
            for (String link : mediaUrls) {
                MediaRequest media = MediaRequest.builder()
                        .mediaUrl(link)

                        .message(message)
                        .build();

                mediaList.add(media);
            }
            mediaRepository.saveAll(mediaList);
        }

        // HANDLE REACTIONS
        ChatMessageDTO messageDTO = MessageHelper.convertToDTO(message);
        messageDTO.setMedias(MessageHelper.convertMediaToDTO(mediaList));
        // messageDTO.setReactions(MessageHelper.convertReactionsToDTO(reactionRepository.findByMessageId(message.getId())));

        // BROADCAST REAL-TIME THROUGH WEBSOCKET
        MessageHelper.broadcastNewMessage(messageDTO, sessionManager);

        return messageDTO;
    }

    @Override
    @Transactional
    public void deleteMessage(Long messageId, Long userId) throws AccessDeniedException {
        Messages message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        // Kiểm tra quyền xóa (chỉ người gửi mới được xóa)
        if (!message.getSender().getId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to delete this message");
        }

        message.setDeleted(true);
        messageRepository.save(message);

        // Thông báo qua WebSocket nếu cần
        // chatWebSocketService.notifyMessageDeleted(messageId, message.getConversation().getId());
    }

    @Override
    @Transactional
    public ChatMessageDTO editMessage(Long messageId, String newContent, Long userId) throws AccessDeniedException {
        Messages message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        // Kiểm tra quyền chỉnh sửa (chỉ người gửi mới được chỉnh sửa)
        if (!message.getSender().getId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to edit this message");
        }

        message.setMessageContent(newContent);
        message.setEditedAt(LocalDateTime.now());
        message = messageRepository.save(message);

        ChatMessageDTO messageDTO = MessageHelper.convertToDTO(message);
        messageDTO.setMedias(MessageHelper.convertMediaToDTO(mediaRepository.findByMessageId(messageId)));
        messageDTO.setReactions(MessageHelper.convertReactionsToDTO(reactionRepository.findByMessageId(messageId)));

        // Thông báo qua WebSocket nếu cần
        // chatWebSocketService.notifyMessageEdited(messageDTO);

        return messageDTO;
    }

    @Override
    @Transactional
    public ReactionDTO addReaction(Long messageId, String reactionType, Long userId) {
        Messages message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Kiểm tra xem đã có reaction chưa
        MessageReaction existingReaction = reactionRepository.findByUserIdAndMessageId(userId, messageId);

        if (existingReaction != null) {
            // Cập nhật reaction nếu đã tồn tại
            existingReaction.setReactionType(reactionType);
            existingReaction = reactionRepository.save(existingReaction);

            return ReactionDTO.builder()
                    .id(existingReaction.getId())
                    .type(existingReaction.getReactionType())
                    .userId(userId)
                    .fullName(user.getFirstName() + " " + user.getSurname() + " " + user.getLastName())
                    .build();
        } else {
            // Tạo reaction mới
            MessageReaction reaction = MessageReaction.builder()
                    .reactionType(reactionType)
                    .createdAt(LocalDateTime.now())
                    .user(user)
                    .message(message)
                    .build();

            reaction = reactionRepository.save(reaction);

            ReactionDTO reactionDTO = ReactionDTO.builder()
                    .id(reaction.getId())
                    .type(reaction.getReactionType())
                    .userId(userId)
                    .fullName(user.getFirstName() + " " + user.getSurname() + " " + user.getLastName())
                    .build();

            // Thông báo qua WebSocket nếu cần
            // chatWebSocketService.notifyReactionAdded(messageId, message.getConversation().getId(), reactionDTO);

            return reactionDTO;
        }
    }

    @Override
    @Transactional
    public void removeReaction(Long messageId, Long userId) {
        MessageReaction reaction = reactionRepository.findByUserIdAndMessageId(userId, messageId);
        if (reaction != null) {
            reactionRepository.delete(reaction);

            // Thông báo qua WebSocket nếu cần
            // chatWebSocketService.notifyReactionRemoved(messageId, userId);
        }
    }
}
