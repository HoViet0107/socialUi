import { ref } from "vue";
import { fetchFeedItemsRequest } from 'src/models/models';
import { createFeedItem, fetchFeedItemList, fetchUserDetails } from "./commonRequest";

let comments = ref([]);
const hasMoreReplies = ref({});

/**
 * Fetches comments or replies based on the specified parameters.
 * 
 * This function handles both initial loading and pagination for comments and replies.
 * It manages loading states, error handling, and data processing including user details.
 * 
 * @param {Object} body - Contains state variables like loading flags, pagination info, etc.
 * @param {string} itemType - Type of items to fetch ('comment' or 'reply').
 * @param {Object|null} comment - Parent comment object when fetching replies, null for comments.
 * @param {number} page - Page number to fetch (for pagination).
 * @param {boolean} isLoadMore - Whether this is a request to load more items.
 * @returns {Promise<Object>} Object containing updated body state and comments data.
 */
export async function fetchCommentsHelper(body, itemType, comment, page = 1, isLoadMore = false) {
    if (itemType === 'comment' && !isLoadMore) {
        body.loading = true;
    } else if (itemType === 'comment' && isLoadMore) {
        body.isLoadingMore = true;
    }

    body.error = null;
    try {
        const parentItemId = itemType === 'comment' ? null : comment.feedItemId;
        const requestBody = fetchFeedItemsRequest(itemType, page, body.pageSize, body.postId, parentItemId);

        const data = await fetchFeedItemList(requestBody);
        if (itemType === 'reply') {
            // Handle reply data for a specific comment
            if (!comment.replies) {
                comment.replies = [];
            }

            // Khởi tạo các biến theo dõi trạng thái nếu chưa có
            if (body.isLoadingMoreReplies[comment.feedItemId] === undefined) {
                body.isLoadingMoreReplies[comment.feedItemId] = false;
            }
            if (body.hasMoreReplies[comment.feedItemId] === undefined) {
                body.hasMoreReplies[comment.feedItemId] = true;
            }
            if (body.replyCurrentPage[comment.feedItemId] === undefined) {
                body.replyCurrentPage[comment.feedItemId] = 1;
            }

            // Cập nhật trạng thái loading
            if (isLoadMore) {
                body.isLoadingMoreReplies[comment.feedItemId] = true;
            } else {
                body.loadingReplies[comment.feedItemId] = true;
            }

            try {
                const requestBody = fetchFeedItemsRequest(itemType, page, body.replyPageSize, body.postId, comment.feedItemId);
                const data = await fetchFeedItemList(requestBody);

                if (Array.isArray(data) && data.length > 0) {
                    // Process user details for each reply
                    for (let reply of data) {
                        try {
                            reply.user = await fetchUserDetails(reply);
                        } catch (err) {
                            console.error(`Error fetching user details for reply ${reply.feedItemId}:`, err);
                            reply.user = { firstName: 'Unknown', lastName: 'User' };
                        }
                    }

                    // Nếu đang tải thêm, thêm vào danh sách hiện có
                    if (isLoadMore) {
                        // Kiểm tra trùng lặp
                        const existingIds = new Set(comment.replies.map(item => item.feedItemId));
                        const uniqueNewReplies = data.filter(reply => !existingIds.has(reply.feedItemId));

                        // Nếu không có reply mới, đánh dấu là không còn reply để tải
                        if (uniqueNewReplies.length === 0) {
                            hasMoreReplies.value[comment.feedItemId] = false;
                            console.log(`All replies for comment ${comment.feedItemId} already loaded`);
                        } else {
                            // Thêm replies mới vào danh sách
                            comment.replies = [...comment.replies, ...uniqueNewReplies];

                            // Cập nhật trang hiện tại
                            body.replyCurrentPage[comment.feedItemId] = page;
                        }
                    } else {
                        // Nếu đang tải lần đầu, thay thế danh sách hiện có
                        comment.replies = data;
                    }

                    // Kiểm tra xem có thêm replies để tải không
                    hasMoreReplies.value[comment.feedItemId] = data.length === body.replyPageSize;

                    // Update reply count
                    if (!comment.replyCount || comment.replyCount < comment.replies.length) {
                        comment.replyCount = comment.replies.length;
                    }
                } else {
                    if (page === 1) {
                        comment.replies = [];
                    }
                    body.hasMoreReplies[comment.feedItemId] = false;
                }
            } catch (err) {
                console.error(`Error fetching replies for comment ${comment.feedItemId}:`, err);
                if (page === 1) {
                    comment.replies = [];
                }
                body.hasMoreReplies[comment.feedItemId] = false;
            } finally {
                if (isLoadMore) {
                    body.isLoadingMoreReplies[comment.feedItemId] = false;
                } else {
                    body.loadingReplies[comment.feedItemId] = false;
                }
            }

            return;
        } else {
            // Handle comment data
            if (Array.isArray(data)) {
                // Kiểm tra nếu không có dữ liệu mới
                if (data.length === 0) {
                    body.hasMoreComments = false;
                    console.log("No more comments to load - received empty array");
                    return;
                }

                // Nếu đang tải thêm, thêm vào danh sách hiện có
                if (isLoadMore) {
                    // Kiểm tra trùng lặp
                    const existingIds = new Set(comments.value.map(item => item.feedItemId));
                    const uniqueNewComments = data.filter(comment => !existingIds.has(comment.feedItemId));

                    // Nếu không có comment mới, đánh dấu là không còn comment để tải
                    if (uniqueNewComments.length === 0) {
                        body.hasMoreComments = false;
                        console.log("All comments already loaded");
                        return;
                    }

                    // Xử lý thông tin người dùng cho mỗi comment mới
                    for (let comment of uniqueNewComments) {
                        try {
                            comment.user = await fetchUserDetails(comment);
                            if (!comment.replies) {
                                comment.replies = [];
                            }
                        } catch (err) {
                            console.error(`Error fetching user details for comment ${comment.feedItemId}:`, err);
                            comment.user = { firstName: 'Unknown', lastName: 'User' };
                        }
                    }

                    // Thêm comments mới vào danh sách
                    comments.value = [...comments.value, ...uniqueNewComments];
                } else {
                    // Nếu đang tải lần đầu, thay thế danh sách hiện có
                    comments.value = data;

                    // Xử lý thông tin người dùng cho mỗi comment
                    for (let comment of data) {
                        try {
                            comment.user = await fetchUserDetails(comment);
                            if (!comment.replies) {
                                comment.replies = [];
                            }
                        } catch (err) {
                            console.error(`Error fetching user details for comment ${comment.feedItemId}:`, err);
                            comment.user = { firstName: 'Unknown', lastName: 'User' };
                        }
                    }
                }

                // Kiểm tra xem có thêm comment để tải không
                body.hasMoreComments = data.length === body.pageSize;

                // Tăng trang hiện tại nếu đang tải thêm
                if (isLoadMore) {
                    body.currentPage = page;
                }
            } else if (!data) {
                // Handle the case where no comment is returned
                if (!isLoadMore) {
                    comments.value = [];
                }
                body.hasMoreComments = false;
            } else {
                console.error('Unexpected response format:', data);
                if (!isLoadMore) {
                    comments.value = [];
                    body.error = 'Received invalid data format from server';
                }
                body.hasMoreComments = false;
            }
        }
        return {
            body,
            comments: comments.value
        };
    } catch (err) {
        console.error('Error fetching comments:', err);
        body.error = 'Failed to load comments. Please try again.';
        if (!isLoadMore && itemType === 'comment') {
            comments.value = [];
        }
        body.hasMoreComments = false;
    } finally {
        if (itemType === 'comment' && !isLoadMore) {
            body.loading = false;
        } else if (itemType === 'comment' && isLoadMore) {
            body.isLoadingMore = false;
        }

        if (itemType === 'reply') {
            body.loadingReplies[comment.feedItemId] = false;
        }
    }
}

/**
 * Creates a new comment or reply and updates the UI accordingly.
 * 
 * This function handles:
 * - Creating new comments or replies via API
 * - Fetching user details for the new item
 * - Updating UI state (showing replies, resetting forms)
 * - Error handling
 * 
 * @param {Object} requestBody - Contains token, request data, and UI state variables.
 * @param {string} requestBody.token - Authentication token.
 * @param {Object} requestBody.requestData - Data for the new item (content, type, etc.).
 * @param {Array} requestBody.comments - Current comments array.
 * @param {Object} requestBody.showReplies - Object tracking which comments have visible replies.
 * @returns {Promise<Object>} Object containing updated data, comments, and UI state.
 */
export async function addNewFeedItem(requestBody) {
    try {
        if (!requestBody.token) {
            console.error('No authentication token found');
            return {
                error: 'No authentication token found',
                comments: requestBody.comments
            };
        }

        const data = await createFeedItem(requestBody.requestData, requestBody.token);

        // Add user detail into new comment/reply
        try {
            data.user = await fetchUserDetails(data) ?? { firstName: 'Unknown', lastName: 'User' };
        } catch (err) {
            console.error('Error fetching user details:', err);
            data.user = { firstName: 'Unknown', lastName: 'User' };
        }

        // Create a new array containing the new comment to return
        let newComments = [data];

        if (requestBody.requestData.itemType === 'COMMENT') {
            // Initialize showReplies for the new comment
            requestBody.showReplies[data.feedItemId] = false;
            console.log('New comment created:', data);
        } else if (requestBody.requestData.itemType === 'REPLY') {
            // Ensure replies are shown
            requestBody.showReplies[requestBody.requestData.parentFItem] = true;
            console.log('New reply created:', data);
        }

        // Reset form
        requestBody.newReply = '';
        requestBody.replyingTo = null;
        requestBody.error = null;

        return {
            data: data,
            body: requestBody,
            comments: newComments,
            showReplies: requestBody.showReplies
        };
    } catch (error) {
        console.error("Failed to create feed item:", error);
        return {
            error: 'Failed to create feed item',
            comments: requestBody.comments
        };
    }
}