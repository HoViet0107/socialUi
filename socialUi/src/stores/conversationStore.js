import { defineStore } from 'pinia'

export const useConversationStore = defineStore('conversation', {
    state: () => ({
        conversations: [],
        currentConversation: null,
        loading: false,
        cursor: null,
        hasMore: true,
        searchQuery: '',
        filter: 'all', // all, unread, archived
        isInitialized: false
    }),

    getters: {
        /**
         * Get sorted conversations by last message time
         */
        sortedConversations: (state) => {
            return [...state.conversations].sort((a, b) => {
                const dateA = new Date(a.lastMessageAt || a.createdAt);
                const dateB = new Date(b.lastMessageAt || b.createdAt);
                return dateB - dateA;
            });
        },

        /**
         * Get filtered conversations based on current filter
         */
        filteredConversations: (state) => {
            let filtered = state.sortedConversations;

            // Apply search filter
            if (state.searchQuery) {
                const query = state.searchQuery.toLowerCase();
                filtered = filtered.filter(conv => {
                    // Search in conversation name
                    if (conv.name && conv.name.toLowerCase().includes(query)) {
                        return true;
                    }

                    // Search in participant names
                    if (conv.participants) {
                        return conv.participants.some(p =>
                            p.name && p.name.toLowerCase().includes(query)
                        );
                    }

                    // Search in last message
                    if (conv.lastMessage && conv.lastMessage.content) {
                        return conv.lastMessage.content.toLowerCase().includes(query);
                    }

                    return false;
                });
            }

            // Apply status filter
            switch (state.filter) {
                case 'unread':
                    return filtered.filter(conv => conv.unreadCount > 0);
                case 'archived':
                    return filtered.filter(conv => conv.isArchived);
                default:
                    return filtered.filter(conv => !conv.isArchived);
            }
        },

        /**
         * Get conversation by ID
         */
        getConversationById: (state) => (id) => {
            return state.conversations.find(c => c.id === id);
        },

        /**
         * Get total unread count
         */
        totalUnreadCount: (state) => {
            return state.conversations.reduce((count, conv) => {
                if (!conv.isArchived && !conv.isMuted) {
                    return count + (conv.unreadCount || 0);
                }
                return count;
            }, 0);
        },

        /**
         * Get online participants count for a conversation
         */
        getOnlineParticipants: (state) => (conversationId) => {
            const conversation = state.conversations.find(c => c.id === conversationId);
            if (!conversation || !conversation.participants) return 0;

            return conversation.participants.filter(p => p.isOnline).length;
        },

        /**
         * Check if conversation has active call
         */
        hasActiveCall: (state) => (conversationId) => {
            const conversation = state.conversations.find(c => c.id === conversationId);
            return conversation?.activeCall || false;
        }
    },

    actions: {
        /**
         * Initialize the store
         */
        initialize() {
            if (this.isInitialized) return;
            this.isInitialized = true;
            this.conversations = [];
            this.currentConversation = null;
            this.loading = false;
            this.cursor = null;
            this.hasMore = true;
            this.searchQuery = '';
            this.filter = 'all';
        },

        /**
         * Set conversations list
         */
        setConversations(conversations) {
            this.conversations = conversations;
        },

        /**
         * Add new conversation
         */
        addConversation(conversation) {
            const existingIndex = this.conversations.findIndex(c => c.id === conversation.id);

            if (existingIndex === -1) {
                // New conversation - add to beginning
                this.conversations.unshift(conversation);
            } else {
                // Update existing
                this.conversations[existingIndex] = {
                    ...this.conversations[existingIndex],
                    ...conversation
                };
            }
        },

        /**
         * Add multiple conversations (for pagination)
         */
        addConversations(conversations, cursor, hasMore) {
            // Filter out duplicates
            const newConversations = conversations.filter(conv =>
                !this.conversations.some(c => c.id === conv.id)
            );

            this.conversations.push(...newConversations);
            this.cursor = cursor;
            this.hasMore = hasMore;
        },

        /**
         * Update conversation
         */
        updateConversation(conversationId, updates) {
            const index = this.conversations.findIndex(c => c.id === conversationId);
            if (index !== -1) {
                this.conversations[index] = {
                    ...this.conversations[index],
                    ...updates,
                    updatedAt: new Date().toISOString()
                };
            }
        },

        /**
         * Update last message for conversation
         */
        updateLastMessage(conversationId, message) {
            const conversation = this.conversations.find(c => c.id === conversationId);
            if (conversation) {
                conversation.lastMessage = {
                    id: message.id,
                    content: message.content,
                    sentAt: message.sentAt || message.sendAt,
                    senderId: message.senderId,
                    senderName: message.senderName,
                    type: message.type || 'TEXT'
                };
                conversation.lastMessageAt = message.sentAt || message.sendAt;

                // Move to top if new message
                this.moveConversationToTop(conversationId);

                // Increment unread count if not current conversation
                if (this.currentConversation?.id !== conversationId) {
                    conversation.unreadCount = (conversation.unreadCount || 0) + 1;
                }
            }
        },

        /**
         * Move conversation to top of list
         */
        moveConversationToTop(conversationId) {
            const index = this.conversations.findIndex(c => c.id === conversationId);
            if (index > 0) {
                const [conversation] = this.conversations.splice(index, 1);
                this.conversations.unshift(conversation);
            }
        },

        /**
         * Set current conversation
         */
        setCurrentConversation(conversation) {
            this.currentConversation = conversation;

            // Reset unread count for current conversation
            if (conversation) {
                this.markAsRead(conversation.id || conversation);
            }
        },

        /**
         * Remove conversation
         */
        removeConversation(conversationId) {
            const index = this.conversations.findIndex(c => c.id === conversationId);
            if (index !== -1) {
                this.conversations.splice(index, 1);
            }

            // Clear current if it was removed
            if (this.currentConversation?.id === conversationId) {
                this.currentConversation = null;
            }
        },

        /**
         * Archive/Unarchive conversation
         */
        toggleArchive(conversationId) {
            const conversation = this.conversations.find(c => c.id === conversationId);
            if (conversation) {
                conversation.isArchived = !conversation.isArchived;
                conversation.archivedAt = conversation.isArchived ? new Date().toISOString() : null;
            }
        },

        /**
         * Mute/Unmute conversation
         */
        toggleMute(conversationId) {
            const conversation = this.conversations.find(c => c.id === conversationId);
            if (conversation) {
                conversation.isMuted = !conversation.isMuted;
                conversation.mutedUntil = conversation.isMuted ? 'forever' : null;
            }
        },

        /**
         * Mark conversation as read
         */
        markAsRead(conversationId) {
            const conversation = this.conversations.find(c => c.id === conversationId);
            if (conversation) {
                conversation.unreadCount = 0;
                conversation.lastReadAt = new Date().toISOString();
            }
        },

        /**
         * Mark all conversations as read
         */
        markAllAsRead() {
            this.conversations.forEach(conversation => {
                conversation.unreadCount = 0;
                conversation.lastReadAt = new Date().toISOString();
            });
        },

        /**
         * Add participant to conversation
         */
        addParticipant(conversationId, participant) {
            const conversation = this.conversations.find(c => c.id === conversationId);
            if (conversation) {
                if (!conversation.participants) {
                    conversation.participants = [];
                }

                const exists = conversation.participants.some(p => p.userId === participant.userId);
                if (!exists) {
                    conversation.participants.push({
                        ...participant,
                        joinedAt: new Date().toISOString()
                    });
                    conversation.participantCount = conversation.participants.length;
                }
            }
        },

        /**
         * Remove participant from conversation
         */
        removeParticipant(conversationId, userId) {
            const conversation = this.conversations.find(c => c.id === conversationId);
            if (conversation && conversation.participants) {
                conversation.participants = conversation.participants.filter(p => p.userId !== userId);
                conversation.participantCount = conversation.participants.length;
            }
        },

        /**
         * Update participant online status
         */
        updateParticipantStatus(conversationId, userId, isOnline) {
            const conversation = this.conversations.find(c => c.id === conversationId);
            if (conversation && conversation.participants) {
                const participant = conversation.participants.find(p => p.userId === userId);
                if (participant) {
                    participant.isOnline = isOnline;
                    participant.lastSeenAt = isOnline ? null : new Date().toISOString();
                }
            }
        },

        /**
         * Update conversation details
         */
        updateConversationDetails(conversationId, details) {
            const conversation = this.conversations.find(c => c.id === conversationId);
            if (conversation) {
                if (details.name !== undefined) conversation.name = details.name;
                if (details.description !== undefined) conversation.description = details.description;
                if (details.avatarUrl !== undefined) conversation.avatarUrl = details.avatarUrl;
                if (details.type !== undefined) conversation.type = details.type;
                conversation.updatedAt = new Date().toISOString();
            }
        },

        /**
         * Set typing users for a conversation
         */
        setTypingUsers(conversationId, typingUsers) {
            const conversation = this.conversations.find(c => c.id === conversationId);
            if (conversation) {
                conversation.typingUsers = typingUsers;
            }
        },

        /**
         * Set active call status
         */
        setActiveCall(conversationId, callInfo) {
            const conversation = this.conversations.find(c => c.id === conversationId);
            if (conversation) {
                conversation.activeCall = callInfo;
            }
        },

        /**
         * Set search query
         */
        setSearchQuery(query) {
            this.searchQuery = query;
        },

        /**
         * Set filter
         */
        setFilter(filter) {
            this.filter = filter;
        },

        /**
         * Set loading state
         */
        setLoading(loading) {
            this.loading = loading;
        },

        /**
         * Reset pagination
         */
        resetPagination() {
            this.cursor = null;
            this.hasMore = true;
        },

        /**
         * Clear all data
         */
        clearAll() {
            this.conversations = [];
            this.currentConversation = null;
            this.cursor = null;
            this.hasMore = true;
            this.searchQuery = '';
            this.filter = 'all';
            this.loading = false;
        }
    }
})