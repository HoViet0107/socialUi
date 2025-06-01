<template>
    <div class="message-list" ref="messageContainer">
        <q-scroll-area ref="scrollArea" class="message-scroll-area">
            <!-- Load More Button -->
            <div v-if="hasMore" class="text-center q-pa-md">
                <q-btn v-if="!loading" flat color="primary" label="Load earlier messages" size="sm"
                    @click="$emit('load-more')" />
                <q-spinner v-else color="primary" size="24px" />
            </div>

            <!-- Debug info for empty messages array -->
            <div v-if="!messages || messages.length === 0" class="text-center q-pa-lg">
                <q-icon name="info" color="grey" size="24px" />
                <div class="text-grey q-mt-sm">No messages to display</div>
                <div class="text-grey-7 text-caption q-mt-xs">
                    {{ loading ? 'Loading messages...' : 'Send a message to start chatting' }}
                </div>
            </div>

            <!-- Messages Container -->
            <div class="messages-container">
                <template v-for="(message, index) in messages" :key="message.id">
                    <!-- Date Separator -->
                    <div v-if="shouldShowDateSeparator(index)" class="date-separator">
                        <span class="date-text">{{ formatDate(message.sentAt) }}</span>
                    </div>

                    <!-- Message Item -->
                    <div :class="[
                        'message-item',
                        message.senderId === currentUserId ? 'sent' : 'received',
                        shouldGroupWithPrevious(index) ? 'grouped' : ''
                    ]" :data-message-id="message.id">
                        <!-- Avatar  -->
                        <q-avatar v-if="message.senderId !== currentUserId && !shouldGroupWithPrevious(index)"
                            size="28px" class="message-avatar">
                            <img :src="getAvatarUrl(message.senderName)" />
                        </q-avatar>

                        <!-- Message Content Wrapper -->
                        <div class="message-content-wrapper">
                            <!-- Sender Name -->
                            <div v-if="message.senderId !== currentUserId && !shouldGroupWithPrevious(index)"
                                class="message-sender">
                                {{ message.senderName }}
                            </div>

                            <!-- Reply To -->
                            <div v-if="message.replyToMessageId" class="message-reply">
                                <q-icon name="reply" size="16px" />
                                <span>{{ getReplyPreview(message.replyToMessageId) }}</span>
                            </div>

                            <!-- Message Bubble -->
                            <div class="message-bubble">
                                <div class="message-text">{{ message.content }}</div>

                                <!-- Media Attachment -->
                                <div v-if="message.mediaUrl" class="message-media">
                                    <img v-if="isImage(message.mediaType)" :src="message.mediaUrl" class="message-image"
                                        @click="showImage(message.mediaUrl)" />
                                    <video v-else-if="isVideo(message.mediaType)" :src="message.mediaUrl" controls
                                        class="message-video" />
                                    <a v-else :href="message.mediaUrl" target="_blank" class="message-file">
                                        <q-icon name="attach_file" />
                                        {{ message.mediaName || 'File attachment' }}
                                    </a>
                                </div>

                                <!-- Message Time & Status -->
                                <div class="message-info">
                                    <span class="message-time">
                                        {{ formatTime(message.sentAt) }}
                                        <span v-if="message.edited || message.editedAt" class="text-caption text-white-6">(edited)</span>
                                    </span>
                                    <q-icon v-if="message.senderId === currentUserId"
                                        :name="getStatusIcon(message.status)" size="16px"
                                        :color="message.status === 'read' ? 'primary' : 'grey'" />
                                </div>
                            </div>

                            <!-- Reactions -->
                            <div v-if="message.reactions?.length" class="message-reactions">
                                <q-chip v-for="reaction in groupReactions(message.reactions)" :key="reaction.type"
                                    size="sm" color="grey-3" text-color="black" clickable>
                                    {{ reaction.type }} {{ reaction.count }}
                                </q-chip>
                            </div>
                        </div>

                        <!-- Message Actions Menu -->
                        <div class="message-actions">
                            <q-btn round flat size="xs" icon="more_horiz">
                                <q-menu>
                                    <q-list style="min-width: 150px">
                                        <!-- Edit option - chỉ hiện với người gửi -->
                                        <q-item v-if="message.senderId === currentUserId" clickable v-close-popup
                                            @click="handleEditClick(message)">
                                            <q-item-section avatar>
                                                <q-icon name="edit" />
                                            </q-item-section>
                                            <q-item-section>Edit</q-item-section>
                                        </q-item>

                                        <!-- Delete option - chỉ hiện với người gửi -->
                                        <q-item v-if="message.senderId === currentUserId" clickable v-close-popup
                                            @click="handleDeleteClick(message)">
                                            <q-item-section avatar>
                                                <q-icon name="delete" color="negative" />
                                            </q-item-section>
                                            <q-item-section>Delete</q-item-section>
                                        </q-item>

                                        <!-- React option - mọi người -->
                                        <q-item clickable v-close-popup @click="handleReactClick(message)">
                                            <q-item-section avatar>
                                                <q-icon name="emoji_emotions" />
                                            </q-item-section>
                                            <q-item-section>React</q-item-section>
                                        </q-item>

                                        <!-- Reply option - mọi người -->
                                        <q-item clickable v-close-popup @click="handleReplyClick(message)">
                                            <q-item-section avatar>
                                                <q-icon name="reply" />
                                            </q-item-section>
                                            <q-item-section>Reply</q-item-section>
                                        </q-item>
                                    </q-list>
                                </q-menu>
                            </q-btn>
                        </div>
                    </div>
                </template>
            </div>
        </q-scroll-area>
    </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch, onUnmounted } from 'vue';
import { useQuasar } from 'quasar';
import { format, isToday, isYesterday, differenceInMinutes } from 'date-fns';

// Props - Dữ liệu được truyền từ component cha
const props = defineProps({
    messages: {
        type: Array,
        default: () => []
    },
    currentUserId: {
        type: Number,
        required: true
    },
    loading: {
        type: Boolean,
        default: false
    },
    hasMore: {
        type: Boolean,
        default: true
    }
});
console.log(props.messages);
// Events - Các sự kiện gửi lên component cha
const emit = defineEmits(['load-more', 'edit-message', 'delete-message', 'add-reaction', 'reply-to']);

// Composables
const $q = useQuasar();

// Refs - Tham chiếu đến các element DOM
const scrollArea = ref(null);
const messageContainer = ref(null);

// State - Trạng thái component
const isFirstLoad = ref(true); // Đánh dấu lần đầu tải tin nhắn

/**
 * Kiểm tra có nên hiển thị phân cách ngày không
 * @param {number} index - Vị trí tin nhắn trong mảng
 * @returns {boolean} - True nếu cần hiển thị phân cách ngày
 */
const shouldShowDateSeparator = (index) => {
    // Tin nhắn đầu tiên luôn hiển thị phân cách ngày
    if (index === 0) return true;

    const currentMessage = props.messages[index];
    const previousMessage = props.messages[index - 1];

    // So sánh ngày của tin nhắn hiện tại với tin nhắn trước đó
    const currentDate = new Date(currentMessage.sentAt).toDateString();
    const previousDate = new Date(previousMessage.sentAt).toDateString();

    return currentDate !== previousDate;
};

/**
 * Kiểm tra có nên gộp tin nhắn với tin nhắn trước đó không
 * @param {number} index - Vị trí tin nhắn trong mảng
 * @returns {boolean} - True nếu có thể gộp với tin nhắn trước
 */
const shouldGroupWithPrevious = (index) => {
    if (index === 0) return false;

    const currentMessage = props.messages[index];
    const previousMessage = props.messages[index - 1];

    // Gộp nếu cùng người gửi và trong vòng 5 phút
    return currentMessage.senderId === previousMessage.senderId &&
        differenceInMinutes(new Date(currentMessage.sentAt), new Date(previousMessage.sentAt)) < 5;
};

/**
 * Format ngày tháng hiển thị
 * @param {string} timestamp - Timestamp của tin nhắn
 * @returns {string} - Chuỗi ngày tháng đã format
 */
const formatDate = (timestamp) => {
    const date = new Date(timestamp);
    if (isNaN(date)) return '';

    if (isToday(date)) return 'Today';
    if (isYesterday(date)) return 'Yesterday';
    return format(date, 'MMMM d, yyyy');
};

/**
 * Format thời gian hiển thị
 * @param {string} timestamp - Timestamp của tin nhắn
 * @returns {string} - Chuỗi thời gian đã format
 */
const formatTime = (timestamp) => {
    const date = new Date(timestamp);
    if (isNaN(date)) return '';
    return format(date, 'h:mm a');
};

/**
 * Tạo URL avatar từ tên người dùng
 * @param {string} name - Tên người dùng
 * @returns {string} - URL avatar
 */
const getAvatarUrl = (name) => {
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(name || 'User')}&background=random`;
};

/**
 * Lấy icon trạng thái tin nhắn
 * @param {string} status - Trạng thái tin nhắn
 * @returns {string} - Tên icon
 */
const getStatusIcon = (status) => {
    switch (status) {
        case 'SENDING': return 'schedule';
        case 'SENT': return 'done';
        case 'DELIVERED': return 'done_all';
        case 'READ': return 'done_all';
        default: return 'error';
    }
};

/**
 * Kiểm tra loại media có phải hình ảnh không
 * @param {string} mediaType - Loại media
 * @returns {boolean} - True nếu là hình ảnh
 */
const isImage = (mediaType) => {
    return mediaType === 'IMAGE';
};

/**
 * Kiểm tra loại media có phải video không
 * @param {string} mediaType - Loại media
 * @returns {boolean} - True nếu là video
 */
const isVideo = (mediaType) => {
    return mediaType === 'VIDEO';
};

/**
 * Lấy preview của tin nhắn được reply
 * @param {number} replyToMessageId - ID tin nhắn được reply
 * @returns {string} - Nội dung preview
 */
const getReplyPreview = (replyToMessageId) => {
    const replyMessage = props.messages.find(m => m.id === replyToMessageId);
    return replyMessage ? replyMessage.content.substring(0, 50) + '...' : 'Original message';
};

/**
 * Gộp các reaction giống nhau và đếm số lượng
 * @param {Array} reactions - Mảng các reaction
 * @returns {Array} - Mảng reaction đã được gộp
 */
const groupReactions = (reactions) => {
    const grouped = {};
    reactions.forEach(reaction => {
        if (!grouped[reaction.type]) {
            grouped[reaction.type] = { type: reaction.type, count: 0 };
        }
        grouped[reaction.type].count++;
    });
    return Object.values(grouped);
};

/**
 * Hiển thị hình ảnh trong dialog
 * @param {string} imageUrl - URL hình ảnh
 */
const showImage = (imageUrl) => {
    $q.dialog({
        component: 'img',
        componentProps: {
            src: imageUrl,
            style: 'max-width: 90vw; max-height: 90vh;'
        }
    });
};

const handleEditClick = (message) => {
    emit('edit-message', message);
};

const handleDeleteClick = (message) => {
    emit('delete-message', message);
};

const handleReactClick = (message) => {
    showReactionPicker(message);
};

const handleReplyClick = (message) => {
    emit('reply-to', message);
};
/**
 * Show reaction picker
 * @param {Object} message - Message object
 */
const showReactionPicker = (message) => {
    const reactions = ['👍', '❤️', '😂', '😮', '😢', '😡'];

    $q.bottomSheet({
        title: 'React to message',
        grid: true,
        actions: reactions.map(emoji => ({
            label: emoji,
            handler() {
                emit('add-reaction', message.id, emoji);
            }
        }))
    });
};

/**
 * Scroll to bottom of message list
 * Use setTimeout to ensure DOM is fully rendered
 */
const scrollToBottom = () => {
    try {
        if (scrollArea.value && scrollArea.value.setScrollPosition) {
            setTimeout(() => {
                try {
                    const scroll = scrollArea.value.getScroll();
                    if (scroll && typeof scroll.verticalSize !== 'undefined') {
                        scrollArea.value.setScrollPosition('vertical', scroll.verticalSize);
                    } else {
                        // Fallback method 1: Use querySelector to find the scroll element
                        const scrollEl = scrollArea.value.$el?.querySelector('.scroll');
                        if (scrollEl) {
                            scrollEl.scrollTop = scrollEl.scrollHeight;
                        } else {
                            // Fallback method 2: Try direct DOM manipulation
                            const container = messageContainer.value;
                            if (container) {
                                container.scrollTop = container.scrollHeight;
                            }
                        }
                    }
                } catch (innerError) {
                    console.warn('Error in scroll timer:', innerError);
                }
            }, 100);
        }
    } catch (error) {
        console.warn('Error scrolling to bottom:', error);
    }
};

// Expose the method so it can be called from parent components
defineExpose({ scrollToBottom });

// Watch for changes in messages array
watch(() => props.messages, (newMessages, oldMessages) => {
    try {
        const oldLength = oldMessages ? oldMessages.length : 0;
        const newLength = newMessages ? newMessages.length : 0;

        if (isFirstLoad.value) {
            // First load: scroll to bottom to show latest messages
            nextTick(() => {
                scrollToBottom();
                isFirstLoad.value = false;
            });
        } else if (newLength > oldLength) {
            // New messages: automatically scroll to bottom
            nextTick(() => {
                scrollToBottom();
            });
        } else if (newLength < oldLength) {
            // Switch conversation: reset and scroll to bottom
            isFirstLoad.value = true;
            nextTick(() => {
                scrollToBottom();
            });
        } else if (newLength === oldLength && newMessages !== oldMessages) {
            // Messages array reference changed but same length
            nextTick(() => {
                // Check if we need to scroll by examining the last message
                const lastMessage = newMessages[newLength - 1];
                const isNewMessage = lastMessage &&
                    (!oldMessages || !oldMessages[oldLength - 1] ||
                        lastMessage.id !== oldMessages[oldLength - 1].id);

                if (isNewMessage) {
                    scrollToBottom();
                }
            });
        }
    } catch (error) {
        console.warn('Error in messages watcher:', error);
    }
}, { immediate: true, deep: true });

// Handler for new message events from WebSocket
const handleNewMessageEvent = (event) => {
    try {
        // Check if this conversation matches the event
        if (event.detail && props.messages && props.messages.length > 0) {
            nextTick(() => {
                scrollToBottom();
            });
        }
    } catch (error) {
        console.error('Error handling new message event:', error);
    }
};

// Component lifecycle
onMounted(async () => {
    await nextTick();
    scrollToBottom();

    // Add scroll event listener
    if (scrollArea.value) {
        scrollArea.value.setScrollPosition('vertical', 0);
    }

    // Listen for WebSocket new message events
    window.addEventListener('new-message-received', handleNewMessageEvent);
});

// Cleanup khi component bị destroy
onUnmounted(() => {
    // Remove event listeners
    window.removeEventListener('new-message-received', handleNewMessageEvent);
});
</script>

<style lang="scss" scoped>
// Container chính của danh sách tin nhắn
.message-list {
    flex: 1;
    position: relative;
    background: #f0f2f5;
}

// Scroll area của Quasar
.message-scroll-area {
    height: 100%;

    :deep(.scroll) {
        scroll-behavior: smooth;
        /* Enable smooth scrolling behavior */
    }
}

// Container chứa tất cả tin nhắn
.messages-container {
    padding: 16px;
    display: flex;
    flex-direction: column; // Hiển thị theo thứ tự thời gian
    gap: 4px;
    min-height: 100%; // Đảm bảo có thể scroll
}

// Phân cách ngày tháng giữa các tin nhắn
.date-separator {
    text-align: center;
    margin: 16px 0;
    position: relative;

    // Đường kẻ ngang
    &::before {
        content: '';
        position: absolute;
        top: 50%;
        left: 0;
        right: 0;
        height: 1px;
        background: #e4e6eb;
    }

    // Text hiển thị ngày
    .date-text {
        background: #f0f2f5;
        padding: 0 16px;
        font-size: 13px;
        color: #65676b;
        position: relative;
        z-index: 1;
    }
}

// Container của mỗi tin nhắn
.message-item {
    display: flex;
    align-items: flex-end;
    gap: 8px;
    margin-bottom: 2px;

    // Tin nhắn đã gửi (của user hiện tại)
    &.sent {
        flex-direction: row-reverse; // Avatar và nội dung đảo ngược

        .message-bubble {
            background: #0084ff; // Màu xanh của Facebook
            color: white;
            border-radius: 18px 18px 4px 18px;
        }

        .message-actions {
            order: -1; // Menu action ở bên trái
        }
    }

    // Tin nhắn nhận được
    &.received {
        .message-bubble {
            background: white;
            color: #050505;
            border-radius: 18px 18px 18px 4px;
        }
    }

    // Tin nhắn được gộp với tin nhắn trước đó
    &.grouped {
        .message-avatar {
            visibility: hidden; // Ẩn avatar cho tin nhắn gộp
        }

        // Thay đổi border-radius cho tin nhắn gộp
        &.sent .message-bubble {
            border-radius: 18px 18px 4px 18px;
        }

        &.received .message-bubble {
            border-radius: 4px 18px 18px 4px;
        }
    }
}

// Avatar người gửi
.message-avatar {
    margin-bottom: 4px;
}

// Wrapper chứa nội dung tin nhắn
.message-content-wrapper {
    max-width: 60%; // Giới hạn độ rộng tin nhắn
    display: flex;
    flex-direction: column;
    gap: 4px;
}

// Tên người gửi (trong group chat)
.message-sender {
    font-size: 12px;
    color: #65676b;
    margin-left: 12px;
}

// Hiển thị tin nhắn được reply
.message-reply {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px 8px;
    margin-left: 12px;
    background: rgba(0, 0, 0, 0.05);
    border-radius: 8px;
    font-size: 13px;
    color: #65676b;
}

// Bubble chứa nội dung tin nhắn
.message-bubble {
    padding: 8px 12px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
    word-wrap: break-word;
}

// Text nội dung tin nhắn
.message-text {
    font-size: 15px;
    line-height: 1.4;
}

// Media attachments (hình ảnh, video, file)
.message-media {
    margin-top: 8px;

    .message-image {
        max-width: 300px;
        max-height: 300px;
        border-radius: 12px;
        cursor: pointer;
    }

    .message-video {
        max-width: 300px;
        max-height: 300px;
        border-radius: 12px;
    }

    .message-file {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px;
        background: rgba(0, 0, 0, 0.05);
        border-radius: 8px;
        text-decoration: none;
        color: inherit;

        &:hover {
            background: rgba(0, 0, 0, 0.08);
        }
    }
}

// Thông tin thời gian và trạng thái tin nhắn
.message-info {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-top: 4px;
    font-size: 12px;

    .message-time {
        opacity: 0.7;
    }

    .text-caption.text-grey-6 {
        margin-left: 4px;
        font-size: 0.65rem;
        font-style: italic;
    }
}

// Reactions (biểu tượng cảm xúc)
.message-reactions {
    display: flex;
    gap: 4px;
    margin-top: 4px;
    margin-left: 12px;
}

// Menu actions của tin nhắn
.message-actions {
    opacity: 0;
    transition: opacity 0.2s;
}

// Hiển thị menu actions khi hover
.message-item:hover .message-actions {
    opacity: 1;
}

// Responsive cho mobile
@media (max-width: 768px) {
    .message-content-wrapper {
        max-width: 80%; // Tăng độ rộng tin nhắn trên mobile
    }

    .message-media {

        .message-image,
        .message-video {
            max-width: 200px;
            max-height: 200px;
        }
    }
}
</style>