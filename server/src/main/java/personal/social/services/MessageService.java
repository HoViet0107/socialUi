package personal.social.services;

import jakarta.transaction.Transactional;
import org.springframework.web.multipart.MultipartFile;
import personal.social.dto.message.ChatMessageDTO;
import personal.social.dto.CursorResponse;
import personal.social.dto.message.ReactionDTO;
import personal.social.model.Users;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
/**
 * Service to manage messages. This service provides methods to retrieve messages
 * for a given conversation, mark messages as read and send new messages.
 *
 * @author Ho Quoc Viet
 */
public interface MessageService {

    /**
     * Retrieve messages for a specific conversation.
     *
     * @param conversationId the ID of the conversation
     * @param userId the ID of the user requesting messages
     * @param cursor the timestamp to paginate messages
     * @param size the number of messages to retrieve
     * @return a response containing a list of chat messages
     * @throws AccessDeniedException if user access is denied
     */
    @Transactional
    CursorResponse<ChatMessageDTO> getMessagesForConversation(Long conversationId, Long userId, LocalDateTime cursor, int size) throws AccessDeniedException;

    /**
     * Send a message in a conversation.
     *
     * @param request the message details
     * @param sender the user sending the message
     * @param fileUrl the URL of any attached file
     * @return the sent chat message
     * @throws AccessDeniedException if user access is denied
     */
    @Transactional
    ChatMessageDTO sendMessage(ChatMessageDTO request, Users sender, MultipartFile file) throws AccessDeniedException;

    /**
     * Delete a message.
     *
     * @param messageId the ID of the message to delete
     * @param userId the ID of the user attempting to delete the message
     * @throws AccessDeniedException if user access is denied
     */
    @Transactional
    void deleteMessage(Long messageId, Long userId) throws AccessDeniedException;

    /**
     * Edit a message.
     *
     * @param messageId the ID of the message to edit
     * @param newContent the new content for the message
     * @param userId the ID of the user attempting to edit the message
     * @return the edited chat message
     * @throws AccessDeniedException if user access is denied
     */
    @Transactional
    ChatMessageDTO editMessage(Long messageId, String newContent, Long userId) throws AccessDeniedException;

    /**
     * Add a reaction to a message.
     *
     * @param messageId the ID of the message to react to
     * @param reactionType the type of reaction
     * @param userId the ID of the user adding the reaction
     * @return the added reaction details
     */
    @Transactional
    ReactionDTO addReaction(Long messageId, String reactionType, Long userId);

    /**
     * Remove a reaction from a message.
     *
     * @param messageId the ID of the message
     * @param userId the ID of the user removing the reaction
     */
    @Transactional
    void removeReaction(Long messageId, Long userId);
}
