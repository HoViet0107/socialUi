<template>
  <q-page class="full-height q-pa-md" style="display: flex; flex-direction: column; align-items: center;">
    <!-- Input form -->
    <q-card class="post-card q-mb-md full-width" style="max-width: 600px;">
      <q-card-section class="row items-center q-pb-none">
        <q-avatar size="40px" class="q-mr-sm">
          <img src="https://cdn.quasar.dev/img/avatar5.jpg">
        </q-avatar>
        <div class="text-subtitle2">What's on your mind?</div>
      </q-card-section>

      <q-card-section>
        <q-input v-model="text" type="textarea" autogrow borderless class="post-input"
          placeholder="Share your thoughts..." counter maxlength="5000" :dense="dense">
          <template v-slot:append>
            <q-icon v-if="text !== ''" name="close" @click="text = ''" class="cursor-pointer" />
          </template>
        </q-input>
      </q-card-section>

      <q-card-actions align="right" class="q-pa-md">
        <q-btn flat round dense icon="image" color="primary">
          <q-tooltip>Add photo</q-tooltip>
        </q-btn>
        <q-btn flat round dense icon="videocam" color="red-5">
          <q-tooltip>Add video</q-tooltip>
        </q-btn>
        <q-btn flat round dense icon="emoji_emotions" color="orange">
          <q-tooltip>Add emoji</q-tooltip>
        </q-btn>
        <q-space />
        <q-btn unelevated color="primary" icon-right="send" label="Post" @click="submitFeedItem" :loading="isSubmitting"
          :disable="!text.trim()" />
      </q-card-actions>
    </q-card>

    <!-- Loading state -->
    <div v-if="isLoading" class="text-center q-pa-lg full-width">
      <q-spinner color="primary" size="3em" />
      <div class="q-mt-md">Loading posts...</div>
    </div>

    <!-- Empty state -->
    <div v-else-if="feedItems.length === 0" class="text-center q-pa-lg full-width">
      <q-icon name="sentiment_dissatisfied" size="3em" color="grey-7" />
      <div class="text-h6 q-mt-md">No posts yet</div>
      <div class="text-grey">Be the first to share something!</div>
    </div>

    <!-- FeedItems (Posts) -->
    <q-card v-else v-for="feedItem in feedItems" :key="feedItem.feedItemId"
      class="post-card q-mb-md q-mx-auto full-width" style="max-width: 600px;">
      <q-card-section class="q-pb-none">
        <div class="row items-center justify-between">
          <div class="row items-center">
            <q-avatar size="40px" class="q-mr-sm shadow-1">
              <img src="https://cdn.quasar.dev/img/avatar5.jpg" v-if="feedItem.user?.avatar">
              <q-icon name="account_circle" size="40px" v-else />
            </q-avatar>
            <div>
              <div class="text-subtitle1 text-weight-bold">
                {{ feedItem.user ? `${feedItem.user.firstName} ${feedItem.user.lastName}` : feedItem.userId }}
              </div>
              <div class="text-caption text-grey">
                {{ formatDate(feedItem.createdAt) }}
              </div>
            </div>
          </div>

          <ActionMenu :item="feedItem" @edit="openEditDialog({ item: feedItem, type: feedItem.itemType })"
            @remove="handleDelete" @report="handleReport" />
        </div>
      </q-card-section>

      <q-card-section class="q-pt-md">
        <div class="text-body1 post-content">{{ feedItem.content }}</div>
      </q-card-section>

      <q-card-section class="q-pt-none">
        <div class="row items-center q-gutter-x-sm text-grey">
          <span v-if="feedItem.reactions?.likes">
            <q-icon name="favorite" size="16px" color="red" />
            {{ feedItem.reactions.likes }}
          </span>
          <span v-if="feedItem.commentCount">
            <q-icon name="mode_comment" size="16px" />
            {{ feedItem.commentCount }} comments
          </span>
        </div>
      </q-card-section>

      <q-separator />

      <q-card-actions align="stretch" class="q-px-md">
        <q-btn flat class="post-action-btn" :color="feedItem.reactions?.userLiked ? 'red' : 'grey'"
          :icon="feedItem.reactions?.userLiked ? 'favorite' : 'favorite_border'" @click="handlePostReaction(feedItem)">
          Like
        </q-btn>
        <q-btn flat class="post-action-btn" icon="mode_comment" color="grey" @click="openFeedItemDetail(feedItem)">
          Comment
        </q-btn>
        <q-btn flat class="post-action-btn" icon="share" color="grey">
          Share
        </q-btn>
      </q-card-actions>
    </q-card>

    <!-- Loading indicator for infinite scroll -->
    <div v-if="isLoadingMore" class="q-mt-md q-mb-xl full-width flex justify-center"
      style="max-width: 600px; margin: 0 auto;">
      <q-spinner color="primary" size="2em" />
      <div class="q-ml-sm">Loading more posts...</div>
    </div>

    <!-- End of feed message -->
    <div v-if="!hasMorePosts && feedItems.length > 0" class="q-mt-md q-mb-xl full-width flex justify-center"
      style="max-width: 600px; margin: 0 auto;">
      <div class="text-grey">You've reached the end of the feed</div>
    </div>

    <!-- Post detail Dialog -->
    <PostDetail v-model="isFeedItemOpen" :feedItem="selectedFeedItem" @update:feedItem="updateFeedItemAfterComment" />
    <!-- Edit post Dialog -->
    <EditPostDialog v-model="isEditDialogOpen" :item="editedData" :type="editedData?.type || 'post'"
      @save="editFeedItemContent" />
  </q-page>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { FeedItemServices } from "src/services/api";
import PostDetail from "src/components/PostDetail.vue";
import ActionMenu from "src/components/ActionMenu.vue";
import EditPostDialog from "src/components/editor/EditPostDialog.vue";
import { formatDate, getAuthToken } from "src/helpers/helperFunctions";
import { fetchUserDetails, deleteFeedItem, editFeedItem, fetchFeedItemList, fetchFeedItemReactionCount } from "src/helpers/commonRequest";
import { fetchFeedItemsRequest } from "src/models/models";
import { useQuasar } from "quasar";

// State variables
const feedItems = ref([]);
const text = ref('');
const dense = ref(false);
const selectedFeedItem = ref(null);
const isFeedItemOpen = ref(false);
const isEditDialogOpen = ref(false);
const editedData = ref(null);
const isLoading = ref(false);
const isSubmitting = ref(false);
const token = getAuthToken();

// Initialize scroll variables
const isLoadingMore = ref(false);
const hasMorePosts = ref(true);
const currentPage = ref(1);
const pageSize = ref(5); // Number of posts per page
// const totalLoaded = ref(0); // Total number of posts loaded

const $q = useQuasar(); // Quasar instance for notifications

/**
 * Fetches all feed items of type 'post' from the backend.
 * 
 * @async
 * @function fetchFeedItems
 * @description
 * - Sets loading state
 * - Retrieves posts using fetchFeedItemList
 * - Fetches user details for each post
 * - Updates UI with post data
 * - Handles errors and loading states
 */
const fetchFeedItems = async () => {
  isLoading.value = true;
  try {
    const requestBody = fetchFeedItemsRequest('post', currentPage.value, pageSize.value);
    const data = await fetchFeedItemList(requestBody);
    feedItems.value = Array.isArray(data) ? data : [];

    // Sử dụng Promise.all thay vì for...of để tối ưu hiệu suất
    const userDetailsPromises = feedItems.value.map(async (feedItem) => {
      if (feedItem.userId) {
        const userDetail = await fetchUserDetails(feedItem);
        const reactionData = await fetchFeedItemReactionCount(feedItem, token);
        return {
          ...feedItem,
          ...userDetail,
          reactions: reactionData.reactions || {}
        };
      }
      return feedItem;
    });

    feedItems.value = await Promise.all(userDetailsPromises);
    currentPage.value++;

  } catch (error) {
    console.error("Failed to fetch posts:", error);
  } finally {
    isLoading.value = false;
  }
};

/**
 * Loads more posts when user scrolls to bottom
 */
const loadMorePosts = async () => {
  if (isLoadingMore.value || !hasMorePosts.value) return;

  isLoadingMore.value = true;
  try {
    const requestBody = fetchFeedItemsRequest('post', currentPage.value, pageSize.value);

    const data = await fetchFeedItemList(requestBody);
    const newPosts = Array.isArray(data) ? data : [];

    // if there are no new posts, mark as no more posts to load
    // only set hasMorePosts to false if we received []
    if (newPosts.length === 0) {
      hasMorePosts.value = false;
      return;
    }

    // Check for duplicate posts
    const existingIds = new Set(feedItems.value.map(item => item.feedItemId));
    const uniqueNewPosts = newPosts.filter(post => !existingIds.has(post.feedItemId));

    // If there are no new unique posts, mark as no more posts to load
    if (uniqueNewPosts.length === 0) {
      hasMorePosts.value = false;
      return;
    }

    // Fetch user details for new posts
    for (const feedItem of uniqueNewPosts) {
      if (feedItem.userId) {
        const userDetail = await fetchUserDetails(feedItem);
        Object.assign(feedItem, userDetail);
      }
      const reactionData = await fetchFeedItemReactionCount(feedItem, token);
      feedItem.reactions = reactionData.reactions || {};
    }

    // Add new posts to the list
    feedItems.value = [...feedItems.value, ...uniqueNewPosts];
    currentPage.value = currentPage.value + 1; // Increment current page
  } catch (error) {
    console.error("Failed to load more posts:", error);
  } finally {
    isLoadingMore.value = false;
  }
};

/**
 * Checks if user has scrolled to bottom of page
 */
// Thêm throttle cho scroll handler
let scrollTimeout;
const handleScroll = () => {
  if (scrollTimeout) return;

  scrollTimeout = setTimeout(() => {
    const scrollPosition = window.scrollY;
    const windowHeight = window.innerHeight;
    const documentHeight = document.documentElement.scrollHeight;

    // Giảm threshold để load sớm hơn
    if (scrollPosition + windowHeight > documentHeight - 200 && !isLoadingMore.value && hasMorePosts.value) {
      loadMorePosts();
    }
    scrollTimeout = null;
  }, 100);
};

/**
 * Submits a new feed item to the backend.
 * 
 * @async
 * @function submitFeedItem
 * @description
 * - Validates authentication and content
 * - Shows loading state during submission
 * - Creates new post with optimistic UI update
 * - Handles errors with appropriate feedback
 */
const submitFeedItem = async () => {
  if (!token) {
    console.error("Token not found!");
    return;
  }

  if (!text.value.trim()) {
    console.error("Feed item content must not be empty!");
    return;
  }

  isSubmitting.value = true;

  const feedItemData = {
    content: text.value,
    itemType: "POST"
  };

  try {
    const response = await FeedItemServices.createFeedItem(feedItemData, token);
    const feedItem = response.data;

    // Initialize feedItems array if it doesn't exist
    if (!feedItems.value) {
      feedItems.value = [];
    }

    // Get current user details
    const userDetails = await fetchUserDetails(feedItem);

    // Add new post to the top of the feed with user details
    feedItems.value.unshift({
      ...feedItem,
      user: userDetails,
      likeCount: 0,
      commentCount: 0,
      likeIcon: 'favorite_border',
      liked: false
    });

    text.value = ""; // clear text area
  } catch (error) {
    console.error("Failed to create post:", error.response?.data || error.message);
  } finally {
    isSubmitting.value = false;
  }
};

/**
 * Opens the feed item(post) detail dialog.
 *
 * @function openFeedItemDetail
 * @param {Object} feedItem - The post to display in detail view
 * @description Sets the selected post and opens the detail dialog
 */
const openFeedItemDetail = (feedItem) => {
  selectedFeedItem.value = feedItem;
  isFeedItemOpen.value = true;
};

/**
 * Updates a feed item after comments have been added or modified.
 * 
 * @function updateFeedItemAfterComment
 * @param {Object} updatedFeedItem - The updated post with new comment data
 * @description Syncs the post in the main feed with changes from the detail view
 */
const updateFeedItemAfterComment = (updatedFeedItem) => {
  if (!updatedFeedItem) return;

  const index = feedItems.value.findIndex(item => item.feedItemId === updatedFeedItem.feedItemId);
  if (index !== -1) {
    // Update the post with new data while preserving reactivity
    feedItems.value[index] = { ...feedItems.value[index], ...updatedFeedItem };
  }
};

/**
 * Opens the edit dialog for a feed item.
 *
 * @function openEditDialog
 * @param {Object} payload - Contains the item and its type
 * @description Prepares the item for editing and opens the edit dialog
 */
const openEditDialog = ({ item, type }) => {
  editedData.value = { ...item, type }; // Create a copy to avoid reference issues
  isEditDialogOpen.value = true;
};

/**
 * Updates the content of a feed item.
 *
 * @async
 * @function editFeedItemContent
 * @param {Object} updatedData - The updated feed item data
 * @description
 * - Sends update request to the server
 * - Updates the local state to reflect changes
 * - Handles errors and UI state
 */
const editFeedItemContent = async (updatedData) => {
  try {
    if (!token) {
      console.error('No authentication token found');
      return;
    }

    const feedItemId = updatedData.feedItemId;
    const requestBody = {
      feedItemDTO: {
        feedItemId: feedItemId,
        itemType: updatedData.itemType || 'POST',
        fstatus: "ACTIVE",
        content: updatedData.content,
      },
      reactionRequest: {}
    };

    const response = await editFeedItem(requestBody, token);
    if (response.status !== 200) {
      console.error("Failed to update post:", response.data);
      return;
    }
    // Update local state
    const feedItemIndex = feedItems.value.findIndex(feedItem => feedItem.feedItemId === feedItemId);
    if (feedItemIndex !== -1) {
      // Update the content in the local feedItems array
      feedItems.value[feedItemIndex].content = updatedData.content;
    }

    // Update selected post if needed
    if (selectedFeedItem.value && selectedFeedItem.value.feedItemId === feedItemId) {
      selectedFeedItem.value.content = updatedData.content;
    }

    $q.notify({
      type: 'positive',
      message: 'Message edited!',
      position: 'top-right'
    });

  } catch (error) {
    console.error('Error updating content:', error);
  } finally {
    isEditDialogOpen.value = false;
  }
};

/**
 * Handles the deletion of a feed item.
 *
 * @async
 * @function handleDelete
 * @param {Object} feedItem - The feed item to delete
 * @description
 * - Prepares deletion request
 * - Calls the deletion service
 * - Updates UI after successful deletion
 */
const handleDelete = async (feedItem) => {
  const data = {
    feedItemDTO: {
      feedItemId: feedItem.feedItemId,
      itemType: feedItem.itemType || 'POST',
      fstatus: "DELETE"
    },
    reactionRequest: {}
  };

  if (!token) {
    console.error('No authentication token found');
    return;
  }

  try {
    await deleteFeedItem(feedItems, feedItem, data, token);

    // Close detail dialog if the deleted item was being viewed
    if (selectedFeedItem.value && selectedFeedItem.value.feedItemId === feedItem.feedItemId) {
      isFeedItemOpen.value = false;
      selectedFeedItem.value = null;
    }
  } catch (error) {
    console.error('Error deleting item:', error);
  }
};

/**
 * Handles reporting a feed item.
 *
 * @function handleReport
 * @param {Object} item - The item being reported
 * @description Logs the report action (placeholder for actual implementation)
 */
const handleReport = ({ item, type }) => {
  console.log(`${type === "post" ? "Post" : "Comment"} Reported:`, item);
  // Implement report functionality
};

/**
 * Handles like/unlike actions for a post.
 *
 * @async
 * @function handlePostReaction
 * @param {Object} post - The post to react to
 * @description
 * - Toggles like status (placeholder for actual API implementation)
 * - Updates UI to reflect reaction state
 */
const handlePostReaction = async (post) => {
  const requestBody = {
    feedItemDTO: {
      feedItemId: post.feedItemId,
      itemType: post.itemType,
      fstatus: "ACTIVE",
      content: post.content,
    },
    reactionRequest: {
      itemType: post.itemType,
      reactionType: "LIKE",
      itemId: post.feedItemId,
    }
  };

  try {
    const response = await editFeedItem(requestBody, token);
    if (response.status !== 200) {
      console.error("Failed to toggle reaction:", response.data);
      return;
    }
    // Sử dụng đúng cấu trúc reactions
    if (!post.reactions) post.reactions = {};
    post.reactions.userLiked = !post.reactions.userLiked;
    post.reactions.likes = (post.reactions.likes || 0) + (post.reactions.userLiked ? 1 : -1);

  } catch (error) {
    console.error("Error toggling reaction:", error);
  }
};

// Call API when component is loaded
// Add and remove scroll event listener
onMounted(() => {
  fetchFeedItems();
  window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});

</script>

<style lang="scss" scoped>
.post-input {
  transition: all 0.3s ease;
  border-radius: 8px;

  &:focus-within {
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
  }
}

.q-page {
  min-height: calc(100vh - 64px);
  overflow-x: hidden;
  overflow-y: auto; // Cho phép cuộn dọc
  padding-bottom: 20px; // Thêm padding để tránh bị cắt
}

.post-card {
  transition: all 0.3s ease;
  border-radius: 12px;
  overflow: hidden;
  background: white;
  will-change: transform; // Tối ưu animation

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 15px rgba(0, 0, 0, 0.1);
  }
}

.post-card {
  max-width: 600px;
  width: 100%;
  margin: 0 auto 16px auto;

  @media (max-width: 599px) {
    margin: 0 8px 16px 8px;
    max-width: calc(100% - 16px);
  }
}

.full-width {
  @media (max-width: 599px) {
    padding: 8px;
  }
}

.post-content {
  white-space: pre-line;
  word-break: break-word;
  font-size: 1rem;
  line-height: 1.5;
  color: $dark;
  max-width: 100%; // Đảm bảo không vượt quá container
}

.post-action-btn {
  transition: all 0.2s ease;
  border-radius: 8px;
  margin: 4px;
  flex: 1;

  &:hover {
    background: rgba(0, 0, 0, 0.05);
    transform: translateY(-1px);
  }

  &.q-btn--active {
    background: rgba(0, 0, 0, 0.1);
  }
}
</style>
