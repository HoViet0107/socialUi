package personal.social.dto.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateConversationRequest {
    private Long creatorId;
    private String title;
    private String avatar;
    private boolean isGroupChat;
    private LocalDateTime createAt;
    private List<Long> participantIds;
}
