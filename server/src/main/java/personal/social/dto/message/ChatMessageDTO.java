package personal.social.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import personal.social.dto.MediaDTO;
import personal.social.enums.MessageStatus;
import personal.social.enums.MessageType;
import personal.social.helper.MessageHelper;
import personal.social.helper.Utilities;
import personal.social.model.MediaRequest;
import personal.social.model.MessageReaction;
import personal.social.model.Messages;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDTO {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private LocalDateTime sendAt;
    private LocalDateTime editedAt;
    private boolean isRead;
    private MessageStatus status;
    private MessageType type;
    private List<MediaDTO> medias;
    private List<ReactionDTO> reactions;
    private Long replyToMessageId;
    private ChatMessageDTO replyMessage; // Lồng tin nhắn được reply
    private boolean isDeleted;
}