<template>
    <div class="message-input">
        <!-- Reply Preview -->
        <div v-if="replyingTo" class="reply-preview">
            <div class="reply-content">
                <q-icon name="reply" size="16px" />
                <span class="reply-text">Replying to {{ replyingTo.senderName }}</span>
                <span class="reply-message">{{ replyingTo.content.substring(0, 50) }}...</span>
            </div>
            <q-btn round flat size="xs" icon="close" @click="cancelReply" />
        </div>

        <!-- Input Area -->
        <div class="input-container">
            <!-- Attachment Button -->
            <q-btn round flat icon="add_circle" color="primary" size="sm" class="attach-button">
                <q-menu>
                    <q-list style="min-width: 150px">
                        <q-item clickable v-close-popup @click="selectFile('image')">
                            <q-item-section avatar>
                                <q-icon name="image" color="green" />
                            </q-item-section>
                            <q-item-section>Photo</q-item-section>
                        </q-item>

                        <q-item clickable v-close-popup @click="selectFile('video')">
                            <q-item-section avatar>
                                <q-icon name="videocam" color="red" />
                            </q-item-section>
                            <q-item-section>Video</q-item-section>
                        </q-item>

                        <q-item clickable v-close-popup @click="selectFile('file')">
                            <q-item-section avatar>
                                <q-icon name="attach_file" color="blue" />
                            </q-item-section>
                            <q-item-section>File</q-item-section>
                        </q-item>
                    </q-list>
                </q-menu>
            </q-btn>

            <!-- Text Input -->
            <q-input v-model="messageText" placeholder="Type a message..." dense borderless class="message-text-input"
                @keydown.enter.prevent="handleEnterKey" @focus="handleFocus" @blur="handleBlur">
                <template v-slot:append>
                    <q-btn v-if="!messageText && !selectedFile" round flat icon="mic" size="sm" color="primary"
                        @click="startVoiceRecording" />
                    <q-btn v-else round flat icon="send" size="sm" color="primary" @click="sendMessage"
                        :loading="sending" />
                </template>
            </q-input>
        </div>

        <!-- File Preview -->
        <div v-if="selectedFile" class="file-preview">
            <q-chip removable color="primary" text-color="white" icon="attach_file" @remove="selectedFile = null">
                {{ selectedFile.name }}
            </q-chip>
        </div>

        <!-- Hidden file input -->
        <input ref="fileInput" type="file" style="display: none" @change="handleFileSelect" :accept="fileAccept" />
    </div>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue';
import { useQuasar } from 'quasar';
import { MessageServices } from 'src/services/api';
import { getAuthToken } from 'src/helpers/helperFunctions';
// import WebsocketService from 'src/services/WebsocketService';

const props = defineProps({
    conversationId: {
        type: Number,
        required: true
    },
    replyingTo: {
        type: Object,
        default: null
    }
});

const emit = defineEmits(['message-sent', 'cancel-reply']);

const $q = useQuasar();
const token = getAuthToken();

/** Refs*/
const messageText = ref('');
const selectedFile = ref(null);
const sending = ref(false);
const fileInput = ref(null);
const fileAccept = ref('');
const isFocused = ref(false);

/** Watch for conversation change*/
watch(() => props.conversationId, () => {
    /** Clear input when conversation changes*/
    messageText.value = '';
    selectedFile.value = null;
});

/** Methods*/
const handleEnterKey = (event) => {
    if (!event.shiftKey) {
        sendMessage();
    }
};

const handleFocus = () => {
    isFocused.value = true;
};

const handleBlur = () => {
    isFocused.value = false;
};

const sendMessage = async () => {
    if (!messageText.value.trim() && !selectedFile.value) return;

    if (!props.conversationId) {
        $q.notify({
            type: 'negative',
            message: 'Failed to send message: Invalid conversation ID',
            timeout: 3000
        });
        return;
    }

    sending.value = true;

    try {
        const formData = new FormData();
        formData.append('content', messageText.value.trim());
        formData.append('status', 'SENT');
        formData.append('mediaType', selectedFile.value ? getMediaType() : 'NEW_MESSAGE');

        if (selectedFile.value) {
            formData.append('file', selectedFile.value);
        }

        const response = await MessageServices.sendMessage(
            props.conversationId,
            formData,
            token
        );

        const messageData = {
            ...response.data,
            conversationId: props.conversationId,
            id: response.data.id || `sent-${Date.now()}`,
            content: messageText.value.trim(),
            senderId: response.data.senderId || response.data.sender?.id,
            senderName: response.data.senderName || response.data.sender?.name,
            sentAt: response.data.sentAt || response.data.sendAt || response.data.createdAt || new Date().toISOString(),
        };

        emit('message-sent', messageData);

        messageText.value = '';
        selectedFile.value = null;

        $q.notify({
            type: 'positive',
            message: 'Message sent',
            timeout: 1000,
            position: 'top'
        });
    } catch (error) {
        console.error('Error sending message:', error);

        let errorMessage = 'Failed to send message';
        if (error.response) {
            if (error.response.status === 413) {
                errorMessage = 'File too large. Maximum size is 25MB';
            } else if (error.response.status === 415) {
                errorMessage = 'Unsupported file type';
            } else if (error.response.data?.message) {
                errorMessage = error.response.data.message;
            } else if (error.response.data) {
                errorMessage = `Server error: ${JSON.stringify(error.response.data)}`;
            }
        } else if (error.request) {
            errorMessage = 'Network error. Please check your connection';
        }

        $q.notify({
            type: 'negative',
            message: errorMessage,
            timeout: 5000
        });
    } finally {
        sending.value = false;
    }
};

const selectFile = (type) => {
    switch (type) {
        case 'image':
            fileAccept.value = 'image/*';
            break;
        case 'video':
            fileAccept.value = 'video/*';
            break;
        default:
            fileAccept.value = '*';
    }

    fileInput.value.click();
};

const handleFileSelect = (event) => {
    const file = event.target.files[0];
    if (!file) return;

    const maxSize = 25 * 1024 * 1024; // 25MB
    const fileType = file.type;

    if (file.size > maxSize) {
        $q.notify({ type: 'negative', message: 'File size must be less than 25MB', timeout: 3000 });
    } else if (fileAccept.value === 'image/*' && !fileType.startsWith('image/')) {
        $q.notify({ type: 'negative', message: 'Please select an image file', timeout: 3000 });
    } else if (fileAccept.value === 'video/*' && !fileType.startsWith('video/')) {
        $q.notify({ type: 'negative', message: 'Please select a video file', timeout: 3000 });
    } else {
        selectedFile.value = file;
    }

    event.target.value = '';
};

const getMediaType = () => {
    if (!selectedFile.value) return 'NONE';

    const type = selectedFile.value.type;
    if (type.startsWith('image/')) return 'IMAGE';
    if (type.startsWith('video/')) return 'VIDEO';
    if (type.startsWith('audio/')) return 'AUDIO';
    return 'FILE';
};

const startVoiceRecording = () => {
    $q.notify({
        type: 'info',
        message: 'Voice recording not implemented yet',
        caption: 'This feature is coming soon!',
        timeout: 3000
    });
};

const cancelReply = () => {
    emit('cancel-reply');
};

/** Cleanup on unmount*/
onUnmounted(() => {

});
</script>

<style lang="scss" scoped>
.message-input {
    background: white;
    border-top: 1px solid #e4e6eb;
}

.reply-preview {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 16px;
    background: #f0f2f5;
    border-bottom: 1px solid #e4e6eb;

    .reply-content {
        display: flex;
        align-items: center;
        gap: 8px;
        flex: 1;
        overflow: hidden;
    }

    .reply-text {
        font-size: 13px;
        color: #65676b;
    }

    .reply-message {
        font-size: 13px;
        color: #050505;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }
}

.input-container {
    display: flex;
    align-items: flex-end;
    padding: 12px 16px;
    gap: 8px;
}

.attach-button {
    margin-bottom: 8px;
}

.message-text-input {
    flex: 1;
    background: #f0f2f5;
    border-radius: 20px;
    padding: 8px 16px;

    :deep(.q-field__control) {
        min-height: 36px;
    }

    :deep(textarea) {
        resize: none;
        max-height: 120px;
        line-height: 1.4;
    }
}

.file-preview {
    padding: 0 16px 12px;
}

@media (max-width: 768px) {
    .input-container {
        padding: 8px;
    }
}
</style>