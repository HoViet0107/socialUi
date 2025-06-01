package personal.social.services.impl;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import personal.social.dto.FeedItemDTO;
import personal.social.dto.FeedItemRequest;
import personal.social.dto.ReactionInfoDTO;
import personal.social.dto.ReactionRequest;
import personal.social.enums.ObjectType;
import personal.social.enums.ReactionType;
import personal.social.enums.Status;
import personal.social.helper.CommonHelpers;
import personal.social.model.*;
import personal.social.repository.FeedItemContentRepository;
import personal.social.repository.FeedItemRepository;
import personal.social.repository.ReactionSummaryRepository;
import personal.social.repository.UserReactionRepository;
import personal.social.services.FeedItemService;

import java.nio.file.AccessDeniedException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the FeedItemService interface.
 * Handles operations related to feed items (posts, comments, replies) and user reactions.
 * <p>
 * This class manages the lifecycle of feed items, including creation, retrieval,
 * updating content and status, and managing user reactions such as likes, dislikes,
 * shares, and reports.
 */
@Service
@Slf4j
public class FeedItemServiceImpl implements FeedItemService {
    private final CommonHelpers helpers;
    private final FeedItemRepository feedItemRepo;
    private final FeedItemContentRepository fItemContentRepo;
    private final ReactionSummaryRepository summaryRepo;
    private final UserReactionRepository userReactionRepo;

    /**
     * Constructor for FeedItemServiceImpl with dependency injection.
     *
     * @param helpers          Common helper methods
     * @param feedItemRepo     Repository for feed item data access
     * @param fItemContentRepo Repository for feed item content
     * @param summaryRepo      Repository for reaction summaries
     * @param userReactionRepo Repository for user reactions
     */
    @Autowired
    public FeedItemServiceImpl(
            CommonHelpers helpers,
            FeedItemRepository feedItemRepo,
            FeedItemContentRepository fItemContentRepo,
            ReactionSummaryRepository summaryRepo,
            UserReactionRepository userReactionRepo) {
        this.helpers = helpers;
        this.feedItemRepo = feedItemRepo;
        this.fItemContentRepo = fItemContentRepo;
        this.summaryRepo = summaryRepo;
        this.userReactionRepo = userReactionRepo;
    }

    /**
     * Retrieves a paginated list of feed items filtered by item type and additional request criteria.
     * Supports retrieving posts, comments, and replies.
     *
     * @param pageNumber      the page number (starting at 1)
     * @param pageSize        number of items per page
     * @param fItemType       type of feed item (POST, COMMENT, REPLY)
     * @param feedItemRequest additional filtering parameters such as parent ID, post ID, etc.
     * @return a Page of FeedItemDTO representing the feed items
     * @throws Exception if retrieval fails or no items found
     */
    @Override
    public Page<FeedItemDTO> getAllFeedItem(Integer pageNumber, Integer pageSize, ObjectType fItemType, FeedItemRequest feedItemRequest) throws Exception {
        List<Object[]> results = switch (fItemType.name()) {
            case "POST" -> feedItemRepo.findAllFeedItems(pageNumber, pageSize);
            case "COMMENT", "REPLY" -> feedItemRepo.findAllCommentsByParentId(
                    fItemType.name(),
                    feedItemRequest.getPostId(),
                    feedItemRequest.getParentItemId(),
                    pageNumber,
                    pageSize
            );
            default -> {
                log.warn("Unknown feed item type: {}", fItemType);
                yield Collections.emptyList();
            }
        };

        // throw exception if results have no items
        if (results.isEmpty()) {
            throw new RuntimeException("Results is empty!");
        }

        // Convert result from Object[] to FeedItemDTO object
        List<FeedItemDTO> dtoList = results.stream().map(obj -> new FeedItemDTO(
                ((Number) obj[0]).longValue(),
                ((Timestamp) obj[1]).toLocalDateTime(), // created At
                ((Timestamp) obj[2]).toLocalDateTime(), // updated at
                Status.valueOf((String) obj[3]), // feed item status
                ObjectType.valueOf((String) obj[4]), //feed item type(POST, COMMENT, REPLY)
                (obj[5].toString()), // content
                ((Number) obj[6]).longValue(), // user id
                ((obj[7] != null) ? ((Number) obj[7]).longValue() : null), // parent feed item id
                ((Number) obj[8]).longValue(), // post id
                ((Number) obj[9]).longValue() // reply to
        )).collect(Collectors.toList());

        long total = feedItemRepo.countAllFeedItems(fItemType.name()); // call procedure count
        // Create a Page object to paginate the results
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        return new PageImpl<>(dtoList, pageable, total);
    }

    /**
     * Retrieves aggregated reaction information (like, dislike, share, report counts)
     * for a specific feed item, optionally considering the user's own reaction.
     *
     * @param user       the user requesting reaction info (nullable)
     * @param feedItemId the ID of the feed item
     * @return ReactionInfoDTO containing reaction counts and user reaction status
     */
    @Override
    public ReactionInfoDTO getFeedItemReactionInfo(Users user, Long feedItemId) {
        return feedItemRepo.findFeedItemReactionInfo(user.getId(), feedItemId);
    }

    /**
     * Creates a new feed item (post, comment, or reply) and initializes related content
     * and reaction summary.
     *
     * @param feedItemDTO the feed item data to create
     * @param user        the user creating the feed item
     * @return the created FeedItemDTO with updated metadata like ID and timestamps
     */
    @Override
    @Transactional
    public FeedItemDTO createFeedItem(FeedItemDTO feedItemDTO, Users user) {
        LocalDateTime currentTime = LocalDateTime.now();
        // Create new feed item object
        FeedItem newFeedItem = new FeedItem(
                currentTime,
                currentTime,
                Status.ACTIVE,
                feedItemDTO.getItemType(),
                user,
                feedItemDTO.getParentFItem(),
                feedItemDTO.getPostId(),
                feedItemDTO.getReplyTo()
        );
        // Save to database
        helpers.saveEntity(newFeedItem, feedItemRepo, "feedItem record");

        // create and save feed item content
        FeedItemContent fItemContent = new FeedItemContent(
                feedItemDTO.getContent(),
                currentTime,
                newFeedItem
        );
        helpers.saveEntity(fItemContent, fItemContentRepo, "feedItem content"); // Save to database

        // Create and save feed item reaction summary
        ReactionSummary reactionSummary = new ReactionSummary(
                feedItemDTO.getItemType(),
                newFeedItem,
                0L, 0L, 0L, 0L
        );
        helpers.saveEntity(reactionSummary, summaryRepo, "feedItem reaction summary"); // Save to database

        feedItemDTO.setFeedItemId(newFeedItem.getId());
        feedItemDTO.setCreatedAt(currentTime);
        feedItemDTO.setUpdatedAt(currentTime);
        feedItemDTO.setFstatus(newFeedItem.getFstatus());
        feedItemDTO.setUserId(user.getId());

        return feedItemDTO;
    }

    /**
     * Edits an existing feed item including content and status updates,
     * and processes user reactions like like, dislike, share, and report.
     *
     * @param feedItemDTO     the updated feed item data
     * @param reactionRequest the reaction update request (can be null or partial)
     * @param user            the user requesting the edit or reaction
     * @return confirmation message about the edit or reaction result
     * @throws AccessDeniedException if the user lacks permission to edit the feed item
     */
    @Override
    @Transactional
    public String editFeedItem(FeedItemDTO feedItemDTO, ReactionRequest reactionRequest, Users user) throws AccessDeniedException {
        // Find the feed item to edit
        FeedItem existedFeedItem = feedItemRepo.findByFeedItemId(feedItemDTO.getFeedItemId());

        // Check if status or content changed
        if (existedFeedItem != null) {
            boolean isFeedItemChanged = false;
            boolean isContentChanged = false;
            // check user permission
            if (!existedFeedItem.getUser().equals(user)) {
                throw new AccessDeniedException("You don't have permission to edit this feed item!");
            }

            // set new status if its changed
            if (!existedFeedItem.getFstatus().equals(feedItemDTO.getFstatus())) {
                existedFeedItem.setFstatus(feedItemDTO.getFstatus());
                isFeedItemChanged = true;
            }

            FeedItemContent existedFiContent = fItemContentRepo.findByFItemId(existedFeedItem.getId());
            // set new content if its changed
            if (
                    !existedFiContent.getContent().equals(feedItemDTO.getContent()) &&
                    feedItemDTO.getContent() != null &&
                    !feedItemDTO.getContent().isEmpty() &&
                    !feedItemDTO.getContent().isBlank()
            ) {
                existedFiContent.setContent(feedItemDTO.getContent());
                isContentChanged = true;
            }
            // Save changes to database
            if (isFeedItemChanged) {
                helpers.saveEntity(existedFeedItem, feedItemRepo, "feed item status");
            }
            if (isContentChanged) {
                helpers.saveEntity(existedFiContent, fItemContentRepo, "feed item content");
            }
        } else {
            throw new RuntimeException("FeedItem not existed!");
        }

        // Handle user responses (if any)
        if (reactionRequest.getItemType() == null ||
                reactionRequest.getReactionType() == null ||
                reactionRequest.getItemId() == null) {
            return "";
        }

        // Handling different types of reactions
        switch (reactionRequest.getReactionType()) {
            case LIKE -> handleLike(reactionRequest, user, existedFeedItem);
            case DISLIKE -> handleDislike(reactionRequest, user, existedFeedItem);
            case SHARE -> {
                handleShare(reactionRequest);
                return "Feed item shared!";
            }
            case REPORT -> {
                handleReport(reactionRequest);
                return "Feed item reported";
            }
            default ->
                    throw new IllegalArgumentException("Unsupported reaction type: " + reactionRequest.getReactionType());
        }
        return "Feed item content updated successfully!";
    }

    /**
     * Handle "LIKE" reaction for a feed item by delegating to the generic reaction handler.
     *
     * @param reactionRequest the reaction request containing item ID and reaction type
     * @param user            the user performing the reaction
     * @param existedFeedItem the existing feed item being reacted to
     */
    private void handleLike(ReactionRequest reactionRequest, Users user, FeedItem existedFeedItem) {
        handleReaction(reactionRequest, user, existedFeedItem, "like");
    }

    /**
     * Handle "DISLIKE" reaction for a feed item by delegating to the generic reaction handler.
     *
     * @param reactionRequest the reaction request containing item ID and reaction type
     * @param user            the user performing the reaction
     * @param existedFeedItem the existing feed item being reacted to
     */
    private void handleDislike(ReactionRequest reactionRequest, Users user, FeedItem existedFeedItem) {
        handleReaction(reactionRequest, user, existedFeedItem, "dislike");
    }

    /**
     * Handle "SHARE" reaction for a feed item.
     * (Implementation is pending - to be added as per business logic)
     *
     * @param reactionRequest the reaction request containing item ID and reaction type
     */
    private void handleShare(ReactionRequest reactionRequest) {
        // handle share (POST, COMMENT)
    }

    /**
     * Handle "REPORT" reaction for a feed item.
     * (Implementation is pending - to be added as per business logic)
     *
     * @param reactionRequest the reaction request containing item ID and reaction type
     */
    private void handleReport(ReactionRequest reactionRequest) {
        // handle report (POST, COMMENT)
    }

    /**
     * Generic method to handle user reactions on a feed item.
     * It manages creating new reactions, toggling (undo) reactions if already reacted,
     * or switching reaction types if user changes reaction.
     *
     * @param reactionRequest the reaction request containing item ID, reaction type, and item type
     * @param user            the user performing the reaction
     * @param existedFeedItem the existing feed item being reacted to
     * @param operation       a string describing the type of operation ("like", "dislike", etc.) for logging purposes
     */
    private void handleReaction(ReactionRequest reactionRequest, Users user, FeedItem existedFeedItem, String operation) {
        // Get current reaction summary information
        ReactionSummary existedReactionSum = summaryRepo.findByFeedItemId(reactionRequest.getItemId());
        // Check if the user has responded before
        UserReaction userReaction = userReactionRepo.findByFeedItemId(reactionRequest.getItemId(), reactionRequest.getReactionType());

        if (userReaction == null) {
            // Create a new response if the user has not responded yet
            userReaction = createNewUserReaction(
                    user, existedFeedItem,
                    reactionRequest.getReactionType(),
                    reactionRequest.getItemType()
            );
            // Increase the number of reactions in the summary table
            existedReactionSum = updateReactionCount(
                    existedReactionSum,
                    reactionRequest.getReactionType(),
                    true
            );
        } else if (userReaction.getReactionType().equals(reactionRequest.getReactionType())) {
            // User already reacted with this type, so undo the reaction
            existedReactionSum = updateReactionCount(
                    existedReactionSum,
                    reactionRequest.getReactionType(),
                    false
            );

            // Set the opposite reaction type and update reacted time to mark undo
            userReaction.setReactionType(
                    getOppositeReactionType(reactionRequest.getReactionType())
            );
            userReaction.setReactedAt(LocalDateTime.now());
        } else {
            // User had reacted with a different type, so switch to the new reaction type
            userReaction.setReactionType(reactionRequest.getReactionType());
            userReaction.setReactedAt(LocalDateTime.now());
            // Increment reaction count for the new reaction type
            existedReactionSum = updateReactionCount(existedReactionSum, reactionRequest.getReactionType(), true);
        }

        // Save updated user reaction and reaction summary to DB
        helpers.saveEntity(userReaction, userReactionRepo, "user " + operation + " reaction");
        helpers.saveEntity(existedReactionSum, summaryRepo, "feed item summary(" + operation + ")");
    }

    /**
     * Create a new UserReaction entity based on user, feed item, reaction type, and item type.
     *
     * @param user         the user performing the reaction
     * @param feedItem     the feed item being reacted to
     * @param reactionType the reaction type (LIKE, DISLIKE, etc.)
     * @param itemType     the object type of the feed item (POST, COMMENT, etc.)
     * @return the new UserReaction entity
     */
    private UserReaction createNewUserReaction(Users user, FeedItem feedItem, ReactionType reactionType, ObjectType itemType) {
        UserReaction reaction = new UserReaction();
        reaction.setReactedAt(LocalDateTime.now());
        reaction.setFeedItem(feedItem);
        reaction.setReactionType(reactionType);
        reaction.setObjectType(itemType);
        reaction.setUser(user);
        return reaction;
    }

    /**
     * Update the reaction counts in the ReactionSummary.
     * It increments or decrements the count for the given reaction type.
     *
     * @param existedReactionSum the existing ReactionSummary entity
     * @param reactionType       the reaction type to update
     * @param increment          true to increment the count, false to decrement
     * @return the updated ReactionSummary entity
     */
    private ReactionSummary updateReactionCount(ReactionSummary existedReactionSum, ReactionType reactionType, boolean increment) {
        long delta = increment ? 1 : -1;// if increment is true -> 1 else -> -1
        // Update the corresponding response number, ensuring it is not negative.
        switch (reactionType) {
            case LIKE -> existedReactionSum.setLikes(Math.max(0, existedReactionSum.getLikes() + delta));
            case DISLIKE -> existedReactionSum.setDislikes(Math.max(0, existedReactionSum.getDislikes() + delta));
            case SHARE -> existedReactionSum.setShares(Math.max(0, existedReactionSum.getShares() + delta));
            case REPORT -> existedReactionSum.setReports(Math.max(0, existedReactionSum.getReports() + delta));
        }
        return existedReactionSum;
    }

    /**
     * Return the opposite reaction type to represent undo operations.
     *
     * @param type the original reaction type
     * @return the opposite (undo) reaction type
     * @throws IllegalArgumentException if no opposite reaction type is defined
     */
    private ReactionType getOppositeReactionType(ReactionType type) {
        return switch (type) {
            case LIKE -> ReactionType.UNLIKE;
            case DISLIKE -> ReactionType.UNDISLIKE;
            case SHARE -> ReactionType.UNSHARE;
            case REPORT -> ReactionType.UNREPORT;
            default -> throw new IllegalArgumentException("No opposite defined for: " + type);
        };
    }
}
