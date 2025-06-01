<template>
    <!-- ✅ Thêm debug style và force z-index cao -->
    <q-dialog 
        :model-value="modelValue" 
        @update:model-value="val => $emit('update:modelValue', val)" 
        persistent
        style="z-index: 99999 !important;"
    >
        <q-card style="min-width: 350px; z-index: 99999 !important;">
            <q-card-section class="row items-center">
                <div class="text-h6">Edit Message</div>
                <q-space />
                <q-btn icon="close" flat round dense @click="$emit('update:modelValue', false)" />
            </q-card-section>

            <q-card-section class="q-pt-none">
                <q-input v-model="editedContent" type="textarea" autofocus
                    :rules="[val => !!val || 'Please enter message content']" outlined label="Message Content"
                    :loading="saving" rows="3" ref="messageInput" />
            </q-card-section>

            <q-card-actions align="right">
                <q-btn flat label="Cancel" color="negative" @click="$emit('update:modelValue', false)" />
                <q-btn flat label="Save" color="primary" :loading="saving" :disable="!editedContent.trim()"
                    @click="saveEdit" />
            </q-card-actions>
        </q-card>
    </q-dialog>
</template>

<script setup>
import { ref, defineProps, defineEmits, watch } from 'vue';

const props = defineProps({
    modelValue: Boolean,
    message: {
        type: Object,
        default: () => ({})
    }
});

const emit = defineEmits(['update:modelValue', 'save']);

// Local states
const editedContent = ref('');
const saving = ref(false);
const messageInput = ref(null);

// Không cần computed property vì chúng ta đã xử lý trực tiếp với :model-value và @update:model-value

// Watch for dialog open
watch(() => props.modelValue, (val) => {
    if (val && props.message) {
        // Initialize content when dialog opens
        editedContent.value = props.message.content || '';

        // Focus the input when dialog opens
        setTimeout(() => {
            if (messageInput.value) {
                messageInput.value.focus();
            }
        }, 100);
    }
});

/**
 * Lưu nội dung tin nhắn đã chỉnh sửa
 */
const saveEdit = async () => {
    if (!editedContent.value.trim()) return;

    try {
        saving.value = true;
        // Get the appropriate ID from the message object
        const messageId = props.message.id || props.message.messageId;

        if (!messageId) {
            throw new Error('Cannot edit message: No message ID found');
        }

        // Emit event for parent component to handle
        emit('save', {
            messageId: messageId,
            newContent: editedContent.value.trim()
        });
        
        // Explicitly close the dialog after saving
        emit('update:modelValue', false);
    } catch (error) {
        console.error('Error in save edit function:', error);
    } finally {
        saving.value = false;
    }
};
</script>
