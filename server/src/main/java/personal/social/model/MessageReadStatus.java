package personal.social.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "message_read_status")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageReadStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @ManyToOne
    @JoinColumn(name = "message_id")
    private Messages message;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;
}
