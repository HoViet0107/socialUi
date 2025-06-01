package personal.social.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import personal.social.model.MediaRequest;

import java.util.List;

@Repository
public interface MessageMediaRepository extends JpaRepository<MediaRequest, Long> {
    List<MediaRequest> findByMessageIdIn(List<Long> messageIds);

    List<MediaRequest> findByMessageId(Long messageId);
}