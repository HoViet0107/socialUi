package personal.social.model;

import jakarta.persistence.*;
import lombok.*;
import personal.social.enums.MessageStatus;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Messages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "send_at", nullable = false)
    private LocalDateTime sendAt;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    private boolean isDeleted;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_status")
    private MessageStatus status = MessageStatus.SENT;

    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;

    @Column(name = "is_read", nullable = false, columnDefinition = "boolean default false")
    private boolean isRead;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private Users sender;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL)
    private List<MediaRequest> media;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL)
    private List<MessageReaction> reactions;

    @ManyToOne
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversations conversation;
}
