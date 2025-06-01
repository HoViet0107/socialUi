package personal.social.dto.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import personal.social.dto.UserDTO;
import personal.social.dto.message.ChatMessageDTO;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationRequest {
    private Long id;
    private String title;
    private String avatar;
    private boolean isGroupChat;
    private LocalDateTime createAt;
    private ChatMessageDTO lastMessage;
    private int unreadCount;
    private List<UserDTO> participants;
    private boolean isTyping; // is anyone is typing
    private String typingUserName; // who is typing
}