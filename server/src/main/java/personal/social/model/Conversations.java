package personal.social.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Conversations {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String title;

    @Column(name = "avatar_url", columnDefinition = "TEXT", nullable = false)
    private String avatarUrl;

    @Column(name = "is_group_chat", nullable = false, columnDefinition = "boolean default false")
    private boolean isGroupChat;

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createAt;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "last_message_id")
    private Long lastMessageId;;
}
