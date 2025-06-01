package personal.social.model;

import jakarta.persistence.*;
import lombok.*;
import personal.social.enums.ParticipantRole;
import personal.social.model.id.ConversationParticipantsId;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "conversation_participants")
public class ConversationParticipants {

    @EmbeddedId
    private ConversationParticipantsId id;

    @ManyToOne
    @MapsId("conversationId") // Tên trùng với field trong ConversationParticipantsId
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversations conversation;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "is_online", nullable = false, columnDefinition = "boolean default false")
    private boolean isOnline;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_role", nullable = false, length = 20, columnDefinition = "varchar(20) default 'PARTICIPANT'")
    private ParticipantRole participantRole = ParticipantRole.PARTICIPANT;

    public ConversationParticipants(Conversations conversation, Users user) {
        this.id = new ConversationParticipantsId(conversation.getId(), user.getId());
        this.conversation = conversation;
        this.user = user;
        this.joinedAt = LocalDateTime.now();
    }
}
