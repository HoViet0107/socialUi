<template>
    <div class="manage-posts">
        <!-- Post List -->
        <div v-if="!selectedPostId" class="post-list">
            <div class="header-section q-mb-lg">
                <div class="text-h5">Quản lý bài viết</div>
                <q-btn-group flat>
                    <q-btn icon="refresh" color="primary" @click="fetchPosts">
                        Làm mới
                    </q-btn>
                    <q-btn icon="filter_list" color="primary">
                        Bộ lọc
                        <q-menu>
                            <q-list style="min-width: 150px">
                                <q-item clickable v-close-popup @click="filterByDate('today')">
                                    <q-item-section>Hôm nay</q-item-section>
                                </q-item>
                                <q-item clickable v-close-popup @click="filterByDate('week')">
                                    <q-item-section>Tuần này</q-item-section>
                                </q-item>
                                <q-item clickable v-close-popup @click="filterByDate('month')">
                                    <q-item-section>Tháng này</q-item-section>
                                </q-item>
                            </q-list>
                        </q-menu>
                    </q-btn>
                </q-btn-group>
            </div>

            <div class="table-container">
                <q-table :rows="posts" :columns="columns" row-key="postId" :loading="loading" flat bordered
                    separator="cell" class="posts-table" v-model:pagination="pagination"
                    :rows-per-page-options="[10, 20, 50]">
                    <!-- Custom header -->
                    <template v-slot:header="props">
                        <q-tr :props="props">
                            <q-th v-for="col in props.cols" :key="col.name" :props="props">
                                {{ col.label }}
                            </q-th>
                        </q-tr>
                    </template>

                    <!-- Custom body -->
                    <template v-slot:body="props">
                        <q-tr :props="props" class="cursor-pointer" @click="viewPost(props.row)">
                            <q-td key="postId" :props="props">
                                {{ props.row.postId }}
                            </q-td>
                            <q-td key="content" :props="props" class="ellipsis">
                                {{ props.row.content }}
                            </q-td>
                            <q-td key="user" :props="props">
                                <div class="row items-center">
                                    <q-avatar size="32px" class="q-mr-sm">
                                        <img :src="getRandomAvatar(props.row.userId)">
                                    </q-avatar>
                                    {{ props.row.userName || props.row.userId }}
                                </div>
                            </q-td>
                            <q-td key="createdAt" :props="props">
                                {{ formatDate(props.row.createdAt) }}
                            </q-td>
                            <q-td key="actions" :props="props" class="text-center">
                                <div class="row q-gutter-sm justify-center">
                                    <q-btn flat round size="sm" color="primary" icon="visibility"
                                        @click.stop="viewComments(props.row)">
                                        <q-tooltip>Xem bình luận</q-tooltip>
                                    </q-btn>
                                    <q-btn flat round size="sm" color="warning" icon="edit"
                                        @click.stop="editPost(props.row)">
                                        <q-tooltip>Chỉnh sửa bài viết</q-tooltip>
                                    </q-btn>
                                    <q-btn flat round size="sm" color="negative" icon="delete"
                                        @click.stop="confirmDelete(props.row)">
                                        <q-tooltip>Xóa bài viết</q-tooltip>
                                    </q-btn>
                                </div>
                            </q-td>
                        </q-tr>
                    </template>

                    <!-- Loading state -->
                    <template v-slot:loading>
                        <div class="text-center q-pa-md">
                            <q-spinner color="primary" size="2em" />
                            <div class="q-mt-sm">Đang tải bài viết...</div>
                        </div>
                    </template>

                    <!-- No data state -->
                    <template v-slot:no-data>
                        <div class="text-center q-pa-lg">
                            <q-icon name="sentiment_dissatisfied" size="3em" color="grey-7" />
                            <div class="text-h6 q-mt-md">Không tìm thấy bài viết</div>
                            <div class="text-grey">Hãy thử điều chỉnh bộ lọc của bạn</div>
                        </div>
                    </template>
                </q-table>
            </div>
        </div>

        <!-- Comments List -->
        <div v-else class="comments-section">
            <div class="header-section q-mb-lg">
                <div class="row items-center">
                    <q-btn flat color="primary" icon="arrow_back" @click="selectedPostId = null" class="q-mr-md" />
                    <div class="text-h5">Bình luận cho bài viết #{{ selectedPostId }}</div>
                </div>
            </div>

            <div class="table-container">
                <q-table :rows="comments" :columns="commentColumns" row-key="commentId" :loading="loading" flat bordered
                    separator="cell" class="comments-table" v-model:pagination="commentPagination"
                    :rows-per-page-options="[10, 20, 50]">
                    <template v-slot:body="props">
                        <q-tr :props="props">
                            <q-td key="commentId" :props="props">
                                {{ props.row.commentId }}
                            </q-td>
                            <q-td key="content" :props="props" class="ellipsis">
                                {{ props.row.content }}
                            </q-td>
                            <q-td key="user" :props="props">
                                <div class="row items-center">
                                    <q-avatar size="32px" class="q-mr-sm">
                                        <img :src="getRandomAvatar(props.row.userId)">
                                    </q-avatar>
                                    {{ props.row.userName || props.row.userId }}
                                </div>
                            </q-td>
                            <q-td key="createdAt" :props="props">
                                {{ formatDate(props.row.createdAt) }}
                            </q-td>
                            <q-td key="actions" :props="props" class="text-center">
                                <div class="row q-gutter-sm justify-center">
                                    <q-btn flat round size="sm" color="warning" icon="edit"
                                        @click="editComment(props.row)">
                                        <q-tooltip>Chỉnh sửa bình luận</q-tooltip>
                                    </q-btn>
                                    <q-btn flat round size="sm" color="negative" icon="delete"
                                        @click="confirmDeleteComment(props.row)">
                                        <q-tooltip>Xóa bình luận</q-tooltip>
                                    </q-btn>
                                </div>
                            </q-td>
                        </q-tr>
                    </template>
                </q-table>
            </div>
        </div>

        <!-- Edit Dialog -->
        <EditPostDialog v-model="isEditDialogOpen" :item="editData" :type="editType" @save="updateContent" />

        <!-- Delete Confirmation Dialog -->
        <q-dialog v-model="deleteDialog">
            <q-card class="delete-dialog">
                <q-card-section class="row items-center">
                    <q-avatar icon="warning" color="negative" text-color="white" />
                    <span class="text-h6 q-ml-md">Xác nhận xóa</span>
                </q-card-section>

                <q-card-section>
                    Bạn có chắc chắn muốn xóa {{ deleteType }} này không? Hành động này không thể hoàn tác.
                </q-card-section>

                <q-card-actions align="right">
                    <q-btn flat label="Hủy" color="primary" v-close-popup />
                    <q-btn flat label="Xóa" color="negative" @click="executeDelete" v-close-popup />
                </q-card-actions>
            </q-card>
        </q-dialog>
    </div>
</template>

<script setup>
import { ref } from 'vue';
import { date } from 'quasar';
import EditPostDialog from 'src/components/editor/EditPostDialog.vue';
import { FeedItemServices } from 'src/services/api';

// Post list data
const posts = ref([]);
const loading = ref(false);
const selectedPostId = ref(null);
const comments = ref([]);
const isEditDialogOpen = ref(false);
const editData = ref(null);
const editType = ref('');

// Add these new functions
const pagination = ref({
    sortBy: 'createdAt',
    descending: true,
    page: 1,
    rowsPerPage: 10
});

const commentPagination = ref({
    sortBy: 'createdAt',
    descending: true,
    page: 1,
    rowsPerPage: 10
});

const deleteDialog = ref(false);
const deleteType = ref('');
const itemToDelete = ref(null);

const getRandomAvatar = (userId) => {
    const number = (userId % 8) + 1;
    return `https://cdn.quasar.dev/img/avatar${number}.jpg`;
};

const confirmDelete = (post) => {
    deleteType.value = 'bài viết';
    itemToDelete.value = post;
    deleteDialog.value = true;
};

const confirmDeleteComment = (comment) => {
    deleteType.value = 'bình luận';
    itemToDelete.value = comment;
    deleteDialog.value = true;
};

const deletePost = async (post) => {
    try {
        loading.value = true;
        await FeedItemServices.deleteFeedItem(post.postId);
        // Remove from local list
        posts.value = posts.value.filter(p => p.postId !== post.postId);
    } catch (error) {
        console.error('Error deleting post:', error);
    } finally {
        loading.value = false;
    }
};

const deleteComment = async (comment) => {
    try {
        loading.value = true;
        await FeedItemServices.deleteComment(comment.commentId);
        // Remove from local list
        comments.value = comments.value.filter(c => c.commentId !== comment.commentId);
    } catch (error) {
        console.error('Error deleting comment:', error);
    } finally {
        loading.value = false;
    }
};

const executeDelete = async () => {
    if (deleteType.value === 'bài viết') {
        await deletePost(itemToDelete.value);
    } else {
        await deleteComment(itemToDelete.value);
    }
};

const filterByDate = (period) => {
    console.log('Lọc theo:', period);
    // Implement date filtering logic
};

const viewPost = (post) => {
    // Implement post preview logic
    console.log('Xem bài viết:', post);
};

// Table columns definition
const columns = [
    {
        name: 'postId',
        required: true,
        label: 'ID',
        align: 'left',
        field: 'postId',
        sortable: true
    },
    {
        name: 'content',
        required: true,
        label: 'Nội dung',
        align: 'left',
        field: 'content',
        sortable: true
    },
    {
        name: 'user',
        required: true,
        label: 'Người đăng',
        align: 'left',
        field: 'userId',
        sortable: true
    },
    {
        name: 'createdAt',
        required: true,
        label: 'Ngày đăng',
        align: 'left',
        field: 'createdAt',
        sortable: true,
        format: val => formatDate(val)
    },
    {
        name: 'actions',
        required: true,
        label: 'Thao tác',
        align: 'center'
    }
];

const commentColumns = [
    {
        name: 'commentId',
        required: true,
        label: 'ID',
        align: 'left',
        field: 'commentId',
        sortable: true
    },
    {
        name: 'content',
        required: true,
        label: 'Nội dung',
        align: 'left',
        field: 'content',
        sortable: true
    },
    {
        name: 'user',
        required: true,
        label: 'Người bình luận',
        align: 'left',
        field: 'userId',
        sortable: true
    },
    {
        name: 'createdAt',
        required: true,
        label: 'Ngày bình luận',
        align: 'left',
        field: 'createdAt',
        sortable: true,
        format: val => formatDate(val)
    },
    {
        name: 'actions',
        required: true,
        label: 'Thao tác',
        align: 'center'
    }
];

// Date formatting function
const formatDate = (dateString) => {
    return date.formatDate(dateString, 'DD/MM/YYYY HH:mm');
};

// Initial data fetch
const fetchPosts = async () => {
    try {
        loading.value = true;
        const response = await FeedItemServices.getPosts();
        posts.value = response.data;
    } catch (error) {
        console.error('Error fetching posts:', error);
    } finally {
        loading.value = false;
    }
};

const editPost = (post) => {
    editData.value = post;
    editType.value = 'post';
    isEditDialogOpen.value = true;
};

const editComment = (comment) => {
    editData.value = comment;
    editType.value = 'comment';
    isEditDialogOpen.value = true;
};

const updateContent = async (updatedData) => {
    try {
        loading.value = true;
        if (updatedData.type === 'post') {
            await FeedItemServices.updatePost(updatedData);
            const index = posts.value.findIndex(p => p.postId === updatedData.postId);
            if (index !== -1) {
                posts.value[index] = { ...posts.value[index], ...updatedData };
            }
        } else {
            await FeedItemServices.updateComment(updatedData);
            const index = comments.value.findIndex(c => c.commentId === updatedData.commentId);
            if (index !== -1) {
                comments.value[index] = { ...comments.value[index], ...updatedData };
            }
        }
    } catch (error) {
        console.error('Error updating content:', error);
    } finally {
        loading.value = false;
    }
};

const viewComments = async (post) => {
    try {
        loading.value = true;
        selectedPostId.value = post.postId;
        const response = await FeedItemServices.getPostComments(post.postId);
        comments.value = response.data;
    } catch (error) {
        console.error('Error fetching comments:', error);
    } finally {
        loading.value = false;
    }
};

// Fetch posts on component mount
fetchPosts();
</script>

<style lang="scss" scoped>
.manage-posts {
    padding: 20px;
}

.header-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.table-container {
    background: white;
    border-radius: 12px;
    overflow: hidden;
    transition: all 0.3s ease;

    &:hover {
        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
    }
}

.posts-table,
.comments-table {
    .q-table__top {
        padding: 16px;
    }

    thead tr th {
        font-weight: 600;
        background: #f8f9fa;
    }

    tbody tr {
        transition: all 0.2s ease;

        &:hover {
            background: #f8f9fa;
        }

        td {
            padding: 12px 16px;
        }
    }

    .ellipsis {
        max-width: 300px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }
}

.delete-dialog {
    width: 400px;
    border-radius: 12px;

    .q-card__section {
        padding: 20px;
    }
}

@media (max-width: 600px) {
    .manage-posts {
        padding: 12px;
    }

    .header-section {
        flex-direction: column;
        align-items: flex-start;
        gap: 12px;
    }

    .table-container {
        border-radius: 8px;
    }

    .posts-table,
    .comments-table {
        .ellipsis {
            max-width: 150px;
        }
    }
}
</style>