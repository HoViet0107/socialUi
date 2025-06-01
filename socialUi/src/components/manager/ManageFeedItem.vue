<template>
    <div>
        <!-- Post List -->
        <div v-if="!selectedPostId">
            <div class="row justify-between items-center q-mb-md">
                <div class="text-h5">Manage Posts</div>
                <q-btn label="Refresh" icon="refresh" color="primary" @click="fetchPosts" />
            </div>

            <q-table :rows="posts" :columns="columns" row-key="postId" flat bordered dense>
                <template v-slot:body-cell-actions="props">
                    <q-btn flat dense icon="visibility" color="primary" @click="viewComments(props.row)" />
                    <q-btn flat dense icon="edit" color="primary" @click="editPost(props.row)" />
                    <q-btn flat dense icon="delete" color="negative" @click="deletePost(props.row)" />
                </template>
            </q-table>
        </div>

        <!-- Comment List -->
        <div v-else>
            <div class="row justify-between items-center q-mb-md">
                <div class="text-h5">Comments for Post ID: {{ selectedPostId }}</div>
                <q-btn label="Back to Posts" icon="arrow_back" color="primary" @click="selectedPostId = null" />
            </div>

            <q-table :rows="comments" :columns="commentColumns" row-key="commentId" flat bordered dense>
                <template v-slot:body-cell-actions="props">
                    <q-btn flat dense icon="edit" color="primary" @click="editComment(props.row)" />
                    <q-btn flat dense icon="delete" color="negative" @click="deleteComment(props.row)" />
                </template>
            </q-table>
        </div>
        <EditPostDialog v-model="isEditDialogOpen" :item="editData" :type="editType" @save="updateContent" />
    </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { PostServices, CommentServices, UserServices } from "src/services/api";
import EditPostDialog from "../editor/EditPostDialog.vue";

const posts = ref([]); // List of posts
const comments = ref([]); // List of comments for a selected post
const selectedPostId = ref(null); // Currently selected post ID
const token = JSON.parse(localStorage.getItem("authUser")); // Get token from local storage
const isEditDialogOpen = ref(false); // Dialog state for editing posts
const editData = ref(null); // Data for the post or comment being edited
const editType = ref(null); // Type of edit (post or comment)


// Columns for the post table
const columns = [
    { name: "postId", label: "Post ID", align: "left", field: "postId" },
    { name: "content", label: "Content", align: "left", field: "content" },
    { name: "user", label: "User", align: "left", field: "userId" },
    { name: "createdAt", label: "Created At", align: "left", field: "createdAt" },
    { name: "actions", label: "Actions", align: "center" }
];

// Columns for the comment table
const commentColumns = [
    { name: "commentId", label: "Comment ID", align: "left", field: "commentId" },
    { name: "content", label: "Content", align: "left", field: "content" },
    { name: "user", label: "User", align: "left", field: "userId" },
    { name: "createdAt", label: "Created At", align: "left", field: "createdAt" },
    { name: "actions", label: "Actions", align: "center" }
];

// Fetch posts and include user information
const fetchPosts = async () => {
    try {
        const response = await PostServices.getPosts();
        const postList = response.data._embedded.postDTOList;

        // Fetch user information for each post
        for (let post of postList) {
            if (post.userId) {
                const userResponse = await UserServices.getUserById(post.userId);
                post.userName = `${userResponse.data.firstName} ${userResponse.data.lastName}`;
            } else {
                post.userName = "Unknown User";
            }
        }

        posts.value = postList;
    } catch (error) {
        console.error("Error fetching posts:", error);
    }
};

// Fetch comments for a specific post
const fetchComments = async (postId) => {
    try {
        const response = await CommentServices.getComments(postId);
        comments.value = response.data.map(comment => ({
            ...comment,
            userName: `${comment.user?.firstName || "Unknown"} ${comment.user?.lastName || ""}`
        }));
    } catch (error) {
        console.error("Error fetching comments:", error);
    }
};

// View comments for a specific post
const viewComments = (post) => {
    selectedPostId.value = post.postId;
    fetchComments(post.postId);
};

// Edit a post
const editPost = (post) => {
    console.log("Editing post:", post);
    editType.value = "post"; // Set the edit type to post
    editData.value = { ...post }; // Clone the post data for editing
    isEditDialogOpen.value = true; // Open the edit dialog
};

// send changed data to server
const updateContent = async (updatedData) => {
    try {
        if (!token) {
            console.error("No authentication token found");
            return;
        }

        if (editType.value === "post") { // Check if the edit type is post
            const requestBody = {
                content: updatedData.content,
                post: {
                    postId: updatedData.postId
                }
            };

            // Call the API to update the post
            const response = await PostServices.updatePost(requestBody, token);

            // Update the local posts array with the new data
            const postIndex = posts.value.findIndex(post => post.postId === updatedData.postId);
            if (postIndex !== -1) {
                posts.value[postIndex].content = updatedData.content;
                posts.value[postIndex].lastUpdated = response.data.lastUpdated || new Date().toISOString();
            }

            console.log("Post updated successfully");
        }

        if (editType.value === "comment") { // Check if the edit type is comment
            const requestBody = {
                commentId: updatedData.commentId,
                content: updatedData.content,
                parentComment: {
                    commentId: updatedData.parentCommentId
                }
            };

            // Call the API to update the comment
            const response = await CommentServices.editComment(requestBody, token);

            // Update the local comments array with the new data
            const commentIndex = comments.value.findIndex(comment => comment.commentId === updatedData.commentId);
            if (commentIndex !== -1) {
                comments.value[commentIndex].content = updatedData.content;
                comments.value[commentIndex].lastUpdated = response.data.lastUpdated || new Date().toISOString();
            }

            console.log("Comment updated successfully");
        }
    } catch (error) {
        console.error("Error updating post:", error);
    } finally {
        isEditDialogOpen.value = false; // Close the edit dialog
        editData.value = null; // Reset the edit data
    }
};

// Delete a post
const deletePost = async (post) => {
    if (confirm(`Are you sure you want to delete post ID ${post.postId}?`)) {
        try {
            await PostServices.deletePost(post.postId, token);
            posts.value = posts.value.filter(p => p.postId !== post.postId);
        } catch (error) {
            console.error("Error deleting post:", error);
        }
    }
};

// Edit a comment
const editComment = (comment) => {
    console.log("Editing comment:", comment);
    editType.value = "comment"; // Set the edit type to comment
    editData.value = { ...comment }; // Clone the post data for editing
    isEditDialogOpen.value = true; // Open the edit dialog
};

// Delete a comment
const deleteComment = async (comment) => {
    if (confirm(`Are you sure you want to delete comment ID ${comment.commentId}?`)) {
        try {
            await CommentServices.deleteComment(comment, token);
            comments.value = comments.value.filter(c => c.commentId !== comment.commentId);
        } catch (error) {
            console.error("Error deleting comment:", error);
        }
    }
};

// Fetch posts on component mount
onMounted(fetchPosts);
</script>