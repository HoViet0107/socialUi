<template>
    <q-dialog v-model="isOpen">
        <q-card class="q-pa-md full-width" style="max-width: 600px;">
            <q-card-actions align="right">
                <q-btn flat label="Close" color="primary" @click="closeDialog" />
            </q-card-actions>
            <q-card-section>
                <div class="row items-center">
                    <q-avatar>
                        <q-icon name="account_circle" />
                    </q-avatar>
                    <div class="q-ml-sm text-grey text-caption text-weight-bold">
                        {{ feedItem.user ? feedItem.user.firstName : "Unknown" }}&nbsp;
                        {{ feedItem.user ? feedItem.user.surname : "Unknown" }}&nbsp;
                        {{ feedItem.user ? feedItem.user.lastName : "Unknown" }}
                    </div>
                </div>

                <div class="text-body2">{{ feedItem.content }}</div>
                <div class="text-caption text-grey">Created at: {{ formatDate(feedItem.createdAt) }}</div>
            </q-card-section>

            <q-separator />

            <!-- Component show comment -->
            <CommentSection :feedItemId="feedItem.feedItemId" :user="feedItem.user" />
        </q-card>
    </q-dialog>
</template>

<script setup>
import { defineProps, defineEmits, ref, watch } from "vue";
import CommentSection from "src/components/comment/CommentSection.vue";
import { formatDate } from "src/helpers/helperFunctions";

const props = defineProps({
    modelValue: Boolean, // receive `v-model` from parent component
    feedItem: Object, // receive `feedItem` obj from parent component
});

const emit = defineEmits(["update:modelValue"]); // allow child component to emit `v-model`

const isOpen = ref(props.modelValue); // local state for dialog

//watch `modelValue` changed to update `isOpen`
watch(() => props.modelValue, (newVal) => {
    isOpen.value = newVal;
});

// Close dialog and emit event to parent
const closeDialog = () => {
    emit("update:modelValue", false); // emit `v-model` to parent
};
</script>
