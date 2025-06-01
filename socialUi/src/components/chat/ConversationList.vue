<template>
    <div class="conversation-list">
        <!-- Search Header -->
        <div class="conversation-header">
            <h5 class="text-h6 q-ma-none">Chats</h5>
            <q-btn round flat icon="edit" size="sm" @click="$emit('new-conversation')" />
        </div>

        <!-- Search Input -->
        <div class="conversation-search">
            <q-input v-model="searchQuery" outlined dense placeholder="Search conversations" class="search-input">
                <template v-slot:prepend>
                    <q-icon name="search" />
                </template>
            </q-input>
        </div>

        <!-- Conversation Items -->
        <q-scroll-area class="conversation-items">
            <q-list class="full-width">
                <!-- Loading state -->
                <q-item v-if="loading" class="flex justify-center">
                    <q-spinner color="primary" size="2em" />
                </q-item>

                <q-item v-for="conversation in filteredConversations" :key="conversation.id" clickable v-ripple
                    :class="{ 'selected': conversation.id === selectedConversationId }"
                    @click="$emit('select-conversation', conversation.id)">
                    <q-item-section avatar>
                        <q-avatar size="50px">
                            <img :src="conversation.avatar || getDefaultAvatar(conversation)" />
                            <q-badge v-if="conversation.isOnline" color="green" floating rounded class="online-badge" />
                        </q-avatar>
                    </q-item-section>

                    <!-- conversation -->
                    <q-item-section>
                        <q-item-label class="conversation-name">
                            {{ conversation.title }}
                        </q-item-label>
                        <q-item-label caption lines="1" class="conversation-last-message">
                            <span v-if="conversation.lastMessage">
                                {{ conversation.lastMessage.senderId === currentUserId ? 'You: ' :
                                    conversation.lastMessage.senderName }}
                                {{ conversation.lastMessage.content }}
                            </span>
                            <span v-else class="text-grey-5">No messages yet</span>
                        </q-item-label>
                    </q-item-section>

                    <!-- last message -->
                    <q-item-section side top>
                        <q-item-label caption>
                            {{ formatTime(conversation.lastMessageAt) }}
                        </q-item-label>
                    </q-item-section>
                </q-item>

                <!-- Empty State -->
                <div v-if="!loading && conversations.length === 0" class="empty-state">
                    <q-icon name="chat_bubble_outline" size="48px" color="grey-5" />
                    <p class="text-grey-6 q-mt-md">No conversations yet</p>
                </div>
            </q-list>
        </q-scroll-area>
    </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
    conversations: {
        type: Array,
        required: true,
        default: () => []
    },
    selectedConversationId: {
        type: Number,
        default: null
    },
    loading: {
        type: Boolean,
        default: false
    },
    currentUserId: {
        type: Number,
        default: null
    }
});

const _emit = defineEmits(['select-conversation', 'new-conversation']);
const searchQuery = ref('');

// Tính toán conversations sau khi filter
const filteredConversations = computed(() => {
    if (!searchQuery.value) return props.conversations;

    const query = searchQuery.value.toLowerCase();
    return props.conversations.filter(conv =>
        conv.name.toLowerCase().includes(query)
    );
});

const getDefaultAvatar = (conversation) => {
    if (conversation.type === 'GROUP') {
        return 'https://cdn.quasar.dev/img/avatar.png';
    }
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(conversation.name)}&background=random`;
};

const formatTime = (timestamp) => {
    if (!timestamp) return '';

    const date = new Date(timestamp);
    const now = new Date();
    const diffInHours = (now - date) / (1000 * 60 * 60);

    if (diffInHours < 24) {
        return date.toLocaleTimeString('en-US', {
            hour: 'numeric',
            minute: '2-digit',
            hour12: true
        });
    } else if (diffInHours < 168) { // 7 days
        return date.toLocaleDateString('en-US', { weekday: 'short' });
    } else {
        return date.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric'
        });
    }
};
</script>

<style lang="scss" scoped>
.conversation-list {
    width: 360px;
    height: 100%;
    background: white;
    display: flex;
    flex-direction: column;
}

.conversation-header {
    padding: 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #e4e6eb;
}

.conversation-search {
    padding: 8px 16px;

    .search-input {
        :deep(.q-field__control) {
            background: #f0f2f5;
            border-radius: 20px;
        }
    }
}

.conversation-items {
    flex: 1;
    height: calc(100% - 120px);
}

.q-item {
    padding: 8px 16px;
    transition: background-color 0.2s;

    &:hover {
        background: #f0f2f5;
    }

    &.selected {
        background: #e3f2fd;
    }
}

.conversation-name {
    font-weight: 500;
    font-size: 15px;
}

.conversation-last-message {
    font-size: 14px;
    color: #65676b;
}

.online-badge {
    width: 12px;
    height: 12px;
    border: 2px solid white;
}

.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 20px;
    text-align: center;
}

@media (max-width: 768px) {
    .conversation-list {
        width: 100%;
    }
}
</style>
