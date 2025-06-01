package personal.social.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import personal.social.config.socket.WebSocketSessionManager;
import personal.social.dto.message.ChatMessageDTO;
import personal.social.dto.MediaDTO;
import personal.social.dto.message.MessageProjection;
import personal.social.dto.message.ReactionDTO;
import personal.social.enums.MessageStatus;
import personal.social.enums.MessageType;
import personal.social.model.*;
import personal.social.repository.MessageMediaRepository;
import personal.social.repository.MessageReactionRepository;
import personal.social.repository.MessageRepository;
import personal.social.repository.MessageReadStatusRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class MessageHelper {

    /**
     * Converts a Messages entity to a ChatMessageDTO.
     * 
     * @param message The Messages entity to convert
     * @return ChatMessageDTO representation of the message
     */
    public static ChatMessageDTO convertToDTO(Messages message) {
        if (message == null) return null;

        return ChatMessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .sendAt(message.getSendAt())
                .editedAt(message.getEditedAt())
                .content(message.getMessageContent())
                .status(message.getStatus())
                .isDeleted(message.isDeleted())
                .senderId(message.getSender().getId())
                .senderName(Utilities.buildFullName(message.getSender()))
                .senderAvatar(message.getSender().getAvatarUrl())
                .type(MessageType.NEW_MESSAGE)
                .isRead(false)
                .build();
    }

    /**
     * Converts a list of MediaRequest entities to a list of MediaDTOs.
     * 
     * @param mediaList List of MediaRequest entities to convert
     * @return List of MediaDTOs
     */
    public static List<MediaDTO> convertMediaToDTO(List<MediaRequest> mediaList) {
        return mediaList.stream()
                .map(media -> MediaDTO.builder()
                        .id(media.getId())
                        .url(media.getMediaUrl())
                        .thumbnailUrl(media.getThumbnailUrl())
                        .mediaType(media.getMediaType())
                        .fileSize(media.getFileSize())
                        .duration(media.getDuration())
                        .width(media.getWidth())
                        .height(media.getHeight())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Converts a list of MessageReaction entities to a list of ReactionDTOs.
     * 
     * @param reactionList List of MessageReaction entities to convert
     * @return List of ReactionDTOs
     */
    public static List<ReactionDTO> convertReactionsToDTO(List<MessageReaction> reactionList) {
        return reactionList.stream()
                .map(reaction -> ReactionDTO.builder()
                        .id(reaction.getId())
                        .type(reaction.getReactionType())
                        .userId(reaction.getUser().getId())
                        .fullName(Utilities.buildFullName(reaction.getUser()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Loads media attachments for a list of messages.
     * 
     * @param mediaRepository Repository for accessing media data
     * @param messageIds List of message IDs to load media for
     * @return Map of message IDs to their associated media requests
     */
    public static Map<Long, List<MediaRequest>> loadMediaForMessages(
            MessageMediaRepository mediaRepository,
            List<Long> messageIds) {

        if (messageIds.isEmpty()) return Collections.emptyMap();

        return mediaRepository.findByMessageIdIn(messageIds)
                .stream()
                .collect(Collectors.groupingBy(media -> media.getMessage().getId()));
    }

    /**
     * Loads reactions for a list of messages.
     * 
     * @param reactionRepository Repository for accessing reaction data
     * @param messageIds List of message IDs to load reactions for
     * @return Map of message IDs to their associated reactions
     */
    public static Map<Long, List<MessageReaction>> loadReactionsForMessages(
            MessageReactionRepository reactionRepository,
            List<Long> messageIds) {

        if (messageIds.isEmpty()) return Collections.emptyMap();

        return reactionRepository.findByMessageIdIn(messageIds)
                .stream()
                .collect(Collectors.groupingBy(reaction -> reaction.getMessage().getId()));
    }

    /**
     * Loads replied messages for a list of message IDs.
     * 
     * @param messageRepository Repository for accessing message data
     * @param replyMessageIds List of message IDs to load replies for
     * @return Map of message IDs to their replied messages
     */
    public static Map<Long, Messages> loadRepliedMessages(MessageRepository messageRepository, List<Long> replyMessageIds) {
        if (replyMessageIds.isEmpty()) return Collections.emptyMap();

        return messageRepository.findAllById(replyMessageIds).stream()
                .collect(Collectors.toMap(Messages::getId, Function.identity()));
    }

    public static Map<Long, Boolean> loadReadStatusForMessages(
            MessageReadStatusRepository readStatusRepository,
            List<Long> messageIds,
            Long userId) {

        if (messageIds.isEmpty()) return Collections.emptyMap();

        // Single query to get read status for all messages
        Set<Long> readMessageIds = readStatusRepository.findReadMessageIdsByUserAndMessages(messageIds, userId);

        // Convert to map for O(1) lookup
        return messageIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        readMessageIds::contains
                ));
    }

    public static boolean isMessageReadByUser(MessageReadStatusRepository readStatusRepository, Long messageId, Long userId) {
        MessageReadStatus readStatus = readStatusRepository.findByMessageIdAndUserId(messageId, userId);
        return readStatus != null && readStatus.getReadAt() != null;
    }

    public static ChatMessageDTO buildChatMessageDTO(
            MessageProjection projection,
            Map<Long, List<MediaRequest>> mediaMap,
            Map<Long, List<MessageReaction>> reactionMap,
            Map<Long, Boolean> readStatusMap) {

        return ChatMessageDTO.builder()
                .id(projection.getId())
                .conversationId(projection.getConversationId())
                .sendAt(projection.getSendAt())
                .editedAt(projection.getEditedAt())
                .content(projection.getMessageContent())
                .status(MessageStatus.valueOf(projection.getMessageStatus()))
                .isDeleted(projection.getIsDeleted())
                .senderId(projection.getSenderId())
                .senderName(Utilities.buildFullName(projection.getFirstName(), projection.getSurname(), projection.getLastName()))
                .senderAvatar(projection.getSenderAvatar())
                .replyToMessageId(projection.getReplyToMessageId())
                .medias(MessageHelper.convertMediaToDTO(mediaMap.getOrDefault(projection.getId(), Collections.emptyList())))
                .reactions(MessageHelper.convertReactionsToDTO(reactionMap.getOrDefault(projection.getId(), Collections.emptyList())))
                .type(MessageType.NEW_MESSAGE)
                .isRead(readStatusMap.getOrDefault(projection.getId(), false))
                .build();
    }

    public static void broadcastNewMessage(ChatMessageDTO messageDTO, WebSocketSessionManager sessionManager) {
        try {
            // Kiểm tra null messageDTO để tránh NullPointerException
            if (messageDTO == null) {
                log.error("Cannot broadcast null message");
                return;
            }
            
            // 📊 Prepare complete message data for broadcast - Use HashMap to avoid Null
            Map<String, Object> broadcastMessage = new HashMap<>();
            broadcastMessage.put("type", "NEW_MESSAGE");
            broadcastMessage.put("messageId", messageDTO.getId());
            broadcastMessage.put("conversationId", messageDTO.getConversationId());
            broadcastMessage.put("content", messageDTO.getContent() != null ? messageDTO.getContent() : "");
            broadcastMessage.put("senderId", messageDTO.getSenderId());
            broadcastMessage.put("senderName", messageDTO.getSenderName() != null ? messageDTO.getSenderName() : "");
            broadcastMessage.put("sendAt", messageDTO.getSendAt() != null ? messageDTO.getSendAt().toString() : "");
            broadcastMessage.put("medias", messageDTO.getMedias() != null ? messageDTO.getMedias() : Collections.emptyList());
            broadcastMessage.put("replyToMessageId", messageDTO.getReplyToMessageId());
            broadcastMessage.put("timestamp", System.currentTimeMillis());

            // Broadcast to all participants in conversation
            sessionManager.broadcastToConversation(messageDTO.getConversationId(), broadcastMessage);

            log.info("Broadcasted new message {} to conversation {}",
                    messageDTO.getId(), messageDTO.getConversationId());
        } catch (Exception e) {
            log.error("Error broadcasting new message: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }
}
