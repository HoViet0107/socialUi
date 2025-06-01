package personal.social.model;

import jakarta.persistence.*;
import lombok.*;
import personal.social.enums.MediaType;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MediaRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "media_url", nullable = false, columnDefinition = "TEXT")
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Column(name = "thumbnail_url", nullable = false, columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "duration") // Cho audio/video
    private Integer duration;

    @Column(name = "width") // Cho ảnh/video
    private Integer width;

    @Column(name = "height") // Cho ảnh/video
    private Integer height;

    @ManyToOne
    @JoinColumn(name = "message_id", nullable = false)
    private Messages message;
}
