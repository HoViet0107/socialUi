package personal.social.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import personal.social.enums.MediaType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaDTO {
    private Long id;
    private String url;
    private String thumbnailUrl;
    private MediaType mediaType;
    private Long fileSize;
    private Integer duration;
    private Integer width;
    private Integer height;
}