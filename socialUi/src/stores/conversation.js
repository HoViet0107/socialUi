import { defineStore } from 'pinia';
import { ConversationServices } from 'src/services/api';
import { getAuthToken } from 'src/helpers/helperFunctions';

export const useConversationStore = defineStore('conversation', {
    state: () => ({
        conversations: [],
        loading: false,
        error: null
    }),

    getters: {
        getConversations: (state) => state.conversations,
        isLoading: (state) => state.loading,
        hasError: (state) => state.error !== null,
        getError: (state) => state.error
    },

    actions: {
        async fetchConversations() {
            try {
                this.loading = true;
                this.error = null;

                const token = getAuthToken();
                if (!token) {
                    throw new Error('No authentication token found');
                }

                // Use the correct API service for fetching conversations
                const response = await ConversationServices.getUserConversations(token);

                // Extract data from response
                const data = response.data || {};
                const conversations = data.itemDTOList || [];

                // Format conversations
                const formattedConversations = conversations.map(conv => ({
                    id: conv.id,
                    name: conv.title || conv.participantNames?.join(', ') || 'Unknown',
                    lastMessage: conv.lastMessage?.content || '',
                    timestamp: conv.lastMessage?.timestamp || new Date(),
                    unread: conv.unreadCount || 0,
                    avatar: conv.avatar,
                    participants: conv.participantNames || []
                }));

                // Update store state
                this.conversations = formattedConversations;

                return {
                    data: formattedConversations
                };
            } catch (error) {
                console.error('Error in fetchConversations:', error);
                this.error = error.message || 'Failed to fetch conversations';
                throw error;
            } finally {
                this.loading = false;
            }
        },

        async markAsRead(conversationId) {
            try {
                const token = getAuthToken();
                if (!token) {
                    throw new Error('No authentication token found');
                }

                await ConversationServices.markAsRead(conversationId, token);

                // Update local state
                const conversation = this.conversations.find(c => c.id === conversationId);
                if (conversation) {
                    conversation.unread = 0;
                }
            } catch (error) {
                console.error('Error in markAsRead:', error);
                throw error;
            }
        },

        // Reset store state
        resetState() {
            this.conversations = [];
            this.loading = false;
            this.error = null;
        }
    }
});
