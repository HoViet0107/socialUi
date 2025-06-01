package personal.social.services.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import personal.social.config.socket.WebSocketSessionManager;
import personal.social.dto.*;
import personal.social.dto.conversation.ConversationRequest;
import personal.social.dto.conversation.CreateConversationRequest;
import personal.social.dto.conversation.EditConversationRequest;
import personal.social.dto.message.ChatMessageDTO;
import personal.social.enums.MessageStatus;
import personal.social.enums.MessageType;
import personal.social.enums.ParticipantRole;
import personal.social.enums.ParticipantValidationResult;
import personal.social.helper.CommonHelpers;
import personal.social.helper.ConversationHelper;
import personal.social.helper.MessageHelper;
import personal.social.helper.Utilities;
import personal.social.model.*;
import personal.social.model.id.ConversationParticipantsId;
import personal.social.repository.*;
import personal.social.services.ConversationService;

import java.nio.file.AccessDeniedException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MessageReadStatusRepository readStatusRepository;
    private final WebSocketSessionManager sessionManager;
    private final CommonHelpers helpers;

    @Override
    @Transactional(readOnly = true)
    public CursorResponse<ConversationRequest> getConversationsForUser(Users user, LocalDateTime cursor, int size) throws AccessDeniedException {
        // VALIDATE USER ACCESS
        if (user == null) {
            throw new AccessDeniedException("User not found");
        }
        Timestamp cursorTimestamp = cursor != null ? Timestamp.valueOf(cursor) : null;
        List<Object[]> results = conversationRepository.findUserConversationsWithCursor(
                user.getId(), cursorTimestamp, size);
        // Nếu không có kết quả, trả về danh sách rỗng
        if (results.isEmpty()) {
            return new CursorResponse<>(Collections.emptyList(), null, false);
        }

        List<ConversationRequest> convDTOList = results.stream().map(row -> {
            Long convId = ((Number) row[0]).longValue();
            boolean isRead = true; // Mặc định là đã đọc

            ChatMessageDTO lastMessage = null;
            if (row[6] != null) {  // Nếu có last message
                Long messageId = ((Number) row[6]).longValue();
                Long senderId = ((Number) row[10]).longValue();

                // Kiểm tra tin nhắn đã đọc chưa nếu không phải người gửi
                if (!senderId.equals(user.getId())) {
                    isRead = MessageHelper.isMessageReadByUser(readStatusRepository, messageId, user.getId());
                }
                // Tạo đối tượng tin nhắn cuối cùng
                lastMessage = ChatMessageDTO.builder()
                        .id(messageId)
                        .content((String) row[7])
                        .sendAt(((Timestamp) row[8]).toLocalDateTime())
                        .status(MessageStatus.valueOf((String) row[9]))
                        .senderId(senderId)
                        .senderName(Utilities.buildFullName((String) row[11], (String) row[12], (String) row[13]))
                        .isRead(isRead)
                        .isDeleted((boolean) row[14]).build();
            }
            // Return ConversationDTO object
            return ConversationRequest.builder()
                    .id(convId)
                    .title((String) row[1])
                    .avatar((String) row[2])
                    .isGroupChat((Boolean) row[3])
                    .createAt(((Timestamp) row[4]).toLocalDateTime())
                    .lastMessage(lastMessage)
                    .unreadCount(row[15] != null ? ((Number) row[15]).intValue() : 0)
                    .participants(ConversationHelper.getParticipantsForConversation(convId, participantRepository))
                    .isTyping(false) // Mặc định là không có ai đang gõ
                    .build();
        }).collect(Collectors.toList());        // Xác định cursor tiếp theo một cách an toàn
        LocalDateTime nextCursor = null;
        if (!convDTOList.isEmpty()) {
            Object[] lastRow = results.getLast();
            Timestamp editedAt = (Timestamp) lastRow[5];  // edited_at
            Timestamp createdAt = (Timestamp) lastRow[4]; // create_at

            // Sử dụng edited_at nếu có, nếu không thì dùng create_at
            Timestamp timestampToUse = editedAt != null ? editedAt : createdAt;
            if (timestampToUse != null) {
                nextCursor = timestampToUse.toLocalDateTime();
            }
        }

        return new CursorResponse<>(convDTOList, nextCursor, convDTOList.size() == size);
    }


    @Override
    @Transactional
    public ConversationRequest createConversation(CreateConversationRequest request) throws AccessDeniedException, IllegalArgumentException, EntityNotFoundException {
        // Kiểm tra đầu vào
        if (request.getParticipantIds() == null ||
                request.getParticipantIds().isEmpty() ||
                request.getParticipantIds().size() > 50) {
            throw new IllegalArgumentException("Participant list cannot be empty");
        }

        // Đảm bảo người tạo nằm trong danh sách người tham gia
        if (!request.getParticipantIds().contains(request.getCreatorId())) {
            request.getParticipantIds().add(request.getCreatorId());
        }

        // Kiểm tra xem đã có cuộc trò chuyện giữa các người dùng chưa (nếu không phải nhóm)
        if (!request.isGroupChat() && request.getParticipantIds().size() == 2) {
            Long userId1 = request.getParticipantIds().get(0);
            Long userId2 = request.getParticipantIds().get(1);
            // Nếu có cuộc trò chuyện đã tồn tại, trả về thông tin cuộc trò chuyện đó
            try {
                Optional<Conversations> existingConversation = conversationRepository.findOneDirectConversationBetweenUsers(userId1, userId2);
                if (existingConversation.isPresent()) {
                    return getConversationById(existingConversation.get().getId(), userId1);
                }
            } catch (Exception e) {
                log.warn("Error when checking existing conversation: ", e);
                // Kiểm tra lại nếu có lỗi (có thể do race condition)
                Optional<Conversations> retryCheck = conversationRepository.findOneDirectConversationBetweenUsers(userId1, userId2);
                if (retryCheck.isPresent()) {
                    return getConversationById(retryCheck.get().getId(), userId1);
                }
            }
        }
        LocalDateTime currentTime = LocalDateTime.now();

        // Tạo cuộc trò chuyện mới
        Conversations conversation = new Conversations();
        conversation.setTitle(request.getTitle());
        conversation.setAvatarUrl(request.getAvatar());
        conversation.setGroupChat(request.isGroupChat());
        conversation.setCreateAt(currentTime);
        conversation.setEditedAt(currentTime);
        // Lưu đối tượng conversation trước để có ID
        conversation = conversationRepository.save(conversation);

        // Chuẩn bị danh sách người tham gia
        List<ConversationParticipants> participants = new ArrayList<>();
        for (Long participantId : request.getParticipantIds()) {
            Users user = userRepository.findById(participantId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + participantId));
            // Tạo ID kết hợp
            ConversationParticipantsId convParticipantId = new ConversationParticipantsId(
                    conversation.getId(),
                    user.getId()
            );
            ConversationParticipants participant = new ConversationParticipants();
            participant.setId(convParticipantId);
            participant.setUser(user);
            participant.setConversation(conversation);
            participant.setJoinedAt(currentTime);
            // Nếu là nhóm, người tạo sẽ là admin
            if (Objects.equals(request.getCreatorId(), user.getId())) {
                participant.setParticipantRole(ParticipantRole.ADMIN);
            } else {
                participant.setParticipantRole(ParticipantRole.PARTICIPANT);
            }

            participants.add(participant);
        }
        participantRepository.saveAll(participants);

        // Tạo tin nhắn hệ thống (nếu là nhóm)
        if (request.isGroupChat()) {
            Users creator = userRepository.findById(request.getCreatorId())
                    .orElseThrow(() -> new EntityNotFoundException("Creator not found!"));

            String systemMessage = Utilities.buildFullName(creator) + " created the group!";
            Messages message = new Messages();
            message.setSendAt(LocalDateTime.now());
            message.setMessageContent(systemMessage);
            message.setStatus(MessageStatus.DELIVERED);
            message.setSender(creator);
            message.setConversation(conversation);

            messageRepository.save(message);

            conversation.setLastMessageId(message.getId());
            conversationRepository.save(conversation);
        }

        // Lấy thông tin cuộc trò chuyện vừa tạo
        return getConversationById(conversation.getId(), request.getCreatorId());
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationRequest getConversationById(Long conversationId, Long userId) throws AccessDeniedException {
        Conversations conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));

        // Kiểm tra quyền truy cập
        Utilities.isUserHasAccessToConversation(userId, conversationId, participantRepository);

        // Lấy tin nhắn cuối cùng
        ChatMessageDTO lastMessage = null;
        if (conversation.getLastMessageId() != null) {
            Messages message = messageRepository.findById(conversation.getLastMessageId()).orElse(null);
            if (message != null) {
                boolean isRead = true;
                if (!message.getSender().getId().equals(userId)) {
                    isRead = MessageHelper.isMessageReadByUser(readStatusRepository, message.getId(), userId);
                }

                lastMessage = ChatMessageDTO.builder()
                        .id(message.getId())
                        .conversationId(conversationId)
                        .content(message.getMessageContent())
                        .sendAt(message.getSendAt())
                        .status(message.getStatus())
                        .senderId(message.getSender().getId())
                        .senderName(Utilities.buildFullName(message.getSender()))
                        .isRead(isRead)
                        .type(MessageType.NEW_MESSAGE)
                        .isDeleted(message.isDeleted())
                        .build();
            }
        }

        // Lấy số tin nhắn chưa đọc
        int unreadCount = messageRepository.countUnreadMessagesInConversation(conversationId, userId);

        return ConversationRequest.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .avatar(conversation.getAvatarUrl())
                .isGroupChat(conversation.isGroupChat())
                .createAt(conversation.getCreateAt())
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .participants(ConversationHelper.getParticipantsForConversation(conversationId, participantRepository))
                .isTyping(false)
                .build();
    }

    @Override
    @Transactional
    public String leaveConversation(Long conversationId, Users actor) {
        ConversationParticipants participant = participantRepository.findByUserInConversation(actor.getId(), conversationId);
        if (participant == null) {
            throw new EntityNotFoundException("User is not a participant in this conversation!");
        }

        Conversations conversation = participant.getConversation();
        if (!conversation.isGroupChat()) {
            throw new IllegalStateException("Cannot leave a direct conversation!");
        }


        List<ConversationParticipants> participants = new ArrayList<>();
        List<ConversationParticipants> remainingParticipants = participantRepository
                .findRemainingParticipantByConversationId(conversationId, actor.getId());
        // IF USER IS ADMIN AND THERE ARE OTHER MEMBERS THEN TRANSFER ADMIN ROLE
        if (participant.getParticipantRole() == ParticipantRole.ADMIN) {
            // MARK USER AS NOT PARTICIPANT OF CONVERSATION
            participant.setParticipantRole(ParticipantRole.NOT_PARTICIPANT);
            participants.add(participant);

            if (!remainingParticipants.isEmpty()) {
                // FIND VICE ADMIN
                Optional<ConversationParticipants> viceAdmin = remainingParticipants.stream()
                        .filter(p -> p.getParticipantRole() == ParticipantRole.VICE_ADMIN)
                        .findFirst();

                if (viceAdmin.isPresent() && remainingParticipants.size() > 2) {
                    // SET NEW ROW AS ADMIN
                    viceAdmin.get().setParticipantRole(ParticipantRole.ADMIN);
                    participants.add(viceAdmin.get());
                    participantRepository.saveAll(participants);
                } else if (remainingParticipants.size() > 2) {
                    // TRANSFER ADMIN ROLE TO THE FIRST REMAINING PARTICIPANT
                    ConversationParticipants newAdmin = remainingParticipants.getFirst();
                    newAdmin.setParticipantRole(ParticipantRole.ADMIN);
                    participants.add(newAdmin);
                    participantRepository.saveAll(participants);
                    helpers.saveAllEntities(participants, participantRepository, "participants");
                } else {
                    // IF THERE IS ONLY 2 MEMBER LEFT, REMOVE ALL PARTICIPANTS
                    remainingParticipants.forEach(p -> p.setParticipantRole(ParticipantRole.NOT_PARTICIPANT));
                    participantRepository.saveAll(remainingParticipants);
                    return "Group has been deleted!";
                }
            }
        } else {
            if (remainingParticipants.size() < 3) {
                // REMOVE ALL PARTICIPANTS
                participant.setParticipantRole(ParticipantRole.NOT_PARTICIPANT);
                remainingParticipants.forEach(p -> p.setParticipantRole(ParticipantRole.NOT_PARTICIPANT));
                participantRepository.saveAll(remainingParticipants);
                return "Group has been deleted!";
            } else{
                // REMOVE PARTICIPANT
                participant.setParticipantRole(ParticipantRole.NOT_PARTICIPANT);
                participantRepository.save(participant);
            }
        }

        // CREATE SYSTEM MESSAGE
        String systemMessage = Utilities.buildFullName(actor) + " left the group!";

        Messages message = Messages.builder()
                .sendAt(LocalDateTime.now())
                .messageContent(systemMessage)
                .status(MessageStatus.DELIVERED)
                .sender(actor)
                .conversation(conversation)
                .isDeleted(false).isRead(false).build();
        helpers.saveEntity(message, messageRepository, "message");

        // SET LAST MESSAGE FOR CONVERSATION
        conversation.setLastMessageId(message.getId());
        conversation.setEditedAt(LocalDateTime.now());
        helpers.saveEntity(conversation, conversationRepository, "conversation");

        // BROADCAST REAL-TIME THROUGH WEBSOCKET
        ConversationHelper.broadcastParticipantsLeaveConversation(conversationId, Utilities.buildFullName(actor), sessionManager);

        return systemMessage;
    }

    @Override
    @Transactional
    public String addParticipant(Long conversationId, Long actorId, List<Long> addedUserIds) throws AccessDeniedException, IllegalStateException {
        // Kiểm tra người thêm có trong cuộc trò chuyện không
        ConversationParticipants requesterParticipant = participantRepository.findByUserInConversation(actorId, conversationId);
        if (requesterParticipant == null) {
            throw new AccessDeniedException("You are not a participant in this conversation!");
        }

        // kiểm tra người thêm có phải là admin không
        if (!requesterParticipant.getParticipantRole().equals(ParticipantRole.ADMIN)) {
            throw new AccessDeniedException("You are not an admin of this conversation!");
        }

        // Kiểm tra cuộc trò chuyện có phải nhóm không
        Conversations conversation = requesterParticipant.getConversation();
        if (!conversation.isGroupChat()) {
            throw new IllegalStateException("Cannot add participant to a direct conversation!");
        }
        // Kiểm tra danh sách người dùng được thêm
        if (addedUserIds == null || addedUserIds.isEmpty()) {
            throw new IllegalArgumentException("Added user list cannot be empty");
        }
        List<Long> existingUserIds = new ArrayList<>();
        List<Long> nonExistingUserIds = new ArrayList<>();
        // Kiểm tra xem người dùng đã có trong nhóm chưa
        for (Long addedUserId : addedUserIds) {
            ConversationParticipants existingParticipant = participantRepository.findByUserInConversation(addedUserId, conversationId);
            if (existingParticipant != null) {
                existingUserIds.add(addedUserId);
            } else {
                nonExistingUserIds.add(addedUserId);
            }
        }

        // Thêm người tham gia mới
        List<Users> newParticipants = new ArrayList<>();
        for (Long addedUserId : nonExistingUserIds) {
            try {
                Users addedUser = userRepository.findById(addedUserId)
                        .orElseThrow(() -> new EntityNotFoundException("User not found with ID " + addedUserId));
                newParticipants.add(addedUser);
            } catch (EntityNotFoundException e) {
                log.warn("User not found: {}", addedUserId);
            }
        }
        if (newParticipants.isEmpty()) {
            throw new IllegalArgumentException("No new participants to add");
        }
        // Thêm người tham gia mới vào danh sách
        List<ConversationParticipants> newParticipantList = new ArrayList<>();
        for (Users addedUser : newParticipants) {
            // Tạo composite key
            ConversationParticipantsId participantId = new ConversationParticipantsId();
            participantId.setConversationId(conversationId);
            participantId.setUserId(addedUser.getId());

            // Thêm người tham gia mới vào danh sách
            ConversationParticipants newParticipant = new ConversationParticipants();
            newParticipant.setId(participantId);
            newParticipant.setUser(addedUser);
            newParticipant.setConversation(conversation);
            newParticipant.setJoinedAt(LocalDateTime.now());
            newParticipant.setParticipantRole(ParticipantRole.PARTICIPANT);
            newParticipantList.add(newParticipant);
        }
        participantRepository.saveAll(newParticipantList);

        // Tạo tin nhắn hệ thống
        // Chuẩn bị danh sách tên người được thêm
        List<String> addedNames = newParticipants.stream()
                .map(Utilities::buildFullName)
                .collect(Collectors.toList());

        // Lập nội dung tin nhắn
        StringBuilder systemMessage = new StringBuilder(Utilities.buildFullName(requesterParticipant.getUser()) + " added ");
        int displayLimit = 3;

        if (addedNames.size() <= displayLimit) {
            // Nếu dưới 3 người, liệt kê hết
            systemMessage.append(String.join(", ", addedNames));
        } else {
            // Nếu hơn 3 người, hiển thị 3 tên và số người còn lại
            List<String> displayed = addedNames.subList(0, displayLimit);
            int others = addedNames.size() - displayLimit;
            systemMessage.append(String.join(", ", displayed));
            systemMessage.append(" and ").append(others).append(" others");
        }
        systemMessage.append(" to the group!");
        // Cập nhật LastMessageId cho conversation

        Messages message = new Messages();
        message.setSendAt(LocalDateTime.now());
        message.setMessageContent(String.valueOf(systemMessage));
        message.setStatus(MessageStatus.DELIVERED);
        message.setSender(requesterParticipant.getUser());
        message.setConversation(conversation);
        messageRepository.save(message);

        // Cập nhật LastMessageId cho conversation
        conversation.setLastMessageId(message.getId());
        conversation.setEditedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Thông báo qua WebSocket nếu cần
        // sửa lại gửi message thông báo vào nhóm chat
        // chatWebSocketService.notifyParticipantAdded(message);

        return String.valueOf(systemMessage);
    }

    @Override
    @Transactional
    public String editConversation(Long conversationId, EditConversationRequest editRequest, Long userId) throws AccessDeniedException, IllegalStateException {
        // Kiểm tra quyền truy cập
        ConversationParticipants participant = participantRepository.findByUserInConversation(userId, conversationId);
        if (participant == null) {
            throw new AccessDeniedException("You are not a participant in this conversation!");
        }

        Conversations conversation = participant.getConversation();
        if (editRequest.getTitle() == null && editRequest.getAvatarUrl() == null) {
            throw new IllegalStateException("No changes to be made!");
        }

        boolean updated = false;

        if (editRequest.getTitle() != null && !editRequest.getTitle().equals(conversation.getTitle())) {
            conversation.setTitle(editRequest.getTitle());
            updated = true;
        }

        if (editRequest.getAvatarUrl() != null && !editRequest.getAvatarUrl().equals(conversation.getAvatarUrl())) {
            conversation.setAvatarUrl(editRequest.getAvatarUrl());
            updated = true;
        }

        String systemMessage = null;
        if (updated) {
            conversation.setEditedAt(LocalDateTime.now());
            conversationRepository.save(conversation);

            // Tạo tin nhắn hệ thống về thay đổi
            Users user = participant.getUser();
            systemMessage = Utilities.buildFullName(user) + " Changed the conversation " + editRequest.getDescription() + "!";

            Messages message = new Messages();
            message.setSendAt(LocalDateTime.now());
            message.setMessageContent(systemMessage);
            message.setStatus(MessageStatus.DELIVERED);
            message.setSender(user);
            message.setConversation(conversation);

            messageRepository.save(message);

            // Cập nhật LastMessageId
            conversation.setLastMessageId(message.getId());
            conversationRepository.save(conversation);

            // Thông báo qua WebSocket nếu cần
            // chatWebSocketService.notifyConversationUpdated(conversationId);
        }
        return systemMessage;
    }

    private ParticipantValidationResult validateParticipant(Long participantId, ConversationParticipants requester, Long conversationId) {
        // Kiểm tra người thực hiện hành động có phải là admin không
        if (!requester.getParticipantRole().equals(ParticipantRole.ADMIN)) return ParticipantValidationResult.NOT_ADMIN;

        Conversations conversation = requester.getConversation();
        // Kiểm tra cuộc trò chuyện có phải nhóm không
        if (!conversation.isGroupChat()) return ParticipantValidationResult.NOT_GROUP_CHAT;

        ConversationParticipants target = participantRepository.findByUserInConversation(participantId, conversationId);
        // Kiểm tra người bị xóa có trong nhóm không
        if (target == null) return ParticipantValidationResult.TARGET_NOT_FOUND;

        return ParticipantValidationResult.VALID;
    }

    @Override
    @Transactional
    public Map<String, Object> removeParticipantsFromConversation(Long conversationId, Long actorId, List<Long> participantIds) throws AccessDeniedException, IllegalStateException, EntityNotFoundException {
        ConversationParticipants requester = participantRepository.findByUserInConversation(actorId, conversationId);
        // 1. Kiểm tra người thực hiện hành động có trong cuộc trò chuyện không
        if (requester == null) {
            throw new AccessDeniedException("You are not a participant in this conversation!");
        }
        int numberOfParticipants = participantRepository.countUserInConversation(conversationId);
        // 2. Ánh xạ id -> kết quả validate
        Map<Long, ParticipantValidationResult> validationMap = participantIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> validateParticipant(id, requester, conversationId)
                ));

        // 3. Phân tách
        List<Long> removable = validationMap.entrySet().stream()
                .filter(entry -> entry.getValue() == ParticipantValidationResult.VALID)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Danh sách không xóa được và lý do
        Map<Long, ParticipantValidationResult> nonRemovable = validationMap.entrySet().stream()
                .filter(entry -> entry.getValue() != ParticipantValidationResult.VALID)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // 4. Soft-delete các người có thể xóa
        if (!removable.isEmpty()) {
            participantRepository.softRemoveParticipantInConversation(conversationId, removable);
        }

        // 5. Tạo tin nhắn hệ thống
        StringBuilder systemMessage = new StringBuilder(Utilities.buildFullName(requester.getUser()) + " removed ");
        if (!removable.isEmpty()) {
            // Lấy thông tin người bị xóa
            List<Users> removedUsers = userRepository.findAllById(removable);

            List<String> removedNames = Collections.singletonList(removedUsers.stream()
                    .map(Utilities::buildFullName)
                    .collect(Collectors.joining(", ")));
            if (removedUsers.size() < 3) {
                // Nếu dưới 3 người, liệt kê hết
                systemMessage.append(String.join(", ", removedNames));
            } else {
                // Nếu hơn 3 người, hiển thị 3 tên và số người còn lại
                List<String> displayed = removedNames.subList(0, 3);
                int others = removedNames.size() - 3;
                systemMessage.append(String.join(", ", displayed));
                systemMessage.append(" and ").append(others).append(" others");
            }

            // Tạo tin nhắn hệ thống
            systemMessage.append(" from the group!");
            // Tạo tin nhắn
            LocalDateTime now = LocalDateTime.now();
            Messages message = new Messages();
            message.setSendAt(now);
            message.setMessageContent(String.valueOf(systemMessage));
            message.setStatus(MessageStatus.DELIVERED); // Hoặc SENT nếu cần
            message.setSender(requester.getUser()); // Người thực hiện hành động
            message.setConversation(requester.getConversation());

            messageRepository.save(message);

            // Cập nhật LastMessageId
            Conversations conversation = requester.getConversation();
            conversation.setLastMessageId(message.getId());
            conversation.setEditedAt(now);
            conversationRepository.save(conversation);
            numberOfParticipants -= removable.size();
        }
        // 6. Nếu số lượng người tham gia còn lại < 3, xóa nhóm
        if (numberOfParticipants < 3) {
            // Xóa nhóm
            participantRepository.softRemoveAllParticipantsInConversation(conversationId);
        }

        // Thông báo qua WebSocket nếu cần
        // chatWebSocketService.notifyParticipantRemoved(conversationId, removedUserId);
        // 5. Trả về kết quả
        Map<String, Object> result = new HashMap<>();
        result.put("removed", removable);
        result.put("notRemoved", nonRemovable);

        if (numberOfParticipants < 3) {
            result.put("conversationNotify", "Because group members less than 3, group will be deleted!");
        } else {
            result.put("message", systemMessage);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorResponse<UserDTO> searchUsersToAddToConversation(Long conversationId, Long actorId, String keyword, LocalDateTime cursor, int size) throws AccessDeniedException, IllegalStateException {
        // ta bảng bạn bè trước
        return new CursorResponse<>(null, null, false);
    }

    @Override
    @Transactional
    public String deleteConversation(Long conversationId, Long userId) throws AccessDeniedException {
        // Kiểm tra quyền truy cập
        ConversationParticipants participant = participantRepository.findByUserInConversation(userId, conversationId);
        if (participant == null) {
            throw new AccessDeniedException("You are not a participant in this conversation");
        }
        if (participant.getParticipantRole() != ParticipantRole.ADMIN) {
            throw new AccessDeniedException("You are not an admin of this conversation!");
        }
        // Xóa cuộc trò chuyện
        Conversations conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));
        if (conversation.isGroupChat()) {
            participantRepository.softRemoveAllParticipantsInConversation(conversationId);
        }
        // Thông báo qua WebSocket nếu cần
        // chatWebSocketService.notifyConversationDeleted(conversationId, userId);

        return "Conversation deleted!";
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getParticipantsInConversation(Long conversationId, Long userId) throws AccessDeniedException {
        // Kiểm tra quyền truy cập
        ConversationParticipants requesterParticipant = participantRepository.findByUserInConversation(userId, conversationId);
        if (requesterParticipant == null) {
            throw new AccessDeniedException("You are not a participant in this conversation");
        }

        // Lấy danh sách người tham gia
        return ConversationHelper.getParticipantsForConversation(conversationId, participantRepository);
    }
}
