package personal.social.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import personal.social.model.MessageReaction;

import java.util.List;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {
    List<MessageReaction> findByMessageIdIn(List<Long> messageIds);

    List<MessageReaction> findByMessageId(Long messageId);

    MessageReaction findByUserIdAndMessageId(Long userId, Long messageId);
}
