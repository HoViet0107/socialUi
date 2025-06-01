import { ref, computed, onUnmounted } from 'vue';
import { useQuasar } from 'quasar';
import enhancedWebSocketService from 'src/services/EnhancedWebsocketService';
import { useMessageStore } from 'src/stores/messageStore';
import { useConversationStore } from 'src/stores/conversationStore';
import { ConversationServices, MessageServices } from 'src/services/api';
import { eventBus } from 'src/utils/eventBus';
import { useNotifications } from 'src/composable/useNotifications';
import { messageFormatter } from 'src/utils/messageFormatter';

/**
 * Enhanced Chat Manager Composable
 * 
 * Provides complete chat functionality with improved:
 * - Modularity and reusability
 * - Error handling
 * - Performance optimization
 * - Integration with event bus
 */
export function useEnhancedChatManager(token) {
    // Initialize services
    const _$q = useQuasar();
    const notify = useNotifications();
    const wsService = enhancedWebSocketService;

    // State
    const conversations = ref([]);
    const messages = ref([]);
    const selectedConversationId = ref(null);
    const conversationsLoading = ref(false);
    const messagesLoading = ref(false);
    const conversationsCursor = ref(null);
    const messagesCursor = ref(null);
    const messageStore = ref(useMessageStore());
    const conversationStore = ref(useConversationStore());
    const isSearching = ref(false);
    const searchResults = ref([]);
    const searchQuery = ref('');
    const unreadCount = ref(0);

    // Track current conversation for cleanup
    let previousConversationId = null;

    // Computed
    const sortedMessages = computed(() => {
        if (!messageStore.value) return [];
        return messageStore.value.sortedMessages || [];
    });

    const selectedConversation = computed(() => {
        return conversations.value.find(c => c.id === selectedConversationId.value);
    });

    const groupedMessages = computed(() => {
        const messages = sortedMessages.value || [];
        return messageFormatter.groupMessagesByDate(messages, 'sentAt');
    });

    const hasMoreMessages = computed(() => {
        if (!messageStore.value) return false;
        return messageStore.value.hasMore;
    });

    // Methods for conversation management
    const loadConversations = async (loadMore = false) => {
        if (conversationsLoading.value || (!loadMore && conversations.value.length > 0)) return;

        conversationsLoading.value = true;
        try {
            const response = await ConversationServices.getUserConversations(token, conversationsCursor.value, 10);
            const data = response.data;

            if (loadMore) {
                conversations.value = [...conversations.value, ...data.itemDTOList];
            } else {
                conversations.value = data.itemDTOList;
            }

            conversationsCursor.value = data.nextCursor;

            // Update conversation store
            if (conversationStore.value) {
                conversationStore.value.setConversations(conversations.value);
                conversationStore.value.hasMore = !!data.nextCursor;
            }

            // Emit event
            eventBus.emit('conversations:loaded', {
                conversations: conversations.value,
                hasMore: !!data.nextCursor
            });

        } catch (error) {
            notify.error('Failed to load conversations', error.message);
            console.error('Error loading conversations:', error);
        } finally {
            conversationsLoading.value = false;
        }
    };

    const refreshConversations = async () => {
        // Reset cursor to load from the beginning
        conversationsCursor.value = null;
        conversations.value = [];

        await loadConversations();
    };

    const selectConversation = async (conversationId) => {
        if (!conversationId || previousConversationId === conversationId) {
            return;
        }

        try {
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

            // Mark conversation as read in conversation store
            if (conversationStore.value) {
                conversationStore.value.markConversationRead(conversationId);
            }

            // Join new conversation via WebSocket
            wsService.joinConversation(conversationId);
            previousConversationId = conversationId;

            // Load messages for the conversation
            await loadMessages();

            // Emit event
            eventBus.emit('conversation:selected', {
                id: conversationId,
                conversation: selectedConversation.value
            });

        } catch (error) {
            notify.error('Failed to load conversation', error.message);
            console.error('Error selecting conversation:', error);
        }
    };

    const loadMessages = async (loadMore = false) => {
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
                messageStore.value.setLoading(false);
                messagesLoading.value = false;
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

                // Emit event
                eventBus.emit('messages:loaded', {
                    conversationId,
                    messages: messageList,
                    hasMore: Boolean(data.hasMore)
                });
            } else {
                messages.value = [];
                messagesCursor.value = null;
                messageStore.value.hasMore = false;
            }
        } catch (error) {
            notify.error('Failed to load messages', error.message);
            console.error('Error in loadMessages:', error);
        } finally {
            messagesLoading.value = false;
            messageStore.value?.setLoading(false);
        }
    };

    const loadMoreMessages = () => {
        if (hasMoreMessages.value && !messagesLoading.value) {
            loadMessages(true);
        }
    };

    const handleBack = () => {
        console.log('Back navigation called in chat manager');

        try {
            // Reset state
            selectedConversationId.value = null;

            // Handle WebSocket cleanup
            if (previousConversationId) {
                console.log('Leaving WebSocket conversation:', previousConversationId);
                wsService.leaveConversation(previousConversationId);
                previousConversationId = null;
            }

            // Reset message store state
            if (messageStore.value) {
                messageStore.value.setCurrentConversation(null);
            }

            // Emit event
            eventBus.emit('conversation:closed');
        } catch (error) {
            console.error('Error in handleBack function:', error);
            // Fallback
            selectedConversationId.value = null;
        }
    };

    const updateConversationLastMessage = (message) => {
        try {
            // Validate inputs
            if (!message || !conversationStore.value) {
                console.warn('Cannot update conversation: missing message or store');
                return;
            }

            // Get conversation ID from message or use selected conversation
            const conversationId = message.conversationId || selectedConversationId.value;
            if (!conversationId) {
                console.warn('Cannot update conversation: missing conversation ID');
                return;
            }

            // Update conversation's last message in store
            conversationStore.value.updateLastMessage(
                conversationId,
                {
                    id: message.id || message.messageId,
                    content: message.content || '',
                    sentAt: message.sentAt || message.sendAt || message.createdAt || new Date().toISOString(),
                    senderId: message.senderId || message.sender?.id,
                    senderName: message.senderName || message.sender?.name,
                    type: message.type || 'TEXT'
                }
            );
        } catch (error) {
            console.error('Error updating conversation last message:', error);
        }
    };

    // Search functionality
    const searchConversations = async (query) => {
        if (!query || query.trim() === '') {
            searchResults.value = [];
            isSearching.value = false;
            return;
        }

        isSearching.value = true;
        searchQuery.value = query;

        try {
            const response = await ConversationServices.searchConversations(query, token);
            searchResults.value = response.data.items || [];
        } catch (error) {
            notify.error('Search failed', error.message);
            searchResults.value = [];
        } finally {
            isSearching.value = false;
        }
    };

    // Create new conversation
    const createConversation = async (participantIds, name = null, isGroup = false) => {
        try {
            const response = await ConversationServices.createConversation({
                participantIds,
                name,
                isGroup
            }, token);

            // Add to conversation list
            if (response.data) {
                const newConversation = response.data;

                // Update store
                if (conversationStore.value) {
                    conversationStore.value.addConversation(newConversation);
                }

                // Update local list
                conversations.value = [newConversation, ...conversations.value];

                // Emit event
                eventBus.emit('conversation:created', { conversation: newConversation });

                // Select the new conversation
                selectConversation(newConversation.id);

                return newConversation;
            }
        } catch (error) {
            notify.error('Failed to create conversation', error.message);
            throw error;
        }
    };

    // Edit conversation
    const editConversation = async (conversationId, data) => {
        try {
            const response = await ConversationServices.editConversation(conversationId, data, token);

            // Update local state
            const index = conversations.value.findIndex(c => c.id === conversationId);
            if (index !== -1) {
                conversations.value[index] = {
                    ...conversations.value[index],
                    ...data
                };
            }

            // Update store
            if (conversationStore.value) {
                conversationStore.value.updateConversation(conversationId, data);
            }

            // Emit event
            eventBus.emit('conversation:updated', {
                conversationId,
                updates: data
            });

            return response.data;
        } catch (error) {
            notify.error('Failed to update conversation', error.message);
            throw error;
        }
    };

    // Leave conversation
    const leaveConversation = async (conversationId) => {
        try {
            await ConversationServices.leaveConversation(conversationId, token);

            // Update local state
            conversations.value = conversations.value.filter(c => c.id !== conversationId);

            // Update store
            if (conversationStore.value) {
                conversationStore.value.removeConversation(conversationId);
            }

            // If this was the selected conversation, go back
            if (selectedConversationId.value === conversationId) {
                handleBack();
            }

            // Emit event
            eventBus.emit('conversation:left', { conversationId });

            return true;
        } catch (error) {
            notify.error('Failed to leave conversation', error.message);
            throw error;
        }
    };

    // Send message
    const sendMessage = async (conversationId, content, type = 'TEXT') => {
        try {
            // Optimistic UI update - add temporary message
            const tempId = `temp-${Date.now()}`;
            const tempMessage = {
                id: tempId,
                content,
                sentAt: new Date().toISOString(),
                type,
                conversationId,
                isSending: true,
                // Add user info as needed
            };

            // Add to message store
            if (messageStore.value) {
                messageStore.value.addMessage(tempMessage);
            }

            // Send via WebSocket
            const sent = await wsService.sendMessage(conversationId, content, type);

            if (!sent) {
                throw new Error('Failed to send message');
            }

            // Update conversation last message
            updateConversationLastMessage({
                ...tempMessage,
                isSending: false
            });

            return true;
        } catch (error) {
            notify.error('Failed to send message', error.message);
            console.error('Error sending message:', error);
            return false;
        }
    };

    // Setup event listeners
    const setupEventListeners = () => {
        // Listen for WebSocket events
        const unsubscribeNewMessage = eventBus.on('ws:message:NEW_MESSAGE', (data) => {
            if (data.message && data.conversationId) {
                // Update conversation last message
                updateConversationLastMessage(data.message);

                // Update unread count if not in the conversation
                if (selectedConversationId.value !== data.conversationId) {
                    unreadCount.value++;
                }
            }
        });

        // Return cleanup function
        return () => {
            unsubscribeNewMessage();
        };
    };

    // Setup listeners
    const cleanup = setupEventListeners();

    // Cleanup on component unmount
    onUnmounted(() => {
        // Clean up event listeners
        cleanup();

        // Leave current conversation
        if (previousConversationId) {
            wsService.leaveConversation(previousConversationId);
            previousConversationId = null;
        }
    });

    // Expose managed properties and methods
    return {
        // State
        conversations,
        messages,
        selectedConversationId,
        conversationsLoading,
        messagesLoading,
        isSearching,
        searchResults,
        searchQuery,
        unreadCount,

        // Computed
        sortedMessages,
        groupedMessages,
        selectedConversation,
        hasMoreMessages,

        // Methods - Conversation management
        loadConversations,
        refreshConversations,
        selectConversation,
        createConversation,
        editConversation,
        leaveConversation,

        // Methods - Message management
        loadMessages,
        loadMoreMessages,
        sendMessage,

        // Methods - Navigation
        handleBack,

        // Methods - Search
        searchConversations,

        // Methods - Updates
        updateConversationLastMessage
    };
}

// For backward compatibility
export function useChatManager(token) {
    return useEnhancedChatManager(token);
}

export default useEnhancedChatManager;
