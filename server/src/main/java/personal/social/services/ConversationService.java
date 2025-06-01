package personal.social.services;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import personal.social.dto.*;
import personal.social.dto.conversation.ConversationRequest;
import personal.social.dto.conversation.CreateConversationRequest;
import personal.social.dto.conversation.EditConversationRequest;
import personal.social.model.Users;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public interface ConversationService {
    @Transactional(readOnly = true)
    CursorResponse<ConversationRequest> getConversationsForUser(Users user, LocalDateTime cursor, int size) throws AccessDeniedException;

    @Transactional
    ConversationRequest createConversation(CreateConversationRequest request) throws AccessDeniedException;

    @Transactional(readOnly = true)
    ConversationRequest getConversationById(Long conversationId, Long userId) throws AccessDeniedException;

    @Transactional
    String addParticipant(Long conversationId, Long actorId, List<Long> addedUserId) throws AccessDeniedException;

    @Transactional
    String leaveConversation(Long conversationId, Users actor);

    @Transactional
    String editConversation(Long conversationId, EditConversationRequest editRequest, Long userId) throws AccessDeniedException, IllegalStateException;

    @Transactional
    Map<String, Object> removeParticipantsFromConversation(Long conversationId, Long actorId, List<Long> removedUserId) throws AccessDeniedException, IllegalStateException, EntityNotFoundException;


    // TODO: void markConversationAsRead(Long conversationId, Long userId) throws AccessDeniedException;

    // not implemented yet
    @Transactional(readOnly = true)
    CursorResponse<UserDTO> searchUsersToAddToConversation(Long conversationId, Long userId, String keyword, LocalDateTime cursor, int size) throws AccessDeniedException, IllegalStateException;

    // not implemented yet
    @Transactional
    String deleteConversation(Long conversationId, Long userId) throws AccessDeniedException;

    @Transactional(readOnly = true)
    List<UserDTO> getParticipantsInConversation(Long conversationId, Long userId) throws AccessDeniedException;
}
