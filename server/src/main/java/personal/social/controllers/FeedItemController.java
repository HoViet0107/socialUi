package personal.social.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import personal.social.dto.EditFeedItemRequest;
import personal.social.dto.FeedItemDTO;
import personal.social.dto.FeedItemRequest;
import personal.social.enums.ObjectType;
import personal.social.helper.CommonHelpers;
import personal.social.model.Users;
import personal.social.services.FeedItemService;

import java.nio.file.AccessDeniedException;
import java.util.Objects;

/**
 * Controller for handling feed items (posts and comments)
 * Provides endpoints for creating, retrieving, and editing feed items
 * Replaces separate PostController and CommentController with a unified approach
 */
@RestController
@RequestMapping("/api/v1/feed-items")
public class FeedItemController {
    private final FeedItemService feedItemService;
    private final PagedResourcesAssembler<FeedItemDTO> pagedResourcesAssembler;
    private final CommonHelpers helpers;

    @Autowired
    public FeedItemController(FeedItemService feedItemService, PagedResourcesAssembler<FeedItemDTO> pagedResourcesAssembler, CommonHelpers helpers) {
        this.feedItemService = feedItemService;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.helpers = helpers;
    }

    /**
     * Retrieves all posts with pagination
     *
     * @return PagedModel containing posts
     * @throws Exception If an error occurs during retrieval
     * @Param feedItemType The feed item type(post/comment/reply)
     */
    @PostMapping("/fetch")
    public PagedModel<?> getAllFeedItems(
                        @RequestBody FeedItemRequest feedItemRequest) throws Exception {
        // use PagedModel for REST-ful API
        Page<FeedItemDTO> feedItems = switch (feedItemRequest.getItemType()) {
            case "post" -> feedItemService.getAllFeedItem(feedItemRequest.getPageNumber(), feedItemRequest.getPageSize(), ObjectType.POST, feedItemRequest);
            case "comment" -> feedItemService.getAllFeedItem(feedItemRequest.getPageNumber(), feedItemRequest.getPageSize(), ObjectType.COMMENT, feedItemRequest);
            case "reply" -> feedItemService.getAllFeedItem(feedItemRequest.getPageNumber(), feedItemRequest.getPageSize(), ObjectType.REPLY, feedItemRequest);
            default -> throw new IllegalArgumentException("Invalid feed-item-type: " + feedItemRequest.getItemType());
        };

        return pagedResourcesAssembler.toModel(feedItems);
    }

    /**
     * Retrieves reaction information for a feed item
     *
     * @param fItemId The feed item ID
     * @return ResponseEntity containing reaction information
     */
    @GetMapping("/detail/{fItemId}")
    public ResponseEntity<?> getFeedItemReactions(@PathVariable Long fItemId, HttpServletRequest request) {
        Users user = helpers.extractToken(request);
        return ResponseEntity.ok().body(feedItemService.getFeedItemReactionInfo(user, fItemId));
    }

    /**
     * Creates a new feed item (post or comment)
     *
     * @param request     The HTTP request containing authentication
     * @param feedItemDTO The feed item data
     * @return ResponseEntity containing the created feed item
     */
    @PostMapping
    public ResponseEntity<?> createFeedItems(HttpServletRequest request, @RequestBody FeedItemDTO feedItemDTO) {
        Users user = helpers.extractToken(request);
        return ResponseEntity.ok().body(feedItemService.createFeedItem(feedItemDTO, user));
    }

    /**
     * Edits an existing feed item
     *
     * @param request             The HTTP request containing authentication
     * @param editFeedItemRequest The edit request containing feed item data and reaction
     * @return ResponseEntity containing the updated feed item
     * @throws AccessDeniedException If the user doesn't have permission to edit
     */
    @PutMapping
    public ResponseEntity<?> editFeedItem(HttpServletRequest request, @RequestBody EditFeedItemRequest editFeedItemRequest) throws AccessDeniedException {
        Users user = helpers.extractToken(request);
        return ResponseEntity.ok().body(feedItemService.editFeedItem(editFeedItemRequest.getFeedItemDTO(), editFeedItemRequest.getReactionRequest(), user));
    }
}
