<script setup>
import ActionMenu from 'src/components/ActionMenu.vue';
import { ref, onMounted, computed, onUnmounted } from 'vue';
import EditPostDialog from 'src/components/editor/EditPostDialog.vue';
import { editFeedItem } from 'src/helpers/commonRequest';
import { getAuthToken } from 'src/helpers/helperFunctions';
import { addNewFeedItem, fetchCommentsHelper } from 'src/helpers/commentHelperFunctions';

// Define props
const props = defineProps({
    feedItemId: {
        type: Number,
        required: true
    },
    user: {
        type: Object,
        required: true
    }
});

// State management - grouped by purpose
// Loading data state
const loading = ref(false);                     // Download main comment list
const error = ref(null);                        // Error loading comment
const loadingReplies = ref({});                 // Track loading state for each comment's replies

// Comment state
const comments = ref([]);                       // Comment list
const showReplies = ref({});                    // Show/hide reply status for each comment

// Send comment/reply
const replyingTo = ref(null);                   // feedItemId(of comment or reply) is being replied
const newReply = ref({});                       // key = feedItemId, value = reply content

// Edit content
const isEditDialogOpen = ref(false);            // Edit dialog open state
const editData = ref(null);

// Computed properties
const hasComments = computed(() => comments.value.length > 0);

// Initialize comment loading state
const isLoadingMore = ref(false);
const hasMoreComments = ref(true);
const currentPage = ref(1);
const pageSize = ref(5); // Number of posts per page

// Thêm các biến để quản lý việc tải thêm replies
const isLoadingMoreReplies = ref({});
const hasMoreReplies = ref({});
const replyCurrentPage = ref({});
const replyPageSize = ref(2); // Mỗi lần chỉ tải 2 replies

const token = getAuthToken();

/**
 * Fetches comments or replies based on the specified parameters.
 * 
 * @param {string} itemType - Type of items to fetch ('comment' or 'reply').
 * @param {Object|null} comment - Parent comment when fetching replies, null for comments.
 * @param {number} page - Page number for pagination.
 * @param {boolean} isLoadMore - Whether this is a request to load more items.
 */
const fetchComments = async (itemType, comment, page = 1, isLoadMore = false) => {
    try {
        let body = {
            loading: loading.value,
            isLoadingMore: isLoadingMore.value,
            isLoadMore: isLoadMore,
            hasMoreComments: hasMoreComments.value,
            postId: props.feedItemId,
            pageSize: pageSize.value,
            currentPage: currentPage.value,
            isLoadingMoreReplies: isLoadingMoreReplies.value,
            hasMoreReplies: hasMoreReplies.value,
            replyCurrentPage: replyCurrentPage.value,
            loadingReplies: loadingReplies.value,
            replyPageSize: replyPageSize.value,
            error: error.value,
        }
        let data = await fetchCommentsHelper(body, itemType, comment, page, isLoadMore);
        if (!comments.value) {
            comments.value = data.comments;
        } else {
            comments.value = [...comments.value, ...data.comments];
        }
        body = data.body;
        // comment
        loading.value = body.loading;
        isLoadingMore.value = body.isLoadingMore;
        hasMoreComments.value = body.hasMoreComments;
        pageSize.value = body.pageSize;
        currentPage.value = body.currentPage;

        // reply
        loadingReplies.value = body.loadingReplies;
        isLoadingMoreReplies.value = body.isLoadingMoreReplies;
        hasMoreReplies.value = body.hasMoreReplies;
        replyCurrentPage.value = body.replyCurrentPage;
        replyPageSize.value = body.replyPageSize;

        // Chỉ cập nhật lỗi nếu có lỗi từ server
        if (body.error) {
            error.value = body.error;
        } else {
            // Xóa lỗi nếu tải thành công
            error.value = null;
        }
    } catch (err) {
        console.error(`Error fetching ${itemType}:`, err);
        // error.value = `Failed to load ${itemType}. Please try again.`;
    }
};

/**
 * Loads more replies for a specific comment.
 * Handles pagination and loading states for replies.
 * 
 * @param {Object} comment - The parent comment to load more replies for.
 */
const loadMoreReplies = async (comment) => {
    const commentId = comment.feedItemId;
    if (isLoadingMoreReplies.value[commentId] || !hasMoreReplies.value[commentId]) {
        return;
    }
    const nextPage = replyCurrentPage.value[commentId] + 1;
    try {
        await fetchComments('reply', comment, nextPage, true);
    } catch (error) {
        console.error(`Error loading more replies for comment ${commentId}:`, error);
    }
};

/**
 * Loads more comments for the current post.
 * Handles pagination and loading states for comments.
 */
const loadMoreComments = async () => {
    if (isLoadingMore.value || !hasMoreComments.value) return;

    try {
        await fetchComments('comment', null, currentPage.value + 1, true);
    } catch (error) {
        console.error('Error in loadMoreComments:', error);
    }
};

/**
 * Toggles the reply form visibility for a specific item.
 * 
 * @param {number|string} itemId - ID of the item (comment/reply) to toggle reply form for.
 */
const toggleReplyForm = (itemId) => {
    console.log('Toggling reply form for item:', itemId);

    replyingTo.value = replyingTo.value === itemId ? null : itemId;
    // Khởi tạo giá trị rỗng nếu chưa tồn tại
    if (!newReply.value[itemId]) {
        newReply.value[itemId] = '';
    }
};

/**
 * Toggles the visibility of replies for a specific comment.
 * Loads replies if they haven't been loaded yet.
 * 
 * @param {string} itemType - Type of the item ('reply').
 * @param {Object} comment - The comment to toggle replies for.
 */
const toggleReplies = async (itemType, comment) => {
    // Initialize values if they don't exist
    if (showReplies.value[comment.feedItemId] === undefined) {
        showReplies.value[comment.feedItemId] = false;
    }
    if (loadingReplies.value[comment.feedItemId] === undefined) {
        loadingReplies.value[comment.feedItemId] = false;
    }
    if (replyCurrentPage.value[comment.feedItemId] === undefined) {
        replyCurrentPage.value[comment.feedItemId] = 1;
    }
    if (hasMoreReplies.value[comment.feedItemId] === undefined) {
        hasMoreReplies.value[comment.feedItemId] = true;
    }

    // Toggle the visibility state
    showReplies.value[comment.feedItemId] = !showReplies.value[comment.feedItemId];

    // ensure comment.replies is a array
    if (!comment.replies) {
        comment.replies = [];
    }

    // Only fetch if showing and not already loaded
    if (showReplies.value[comment.feedItemId] && comment.replies.length === 0) {
        replyCurrentPage.value[comment.feedItemId] = 1; // Reset page when loading first time
        await fetchComments('reply', comment, 1, false);
    }
};

/**
 * Creates a new comment or reply.
 * 
 * @param {number|string} parentItemId - ID of the parent item.
 * @param {string} itemType - Type of item to create ('COMMENT' or 'REPLY').
 * @param {string} content - Content of the new item.
 * @param {number|string|null} parentCommentId - ID of the parent comment (for replies).
 */
const createNewFeedItem = async (parentItemId, itemType, content, parentCommentId = null) => {
    try {
        const requestData = {
            content: content,
            itemType: itemType,
            parentFItem: parentCommentId,
            postId: props.feedItemId,
            replyTo: parentItemId,
        };

        const requestBody = {
            token: token,
            requestData: requestData,
            comments: comments.value,
            showReplies: showReplies.value,
            postId: props.feedItemId,
            newReply: newReply.value,
            replyingTo: replyingTo.value,
            error: error.value
        };

        const response = await addNewFeedItem(requestBody);

        if (response.error) {
            error.value = response.error;
            return;
        }

        // Fetch new comment/reply from response
        const newItem = response.comments && response.comments.length > 0 ? response.comments[0] : null;

        if (!newItem) {
            console.error('No new item returned from server');
            return;
        }

        // Update Ui based on itemType
        if (itemType === 'COMMENT') {
            // Add new comment to the beginning of the list
            comments.value = [newItem, ...comments.value];
            console.log('Updated comments list:', comments.value);
        } else if (itemType === 'REPLY') {
            // Find parent comment
            const parentCommentIndex = comments.value.findIndex(c => c.feedItemId === parentCommentId);

            if (parentCommentIndex !== -1) {
                // Ensure replies are an array
                if (!comments.value[parentCommentIndex].replies) {
                    comments.value[parentCommentIndex].replies = [];
                }

                // Add new reply to the parent comment's replies
                comments.value[parentCommentIndex].replies = [
                    newItem,
                    ...comments.value[parentCommentIndex].replies
                ];

                // Update replyCount
                if (!comments.value[parentCommentIndex].replyCount) {
                    comments.value[parentCommentIndex].replyCount = 1;
                } else {
                    comments.value[parentCommentIndex].replyCount++;
                }

                // Make sure to show replies
                showReplies.value[parentCommentId] = true;

                console.log('Updated replies for comment:', comments.value[parentCommentIndex]);
            } else {
                console.error('Parent comment not found:', parentCommentId);
            }
        }

        // Update showReplies if needed
        if (response.showReplies) {
            showReplies.value = response.showReplies;
        }

        // Reset form
        if (itemType === 'COMMENT') {
            newReply.value['main'] = '';
        } else if (itemType === 'REPLY') {
            newReply.value[parentItemId] = '';
        }
        replyingTo.value = null;
        error.value = null;
    } catch (err) {
        console.error('Error adding comment/reply:', err);
        error.value = 'Failed to add comment. Please try again.';
    }
};

/**
 * Handles creating a new top-level comment.
 * 
 * @param {number|string} parentItemId - ID of the parent post.
 */
const handleCreateComment = async (parentItemId) => {
    const content = newReply.value['main'] || '';
    if (!content.trim()) {
        error.value = 'Comment cannot be empty';
        return;
    }
    await createNewFeedItem(parentItemId, 'COMMENT', content);
};

/**
 * Handles creating a new reply to a comment or another reply.
 * 
 * @param {number|string} parentItemId - ID of the parent item (comment/reply).
 * @param {number|string} parentCommentId - ID of the parent comment.
 */
const handleCreateReply = async (parentItemId, parentCommentId) => {
    const content = newReply.value[parentItemId] || '';
    if (!content.trim()) {
        error.value = 'Comment cannot be empty';
        return;
    }
    await createNewFeedItem(parentItemId, 'REPLY', content, parentCommentId);
};

/**
 * Handles opening the edit dialog for a comment or reply.
 * 
 * @param {Object} param0 - Object containing the item and its type.
 * @param {Object} param0.item - The item to edit.
 * @param {string} param0.type - Type of the item ('comment' or 'reply').
 */
const handleContentEdit = ({ item, type }) => {
    editData.value = { ...item, type };
    isEditDialogOpen.value = true;
};

/**
 * Edits the content of a feed item (comment or reply).
 * Updates both the API and local state.
 * 
 * @param {Object} updatedData - The updated item data.
 */
const editFeedItemContent = async (updatedData) => {
    const requestBody = {
        feedItemDTO: {
            feedItemId: updatedData.feedItemId,
            itemType: updatedData.itemType,
            fstatus: "ACTIVE",
            parentFItem: updatedData.parentFItem,
            postId: updatedData.postId,
            content: updatedData.content,
        },
        reactionRequest: {}
    };
    const response = await editFeedItem(requestBody, token);

    const commentIndex = comments.value.findIndex(comment => comment.feedItemId === updatedData.parentFItem);
    if (commentIndex !== -1 && updatedData.itemType === 'COMMENT') {
        // Update the content in the local feedItems array
        comments.value[commentIndex].content = updatedData.content;
    } else {
        // Update the content in the replies array
        const replyIndex = comments.value[commentIndex].replies.findIndex(reply => reply.feedItemId === updatedData.feedItemId);
        if (replyIndex !== -1) {
            comments.value[commentIndex].replies[replyIndex].content = updatedData.content;
        }
    }
    console.log(`Comment (${commentIndex}) edited successfully:`, response.data);
};

/**
 * Handles deleting a comment or reply.
 * Updates both the API and local state.
 * 
 * @param {Object} deletedItem - The item to delete.
 */
const handleDelete = async (deletedItem) => {
    try {
        const requestBody = {
            feedItemDTO: {
                feedItemId: deletedItem.feedItemId,
                itemType: deletedItem.itemType,
                fstatus: "DELETE",
                content: deletedItem.content,
            },
            reactionRequest: {}
        };
        const response = await editFeedItem(requestBody, token);
        console.log('Delete response:', response.data);
        const itemIndex = comments.value.findIndex(item => item.feedItemId === deletedItem.feedItemId);
        if (itemIndex !== -1 && deletedItem.itemType === 'COMMENT') {
            // Update the content in the local feedItems array
            comments.value.splice(itemIndex, 1);
        } else {
            // Update the content in the replies array
            const replyIndex = comments.value[itemIndex].replies.findIndex(reply => reply.feedItemId === deletedItem.feedItemId);
            if (replyIndex !== -1) {
                comments.value[itemIndex].replies.splice(replyIndex, 1);
            }
        }
    } catch (error) {
        console.error('Error deleting item:', error);
    }
};

/**
 * Handles reporting a comment or reply
 */
const handleReport = (item) => {
    console.log('Report item:', item);
};

onMounted(() => {
    currentPage.value = 1; // Reset to page 1 when component mounts
    fetchComments('comment', null, 1, false);
});

onUnmounted(() => {
    currentPage.value = 1; // Reset to page 1 when component unmounts
});
</script>

<template>
    <div class="comment-section">
        <!-- Error display -->
        <div v-if="error" class="text-negative q-pa-sm bg-red-1 rounded-borders">
            <q-icon name="error" color="negative" size="sm" class="q-mr-xs" />
            {{ error }}
        </div>

        <!-- Loading state -->
        <div v-if="loading" class="text-center q-pa-md">
            <q-spinner color="primary" size="2em" />
            <div class="text-primary q-mt-sm">Loading comments...</div>
        </div>

        <!-- Add new comment -->
        <div v-if="!loading" class="q-pa-md">
            <q-input v-model="newReply['main']" label="Add a comment..." dense outlined class="q-mb-md"
                :error="!!error && !(newReply['main'] || '').trim()">
                <template v-slot:append>
                    <q-btn icon="send" dense flat @click="handleCreateComment(props.feedItemId)" />
                </template>
            </q-input>
        </div>

        <!-- Empty state -->
        <div v-if="!loading && !hasComments" class="text-center q-pa-md empty-state">
            <q-icon name="chat_bubble_outline" size="3em" color="grey-5" />
            <div class="q-mt-sm text-grey-8">No comments yet. Be the first to comment!</div>
        </div>

        <!-- Comments list -->
        <div v-for="comment in comments" :key="comment.feedItemId" class="q-mb-md comment-item">
            <q-card flat bordered class="q-pa-sm comment-card">
                <q-card-section class="relative">
                    <ActionMenu :item="{ ...comment, type: 'comment' }" @edit="handleContentEdit" @remove="handleDelete"
                        @report="handleReport" />
                    <!-- user tag -->
                    <div class="row items-center q-mb-xs">
                        <q-avatar size="24px" class="q-mr-sm">
                            <img src="https://cdn.quasar.dev/img/avatar.png" />
                        </q-avatar>
                        <template v-if="comment.user">
                            <span class="text-subtitle2 text-weight-bold">{{ comment.user.firstName }} {{
                                comment.user.lastName }}</span>
                        </template>
                        <template v-else>
                            <span class="text-subtitle2 text-weight-bold">{{ comment.userId }}</span>
                        </template>
                    </div>
                    <!-- content tag -->
                    <div class="text-body2 q-py-sm">{{ comment.content }}</div>
                </q-card-section>

                <!-- Reply actions -->
                <q-card-actions>
                    <q-btn flat dense size="sm" icon="reply" @click="toggleReplyForm(comment.feedItemId)">
                        {{ replyingTo === comment.feedItemId ? 'Cancel' : 'Reply' }}
                    </q-btn>
                    <q-btn flat dense size="sm" icon="forum" @click="toggleReplies('reply', comment)">
                        {{ showReplies[comment.feedItemId] ? 'Hide Replies' : 'Show Replies' }}
                        <q-badge v-if="comment.replyCount && comment.replyCount > 0" color="primary" floating>{{
                            comment.replyCount }}</q-badge>
                    </q-btn>
                </q-card-actions>

                <!-- Reply form -->
                <q-input v-if="replyingTo === comment.feedItemId" v-model="newReply[comment.feedItemId]"
                    label="Reply..." dense outlined class="q-mt-sm q-px-md"
                    :error="!!error && !(newReply[comment.feedItemId] || '').trim()">
                    <template v-slot:append>
                        <q-btn icon="send" dense flat
                            @click="handleCreateReply(comment.feedItemId, comment.feedItemId)" />
                    </template>
                </q-input>

                <!-- Replies list -->
                <div v-if="showReplies[comment.feedItemId] && comment.replies && comment.replies.length > 0"
                    class="q-ml-lg q-mt-md">
                    <div v-for="reply in comment.replies" :key="reply.feedItemId" class="q-mb-sm">
                        <q-card flat bordered class="reply-card q-pa-sm bg-grey-1">
                            <q-card-section class="relative q-py-xs">
                                <ActionMenu :item="{ ...reply, type: 'reply', parentCommentId: comment.feedItemId }"
                                    @edit="handleContentEdit" @remove="handleDelete" @report="handleReport" />

                                <div class="row items-center q-mb-xs">
                                    <q-avatar size="20px" class="q-mr-sm">
                                        <img src="https://cdn.quasar.dev/img/avatar2.jpg" />
                                    </q-avatar>
                                    <span class="text-caption text-weight-bold">{{ reply.userId }}</span>
                                    <q-icon name="arrow_right" size="xs" class="q-mx-xs" />
                                    <span class="text-caption text-grey">replying to</span>
                                    <q-icon name="arrow_right" size="xs" class="q-mx-xs" />
                                    <span class="text-caption text-weight-bold">{{ comment.userId }}</span>
                                </div>

                                <div class="text-body2 q-py-xs">{{ reply.content }}</div>
                            </q-card-section>
                            <!-- Reply actions -->
                            <q-card-actions>
                                <q-btn flat dense size="sm" icon="reply" @click="toggleReplyForm(reply.feedItemId)">
                                    {{ replyingTo === reply.feedItemId ? 'Cancel' : 'Reply' }}
                                </q-btn>
                                <!-- Reply form for replies -->
                                <q-input v-if="replyingTo === reply.feedItemId" v-model="newReply[reply.feedItemId]"
                                    label="Reply..." dense outlined class="q-mt-sm q-px-md"
                                    :error="!!error && !(newReply[reply.feedItemId] || '').trim()">
                                    <template v-slot:append>
                                        <q-btn icon="send" dense flat
                                            @click="handleCreateReply(reply.feedItemId, comment.feedItemId)" />
                                    </template>
                                </q-input>
                            </q-card-actions>
                        </q-card>
                    </div>

                    <!-- Thêm nút "Load More Replies" -->
                    <div v-if="hasMoreReplies[comment.feedItemId] && !isLoadingMoreReplies[comment.feedItemId]"
                        class="text-center q-pa-sm">
                        <q-btn flat dense size="sm" color="primary" label="Load More Replies"
                            @click="loadMoreReplies(comment)" :disable="isLoadingMoreReplies[comment.feedItemId]">
                            <template v-slot:loading>
                                <q-spinner-dots color="primary" size="1em" />
                            </template>
                        </q-btn>
                    </div>

                    <!-- Loading indicator for more replies -->
                    <div v-if="isLoadingMoreReplies[comment.feedItemId]" class="text-center q-pa-sm">
                        <q-spinner color="primary" size="1.5em" />
                        <span class="q-ml-sm">Loading more replies...</span>
                    </div>

                    <!-- Loading indicator for replies -->
                    <div v-if="loadingReplies[comment.feedItemId]" class="text-center q-pa-sm">
                        <q-spinner color="primary" size="1.5em" />
                        <span class="q-ml-sm">Loading replies...</span>
                    </div>

                    <!-- No replies message -->
                    <div v-if="showReplies[comment.feedItemId] && (!comment.replies || comment.replies.length === 0) && !loadingReplies[comment.feedItemId]"
                        class="text-center text-grey q-pa-sm">
                        No replies yet.
                    </div>
                </div>
            </q-card>
        </div>

        <div v-if="!loading && hasMoreComments" class="text-center q-pa-md">
            <q-btn :loading="isLoadingMore" color="primary" label="Load More Comments" @click="loadMoreComments"
                :disable="isLoadingMore || !hasMoreComments">
                <template v-slot:loading>
                    <q-spinner-dots color="white" />
                </template>
            </q-btn>
        </div>

        <!-- Loading indicator khi đang tải -->
        <div v-if="isLoadingMore" class="text-center q-pa-md">
            <q-spinner color="primary" size="2em" />
            <div>Loading more comments...</div>
        </div>

        <!-- End of comments message -->
        <div v-if="!hasMoreComments && comments.length > 0 && !isLoadingMore" class="text-center text-grey q-pa-md">
            No more comments to load.
        </div>

        <!-- Edit dialog -->
        <EditPostDialog v-model="isEditDialogOpen" :item="editData" :type="editData?.type || 'post'"
            @save="editFeedItemContent" />
    </div>
</template>

<style lang="scss" scoped>
.comment-section {
    max-width: 100%;
    margin: 0 auto;
}

.comment-input {
    border-radius: 24px;
    transition: all 0.3s ease;
    background: #f8f9fa;

    &:focus-within {
        background: white;
        box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    }
}

.comment-card {
    border-radius: 12px;
    transition: all 0.3s ease;
    margin-bottom: 16px;
    background: white;

    &:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    }
}

.reply-card {
    border-left: 3px solid $primary;
    border-radius: 8px;
    transition: all 0.3s ease;
    background: #f8f9fa;
    margin: 8px 0;

    &:hover {
        background: #f1f4f6;
    }
}

.comment-actions {
    opacity: 0.7;
    transition: opacity 0.2s ease;

    &:hover {
        opacity: 1;
    }

    .q-btn {
        border-radius: 8px;
        padding: 4px 8px;
        min-height: 32px;
        font-size: 0.875rem;

        &:hover {
            background: rgba(0, 0, 0, 0.05);
        }
    }
}

.user-avatar {
    transition: transform 0.2s ease;

    &:hover {
        transform: scale(1.05);
    }
}

// Animation classes
.comment-enter-active,
.comment-leave-active {
    transition: all 0.3s ease;
}

.comment-enter-from,
.comment-leave-to {
    opacity: 0;
    transform: translateY(20px);
}

.reply-enter-active,
.reply-leave-active {
    transition: all 0.3s ease;
}

.reply-enter-from,
.reply-leave-to {
    opacity: 0;
    transform: translateX(20px);
}

// Loading animations
.loading-spinner {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;

    .q-spinner {
        opacity: 0.7;
    }
}

// Empty state
.empty-state {
    text-align: center;
    padding: 40px 20px;
    color: $grey-7;

    .q-icon {
        font-size: 48px;
        margin-bottom: 16px;
        opacity: 0.5;
    }
}

// Reply thread line
.reply-thread {
    position: relative;
    margin-left: 20px;
    padding-left: 20px;

    &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 0;
        bottom: 0;
        width: 2px;
        background: $grey-4;
        border-radius: 1px;
    }
}

// Responsive styles
@media (max-width: 599px) {
    .comment-card {
        margin-bottom: 12px;
    }

    .reply-thread {
        margin-left: 12px;
        padding-left: 12px;
    }

    .comment-actions {
        opacity: 1;
    }
}
</style>
