package personal.social.services.impl;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import personal.social.dto.message.ChatMessageDTO;
import personal.social.repository.MessageReadStatusRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class MessageAsyncService {
    /**
     * Async mark messages as read
     */
    @Async("messageExecutor")
    public CompletableFuture<Void> markMessagesAsReadAsync(
            MessageReadStatusRepository readStatusRepository,
            Long userId,
            List<ChatMessageDTO> messages) {

        Long[] messageIds = messages.stream()
                .filter(msg -> !msg.isRead() && !msg.getSenderId().equals(userId))
                .map(ChatMessageDTO::getId)
                .toArray(Long[]::new);

        if (messageIds.length > 0) {
            readStatusRepository.bulkUpsertReadStatus(messageIds, userId);

            // Optional: WebSocket notification
            // notifyMessageReadViaWebSocket(userId, Arrays.asList(messageIds));
        }

        return CompletableFuture.completedFuture(null);
    }
}
