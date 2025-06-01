/**
 * Models for API requests
 * Centralizes data structures for better maintainability
 */

export const fetchFeedItemsRequest = (itemType, currentPage, pageSize, postId = null, parentItemId = null) => ({
    itemType: itemType,
    parentItemId: parentItemId,
    postId: postId,
    pageNumber: currentPage,
    pageSize: pageSize,
});
