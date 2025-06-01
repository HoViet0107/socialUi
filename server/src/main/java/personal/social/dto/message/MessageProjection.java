package personal.social.dto.message;

import java.time.LocalDateTime;

/**
 * Projection interface for better type safety
 */
public interface MessageProjection {
    Long getId();
    Long getConversationId();
    LocalDateTime getSendAt();
    LocalDateTime getEditedAt();
    String getMessageContent();
    String getMessageStatus();
    Boolean getIsDeleted();
    Long getReplyToMessageId();
    Long getSenderId();
    String getFirstName();
    String getSurname();
    String getLastName();
    String getSenderAvatar();
}
