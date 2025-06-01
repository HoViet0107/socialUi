package personal.social.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CursorResponse<T> {
    List<T> itemDTOList;
    LocalDateTime nextCursor;
    boolean hasMore;
}
