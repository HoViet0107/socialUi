import { ref, computed } from 'vue';
import { useQuasar } from 'quasar';
import { getActivePinia } from 'pinia';
import WebSocketService from 'src/services/WebsocketService';
import { useMessageStore } from 'src/stores/messageStore';
import { useConversationStore } from 'src/stores/conversationStore';
import { ConversationServices, MessageServices } from 'src/services/api';

/**
 * Composable hook để quản lý tính năng chat
 * Tách biệt logic xử lý khỏi component UI
 */
export function useChatManager(token) {
    const $q = useQuasar();
    const wsService = WebSocketService;

    // State
    const conversations = ref([]);
    const messages = ref([]);
    const selectedConversationId = ref(null);
    const conversationsLoading = ref(false);
    const messagesLoading = ref(false);
    const conversationsCursor = ref(null);
    const messagesCursor = ref(null);
    const messageStore = ref(null);
    const conversationStore = ref(null);
    let previousConversationId = null;

    // Initialize stores lazily
    const initializeStores = () => {
        try {
            // Ensure Pinia is available
            const pinia = getActivePinia() || $q.inject('pinia');
            if (!pinia) {
                console.warn('No active Pinia instance found');
                return { messageStore: null, conversationStore: null };
            }

            if (!messageStore.value) {
                messageStore.value = useMessageStore();
            }
            if (!conversationStore.value) {
                conversationStore.value = useConversationStore();
            }
            return { messageStore, conversationStore };
        } catch (error) {
            console.error('Error initializing stores:', error);
            return { messageStore: null, conversationStore: null };
        }
    };
    // Computed
    const sortedMessages = computed(() => {
        initializeStores();
        if (!messageStore.value) return [];
        return messageStore.value.sortedMessages || [];
    });

    const selectedConversation = computed(() => {
        return conversations.value.find(c => c.id === selectedConversationId.value);
    });

    // Track whether we have more conversations to load
    const hasMore = ref(true);

    // Computed property để kiểm tra xem còn conversations để load không
    const hasMoreConversations = computed(() => {
        return hasMore.value && conversationsCursor.value !== null;
    });

    // Methods
    const loadConversations = async (loadMore = false) => {
        // Prevent duplicate requests
        if (conversationsLoading.value) return;

        // Don't load initial data if we already have it
        if (!loadMore && conversations.value.length > 0) return;

        conversationsLoading.value = true;
        try {
            // Initialize stores before using them
            initializeStores();

            const response = await ConversationServices.getUserConversations(token, conversationsCursor.value, 10);
            const data = response.data;

            const newItems = data.itemDTOList || [];

            if (loadMore && newItems.length > 0) {
                // Check for duplicates before adding
                const existingIds = new Set(conversations.value.map(c => c.id));
                const uniqueItems = newItems.filter(item => !existingIds.has(item.id));

                if (uniqueItems.length > 0) {
                    conversations.value = [...conversations.value, ...uniqueItems];
                }
            } else {
                conversations.value = newItems;
            }

            // Update hasMore based on response
            hasMore.value = newItems.length >= 10; // Assuming page size is 10
            conversationsCursor.value = data.nextCursor;

            conversationsCursor.value = data.nextCursor;
            // Update conversation store
            if (conversationStore.value) {
                conversationStore.value.setConversations(conversations.value);
            }
        } catch (error) {
            $q.notify({
                type: 'negative',
                message: 'Failed to load conversations'
            });
            console.error('Error loading conversations:', error.message || error);
        } finally {
            conversationsLoading.value = false;
        }
    };

    const selectConversation = async (conversationId) => {
        if (!conversationId || previousConversationId === conversationId) {
            return;
        }

        try {
            // Initialize stores before using them
            initializeStores();

            // Leave previous conversation if any
            if (previousConversationId) {
                wsService.leaveConversation(previousConversationId);
            }

            // Update UI state
            selectedConversationId.value = conversationId;
            messages.value = [];
            messagesCursor.value = null;

            // Update stores
            if (messageStore.value) {
                messageStore.value.setCurrentConversation(conversationId);
            }

            // Join new conversation via WebSocket
            wsService.joinConversation(conversationId);
            previousConversationId = conversationId;

            // Load messages for the conversation
            await loadMessages();

        } catch (error) {
            console.error('Error selecting conversation:', error);
            $q.notify({
                type: 'negative',
                message: 'Failed to load conversation',
                caption: error.message || 'Please try again',
                timeout: 3000
            });
        }
    };

    const loadMessages = async (loadMore = false) => {
        // Initialize stores before using them
        initializeStores();

        // Validate required data
        if (!selectedConversationId.value || messagesLoading.value || !messageStore.value) {
            console.warn('Cannot load messages: missing required data');
            return;
        }

        const conversationId = selectedConversationId.value;

        messagesLoading.value = true;
        messageStore.value.setLoading(true);

        try {
            // Set conversation only when not loading more old messages
            if (!loadMore) {
                messageStore.value.setCurrentConversation(conversationId);
            }

            // Get current messages from store
            const storeMessages = Array.isArray(messageStore.value.messages)
                ? [...messageStore.value.messages]
                : [];

            // If we already have messages and not loading more, just return
            if (storeMessages.length > 0 && !loadMore) {
                return;
            }

            // Fetch messages from the API
            const response = await MessageServices.getMessagesForConversation(
                conversationId,
                loadMore ? messagesCursor.value : null,
                token
            );

            if (!response?.data) {
                throw new Error('Invalid response from server');
            }

            const data = response.data;

            // Process messages from API
            const messageList = data.itemDTOList || data.messages || [];

            if (messageList.length > 0) {
                // Update cursor and hasMore state first
                messagesCursor.value = data.nextCursor || null;

                if (loadMore) {
                    // When loading old messages, add to the beginning of the list
                    if (typeof messageStore.value.prependMessages === 'function') {
                        messageStore.value.prependMessages(messageList, data.nextCursor, data.hasMore);
                    } else {
                        // Fallback: manually combine old and new messages
                        const allMessages = [...messageList, ...storeMessages];
                        messageStore.value.addMessages(allMessages, data.nextCursor, data.hasMore);
                    }
                } else {
                    // Initial load: replace all messages
                    messageStore.value.messages = [];
                    messageStore.value.addMessages(messageList, data.nextCursor, data.hasMore);
                }

                // Update hasMore state
                messageStore.value.hasMore = Boolean(data.hasMore);
            } else {
                messages.value = [];
                messagesCursor.value = null;
            }
        } catch (error) {
            console.error('Error in loadMessages:', error);
            $q.notify({
                type: 'negative',
                message: 'Failed to load messages',
                caption: error?.message || 'An error occurred while loading messages'
            });
        } finally {
            messagesLoading.value = false;
            messageStore.value?.setLoading(false);
        }
    };

    const editMessage = async (messageId, content, token) => {
        try {
            initializeStores();
            await MessageServices.editMessage(messageId, content, token);
            loadMessages();
        } catch (error) {
            console.error('Error editing message:', error);
            $q.notify({
                type: 'negative',
                message: 'Failed to edit message',
                caption: error?.message || 'An error occurred while editing message'
            });
        }
    };

    const loadMoreConversations = () => {
        // Initialize stores before using them
        initializeStores();

        if (conversationStore.value?.hasMore && !conversationsLoading.value) {
            loadConversations(true);
        }
    };

    const updateConversationLastMessage = (message) => {
        try {
            initializeStores();

            if (!message) {
                console.warn('Cannot update conversation: missing message');
                return;
            }

            if (!conversationStore.value) {
                console.warn('Cannot update conversation: store not initialized');
                return;
            }

            // Get conversation ID from message or use selected conversation
            const conversationId = message.conversationId || selectedConversationId.value;
            if (!conversationId) {
                console.warn('Cannot update conversation: missing conversation ID');
                return;
            }

            const lastMessageData = {
                id: message.id || message.messageId || `temp-${Date.now()}`,
                content: message.content || '',
                sentAt: message.sentAt || message.sendAt || message.createdAt || new Date().toISOString(),
                senderId: message.senderId || message.sender?.id,
                senderName: message.senderName || message.sender?.name,
                type: message.type || 'TEXT'
            };

            conversationStore.value.updateLastMessage(conversationId, lastMessageData);
        } catch (error) {
            console.error('Error updating conversation last message:', error);
        }
    };

    // Expose managed properties and methods
    return {
        // State
        conversations,
        messages,
        selectedConversationId,
        conversationsLoading,
        messagesLoading,
        messageStore,
        conversationStore,

        // Computed
        sortedMessages,
        selectedConversation,
        hasMoreConversations,

        // Methods
        initializeStores,
        loadConversations,
        loadMoreConversations,
        selectConversation,
        loadMessages,
        editMessage,
        updateConversationLastMessage
    };
}
