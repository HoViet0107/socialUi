package personal.social.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long userId;
    private String email;
    private String fullName;
    private String avatarUrl;
    private boolean isOnline;
    private String participantRole;
    private LocalDateTime joinedAt;
}