import { defineStore } from 'pinia'

export const useMessageStore = defineStore('message', {
    state: () => ({
        messages: [],
        currentConversationId: null,
        typingUsers: new Set(),
        loading: false,
        hasMore: true,
        cursor: null,
        isInitialized: false
    }),

    getters: {
        /**
         * Get sorted messages (newest last)
         */
        sortedMessages: (state) => {
            // Sắp xếp tin nhắn theo thời gian, đảm bảo nhận diện cả sendAt và sentAt
            return [...state.messages]
                .filter(msg => msg && (msg.sendAt || msg.sentAt)) // Lọc bỏ tin nhắn không có thời gian
                .sort((a, b) => {
                    const timeA = new Date(a.sentAt || a.sendAt);
                    const timeB = new Date(b.sentAt || b.sendAt);
                    return timeA - timeB; // Từ cũ đến mới (tin mới nhất ở cuối)
                });
        },

        /**
         * Get messages in display order (newest last)
         */
        displayMessages: (state) => {
            return state.sortedMessages;
        },

        typingUsersList: (state) => {
            return Array.from(state.typingUsers)
        }
    },

    actions: {
        /**
         * Initialize the store
         */
        initialize() {
            if (this.isInitialized) return;
            this.isInitialized = true;
            this.messages = [];
            this.currentConversationId = null;
            this.typingUsers = new Set();
            this.loading = false;
            this.hasMore = true;
            this.cursor = null;
        },

        /**
         * Set current conversation
         */
        setCurrentConversation(conversationId) {
            this.currentConversationId = conversationId
            this.messages = []
            this.typingUsers.clear()
            this.cursor = null
            this.hasMore = true
        },

        /**
         * Add new message
         */
        addMessage(message) {
            if (!message) {
                console.warn('Empty message provided to addMessage');
                return;
            }

            const messageId = message.id || message.messageId || `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
            const existingIndex = this.messages.findIndex(m => m.id === messageId || m.messageId === messageId);

            if (existingIndex === -1) {
                this.messages.push({
                    ...message,
                    id: messageId,
                    sendAt: message.sendAt || new Date().toISOString(),
                    content: message.content || '',
                    senderId: message.senderId || null,
                    senderName: message.senderName || 'Unknown'
                });
            } else {
                this.messages[existingIndex] = {
                    ...this.messages[existingIndex],
                    ...message,
                    id: messageId
                };
            }
        },

        /**
         * Add multiple messages (for initial load or load more)
         */
        /**
         * Add older messages to the beginning of the messages array
         * @param {Array} messages - Messages to prepend
         * @param {string} cursor - Pagination cursor
         * @param {boolean} hasMore - Whether there are more messages to load
         */
        prependMessages(messages, cursor, hasMore) {
            if (!Array.isArray(messages) || messages.length === 0) {
                this.hasMore = hasMore;
                return;
            }

            // Đảm bảo tất cả tin nhắn đều có id và sentAt
            const mappedMessages = messages.map(msg => ({
                ...msg,
                id: msg.id || msg.messageId || Date.now() + Math.random(),
                sentAt: msg.sendAt || msg.createdAt
            }));

            // Get existing message IDs to avoid duplicates
            const existingIds = new Set(this.messages.map(m => m.id || m.messageId));

            // Filter out messages that already exist
            const messagesToAdd = mappedMessages.filter(msg => {
                const messageId = msg.id || msg.messageId;
                return messageId && !existingIds.has(messageId);
            });

            if (messagesToAdd.length === 0) {
                this.hasMore = hasMore;
                return;
            }

            // Preserve current messages to maintain scroll position
            const currentMessages = [...this.messages];

            // Prepend new messages to the beginning of the array
            this.messages = [...messagesToAdd, ...currentMessages];
            this.cursor = cursor;
            this.hasMore = Boolean(hasMore);

        },

        /**
         * Add new messages to the end of the messages array
         */
        addMessages(messages, cursor, hasMore) {
            // Safety check for null messages
            if (!Array.isArray(messages) || messages.length === 0) {
                console.warn('MessageStore: No valid messages to add');
                if (hasMore !== undefined) this.hasMore = hasMore;
                if (cursor !== undefined) this.cursor = cursor;
                return;
            }

            // Process messages to ensure they have required fields
            const processedMessages = messages.map(msg => ({
                ...msg,
                id: msg.id || msg.messageId || `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
                sentAt: msg.sentAt || msg.sendAt || msg.createdAt || new Date().toISOString(),
                content: msg.content || '',
                senderId: msg.senderId || null,
                senderName: msg.senderName || 'Unknown'
            }));

            // Filter out duplicates
            const existingIds = new Set(this.messages.map(m => m.id || m.messageId));
            const messagesToAdd = processedMessages.filter(msg => {
                const messageId = msg.id || msg.messageId;
                return messageId && !existingIds.has(messageId);
            });

            if (messagesToAdd.length === 0) {
                if (hasMore !== undefined) this.hasMore = hasMore;
                if (cursor !== undefined) this.cursor = cursor;
                return;
            }

            // Add new messages to the end of the array
            this.messages = [...this.messages, ...messagesToAdd];

            // Update cursor and hasMore state
            if (cursor !== undefined) this.cursor = cursor;
            if (hasMore !== undefined) this.hasMore = hasMore;
        },



        /**
         * Sort messages by timestamp
         */
        sortMessages() {
            this.messages.sort((a, b) =>
                new Date(a.sendAt || a.sentAt) - new Date(b.sendAt || b.sentAt)
            );
        },

        /**
         * Update existing message
         */
        updateMessage(messageId, updatedMessage) {
            // Tìm tin nhắn bằng id hoặc messageId
            let index = this.messages.findIndex(m => m.id === messageId);

            // Nếu không tìm thấy bằng id, thử tìm bằng messageId
            if (index === -1 && messageId) {
                index = this.messages.findIndex(m => m.messageId === messageId);
            }

            if (index !== -1) {
                // Đảm bảo các trường quan trọng luôn tồn tại
                this.messages[index] = {
                    ...this.messages[index],
                    ...updatedMessage,
                    id: updatedMessage.id || this.messages[index].id || updatedMessage.messageId,
                    sentAt: updatedMessage.sentAt || this.messages[index].sentAt || updatedMessage.sendAt
                }
            }
        },

        /**
         * Update only the content of a message
         * @param {string|number} messageId - The ID of the message to update
         * @param {string} newContent - The new content for the message
         */
        updateMessageContent(messageId, newContent) {
            // Find message index by id or messageId
            let index = this.messages.findIndex(m => m.id === messageId);

            // If not found by id, try with messageId
            if (index === -1 && messageId) {
                index = this.messages.findIndex(m => m.messageId === messageId);
            }

            if (index !== -1) {
                // Update only the content field
                this.messages[index] = {
                    ...this.messages[index],
                    content: newContent,
                    edited: true, // Optionally mark as edited
                    editedAt: new Date().toISOString() // Optionally add edit timestamp
                };
            }
        },

        /**
         * Remove message
         */
        removeMessage(messageId) {
            const index = this.messages.findIndex(m => m.id === messageId)
            if (index !== -1) {
                // Mark as deleted instead of removing
                this.messages[index].isDeleted = true
                this.messages[index].content = 'This message has been deleted'
            }
        },

        /**
         * Add reaction to message
         */
        addReaction(messageId, reaction) {
            const message = this.messages.find(m => m.id === messageId)
            if (message) {
                if (!message.reactions) {
                    message.reactions = []
                }

                // Check if user already reacted
                const existingIndex = message.reactions.findIndex(
                    r => r.userId === reaction.userId
                )

                if (existingIndex !== -1) {
                    // Update existing reaction
                    message.reactions[existingIndex] = reaction
                } else {
                    // Add new reaction
                    message.reactions.push(reaction)
                }
            }
        },

        /**
         * Remove reaction from message
         */
        removeReaction(messageId, userId) {
            const message = this.messages.find(m => m.id === messageId)
            if (message && message.reactions) {
                message.reactions = message.reactions.filter(
                    r => r.userId !== userId
                )
            }
        },

        /**
         * Add typing user
         */
        addTypingUser(userEmail) {
            this.typingUsers.add(userEmail)

            // Auto remove after 3 seconds if no update
            setTimeout(() => {
                this.typingUsers.delete(userEmail)
            }, 3000)
        },

        /**
         * Remove typing user
         */
        removeTypingUser(userEmail) {
            this.typingUsers.delete(userEmail)
        },

        /**
         * Clear all messages
         */
        clearMessages() {
            this.messages = []
            this.typingUsers.clear()
            this.cursor = null
            this.hasMore = true
        },

        /**
         * Set loading state
         */
        setLoading(loading) {
            this.loading = loading
        }
    }
})