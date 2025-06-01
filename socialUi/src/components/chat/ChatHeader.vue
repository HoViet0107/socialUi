<template>
    <div class="chat-header">
        <div class="header-info">

            <q-avatar size="40px" class="q-mr-sm">
                <img :src="conversation?.avatar || getDefaultAvatar()" />
                <q-badge v-if="conversation?.isOnline" color="green" floating rounded class="online-badge" />
            </q-avatar>

            <div class="text-truncate">
                <div class="header-title text-truncate">{{ conversation?.name }}</div>
                <div class="header-status text-truncate">
                    <span v-if="conversation?.isOnline" class="text-green">Active now</span>
                    <span v-else-if="conversation?.lastSeen">
                        Last seen {{ formatLastSeen(conversation.lastSeen) }}
                    </span>
                    <span v-else class="text-grey-6">
                        {{ conversation?.participantCount }} members
                    </span>
                </div>
            </div>
        </div>

        <div class="header-actions">
            <q-btn round flat icon="phone" size="sm" class="desktop-only" />
            <q-btn round flat icon="videocam" size="sm" class="desktop-only" />
            <q-btn round flat icon="info" size="sm" class="mobile-tap-target">
                <q-menu anchor="bottom right" self="top right">
                    <q-list style="min-width: 200px">
                        <q-item clickable v-close-popup @click="showParticipants">
                            <q-item-section avatar>
                                <q-icon name="people" />
                            </q-item-section>
                            <q-item-section>View participants</q-item-section>
                        </q-item>

                        <q-item clickable v-close-popup @click="editConversation">
                            <q-item-section avatar>
                                <q-icon name="edit" />
                            </q-item-section>
                            <q-item-section>Edit conversation</q-item-section>
                        </q-item>

                        <q-separator />

                        <q-item clickable v-close-popup @click="$emit('leave-conversation')">
                            <q-item-section avatar>
                                <q-icon name="exit_to_app" color="negative" />
                            </q-item-section>
                            <q-item-section>Leave conversation</q-item-section>
                        </q-item>
                    </q-list>
                </q-menu>
            </q-btn>
        </div>
    </div>
</template>

<script setup>
import { formatDistanceToNow } from 'date-fns';
import { useQuasar } from 'quasar';

const props = defineProps({
    conversation: {
        type: Object,
        default: () => ({})
    }
});

const emit = defineEmits(['edit-conversation', 'leave-conversation', 'back']);

const $q = useQuasar();

const getDefaultAvatar = () => {
    if (props.conversation?.type === 'GROUP') {
        return 'https://cdn.quasar.dev/img/avatar.png';
    }
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(props.conversation?.name || '')}&background=random`;
};

const formatLastSeen = (timestamp) => {
    if (!timestamp) return '';
    return formatDistanceToNow(new Date(timestamp), { addSuffix: true });
};

const showParticipants = () => {
    $q.notify({
        type: 'positive',
        message: `${props.conversation?.participantCount || 0} participants in this conversation`,
        timeout: 2000
    });
};

const editConversation = () => {
    $q.notify({
        title: 'Edit Conversation',
        prompt: {
            model: props.conversation?.title || '',
            type: 'text'
        },
        cancel: true,
        persistent: true
    }).onOk(title => {
        if (title && title !== props.conversation?.title) {
            emit('edit-conversation', { title });
        }
    });
};


</script>

<style lang="scss">
@import '../../css/chat-header.css';
@import '../../css/utility.css';

.chat-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #e4e6eb;
    background: white;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
    z-index: var(--z-high, 50);
}

.header-info {
    display: flex;
    align-items: center;
    max-width: 70%;
    overflow: hidden;
}

.header-title {
    font-weight: 600;
    font-size: 16px;
    color: #050505;
    max-width: 100%;
}

.header-status {
    font-size: 13px;
    color: #65676b;
    margin-top: 2px;
    max-width: 100%;
}

.header-actions {
    display: flex;
    gap: 4px;
}

.online-badge {
    width: 10px;
    height: 10px;
    border: 2px solid white;
}

.mobile-only {
    display: none;
}

.desktop-only {
    display: inline-flex;
}

.back-btn-container {
    margin-right: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    z-index: var(--z-top, 100);
}

.back-text {
    font-size: 16px;
    font-weight: 500;
    color: #1976d2;
    margin-left: 4px;
    display: none;
}

@media (max-width: 768px) {
    .mobile-only {
        display: inline-flex !important;
    }

    .desktop-only {
        display: none !important;
    }

    .chat-header {
        padding: 12px; // Ensure enough padding for the back button
    }

    .header-info {
        max-width: 60%; // Smaller on mobile to accommodate back button
    }

    .back-text {
        display: inline-block;
    }
}
</style>