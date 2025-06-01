package personal.social.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionDTO {
    private Long id;
    private String type;
    private Long userId;
    private String fullName;
}
