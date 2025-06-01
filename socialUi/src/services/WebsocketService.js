import { ref } from 'vue'

/**
 * WebSocket Service
 * 
 * Xử lý WebSocket connections và messages,
 * Cung cấp API để gửi tin nhắn, join/leave conversations
 * Tự động reconnect khi bị disconnect
 * 
 * @class
 * @author vuongvu
 * @since 2021-04-15
 * @version 1.0.0
*/

class WebSocketService {
    /**
     * Constructor
     * 
     * @returns {void}
     */
    constructor() {
        this.ws = null
        this.isConnected = ref(false)
        this.onlineUsers = ref([])
        this.reconnectAttempts = 0
        this.maxReconnectAttempts = 5
        this.reconnectTimeout = null
        this.pingInterval = null
        this.token = null
        this.messageHandlers = new Map()
        this.$q = null
        this.conversationStore = null
        this.messageStore = null
    }

    /**
     * Initialize with Quasar and stores
     * @param {Object} options - Initialization options
     * @param {Object} options.quasar - Quasar instance
     * @param {Object} options.conversationStore - Conversation store instance
     * @param {Object} options.messageStore - Message store instance
     */
    initialize({ quasar, conversationStore, messageStore }) {
        if (!quasar || !conversationStore || !messageStore) {
            throw new Error('All parameters are required for WebSocketService initialization');
        }
        this.$q = quasar;
        this.conversationStore = conversationStore;
        this.messageStore = messageStore;
    }


    /**
     * Connect to WebSocket server
     * @param {string} token - JWT token for authentication
     * @returns {Promise<void>}
     */
    async connect(token) {
        if (!token) {
            const error = new Error('Authentication token is required')
            console.error(error.message)
            throw error
        }

        this.token = token

        // Close existing connection if any
        if (this.ws) {
            this.ws.onclose = null
            this.ws.close()
        }

        // Clear any existing reconnect timeout
        if (this.reconnectTimeout) {
            clearTimeout(this.reconnectTimeout)
            this.reconnectTimeout = null
        }

        try {
            // Try different WebSocket URLs
            const wsUrls = [
                `ws://localhost:8080/ws/chat?token=${encodeURIComponent(token)}`,
                `ws://localhost:8080/ws/chat`,  // Without query param, token in header
            ];

            return new Promise((resolve, reject) => {
                let connectionTimeout = null;
                // let currentUrlIndex = 0;

                const tryConnection = (urlIndex) => {
                    if (urlIndex >= wsUrls.length) {
                        reject(new Error('All WebSocket connection attempts failed'));
                        return;
                    }

                    const wsUrl = wsUrls[urlIndex];

                    try {
                        this.ws = new WebSocket(wsUrl);

                        // Set headers if not using query param
                        if (urlIndex === 1) {
                            // Note: WebSocket doesn't support custom headers directly
                            // Server needs to handle token from query param or cookie
                        }

                        connectionTimeout = setTimeout(() => {
                            this.ws?.close();
                            tryConnection(urlIndex + 1);
                        }, 5000); // 5 seconds timeout per attempt

                        this.ws.onopen = () => {
                            if (connectionTimeout) {
                                clearTimeout(connectionTimeout);
                                connectionTimeout = null;
                            }
                            this.isConnected.value = true;
                            this.reconnectAttempts = 0;

                            // Start ping interval
                            this.startPingInterval();

                            // Notify success
                            if (this.$q) {
                                this.$q.notify({
                                    type: 'positive',
                                    message: 'Connected to chat server',
                                    timeout: 2000
                                });
                            }
                            resolve();
                        };

                        this.ws.onerror = (error) => {
                            if (connectionTimeout) {
                                clearTimeout(connectionTimeout);
                                connectionTimeout = null;
                            }
                            this.ws?.close();
                            setTimeout(() => tryConnection(urlIndex + 1), 1000);
                            console.error('WebSocket error:', error);
                        };

                        this.ws.onmessage = (event) => {
                            try {
                                // First, safely parse the JSON
                                let data;
                                try {
                                    data = JSON.parse(event.data);
                                } catch (parseError) {
                                    console.error('Error parsing WebSocket message:', parseError);
                                    return;
                                }

                                // Process the message safely
                                try {
                                    // Check if there's a custom handler registered
                                    const customHandler = this.messageHandlers.get(data.type);
                                    if (customHandler) {
                                        // Use custom handler first
                                        customHandler(data);
                                    }

                                    // Also use default handler
                                    this.handleMessage(data);
                                } catch (handlerError) {
                                    console.error(`Error in ${data.type} message handler:`, handlerError);
                                    console.error('Handler error details:', handlerError.stack);
                                }
                            } catch (error) {
                                console.error('Unhandled error processing WebSocket message:', error);
                            }
                        };

                        this.ws.onclose = (event) => {
                            if (connectionTimeout) {
                                clearTimeout(connectionTimeout);
                                connectionTimeout = null;
                            }
                            this.isConnected.value = false;
                            this.stopPingInterval();
                            if (event.code !== 1000) {
                                this.attemptReconnect();
                            }
                        };

                    } catch (error) {
                        if (connectionTimeout) {
                            clearTimeout(connectionTimeout);
                            connectionTimeout = null;
                        }
                        console.error('Error connecting to WebSocket:', error);
                        tryConnection(urlIndex + 1);
                    }
                };

                tryConnection(0);
            });

        } catch (error) {
            console.error('Error in WebSocket connection:', error);
            this.handleConnectionError(error);
            throw error;
        }
    }

    /**
     * Handle messages from server
     * 
     * @param {Object} data - Data received from server
     * @returns {void}
     */
    handleMessage(data) {
        switch (data.type) {
            case 'CONNECTION_ESTABLISHED':
                break

            case 'NEW_MESSAGE':
                this.handleNewMessage(data)
                break

            case 'MESSAGE_EDITED':
                this.handleMessageEdited(data)
                break

            case 'MESSAGE_DELETED':
                this.handleMessageDeleted(data)
                break

            case 'REACTION_ADDED':
                this.handleReactionAdded(data)
                break

            case 'REACTION_REMOVED':
                this.handleReactionRemoved(data)
                break

            case 'USER_TYPING':
                this.handleTyping(data)
                break

            case 'CONVERSATION_JOINED':
                break

            case 'CONVERSATION_LEFT':
                break

            case 'ERROR':
                // Ignore HEARTBEAT errors as they're not critical
                if (data.message && (data.message.includes('PING') || data.message.includes('HEARTBEAT'))) {
                    console.log('Heartbeat not recognized by server - this is normal');
                } else {
                    console.error('WebSocket error:', data.message);
                }
                break

            default:
                console.error('Unknown message type:', data.type)
        }
    }

    /**
     * Handle new message
     * 
     * @param {Object} data - Data received from server
     * @returns {void}
     */
    handleNewMessage(data) {
        // Validate data structure
        if (!data) {
            console.error('Invalid WebSocket data received in handleNewMessage:', data);
            return;
        }

        // Extract message information - handle both formats:
        // 1. Backend format: {type, messageId, conversationId, content, senderId, ...}
        // 2. Frontend format: {message: {...}, conversationId: ...}

        let message, conversationId;

        if (data.message) {
            // Format is: {message: {...}, conversationId: ...}
            message = data.message;
            conversationId = data.conversationId;
        } else {
            // Format is: {type, messageId, conversationId, content, senderId, ...}
            message = {
                id: data.messageId,
                content: data.content,
                senderId: data.senderId,
                senderName: data.senderName,
                sentAt: data.sendAt,
                mediaUrl: data.medias && data.medias.length > 0 ? data.medias[0].url : null,
                mediaType: data.medias && data.medias.length > 0 ? data.medias[0].type : null,
                replyToMessageId: data.replyToMessageId
            };
            conversationId = data.conversationId;
        }

        // Additional validation
        if (!conversationId) {
            console.error('Missing conversationId in WebSocket data:', data);
            return;
        }

        // Update conversation list
        if (this.conversationStore) {
            try {
                this.conversationStore.updateLastMessage(conversationId, message);
            } catch (error) {
                console.error('Error updating conversation last message:', error);
            }
        }

        // Check if messageStore is properly initialized
        if (!this.messageStore) {
            console.error('WebSocketService: messageStore is not properly initialized!', {
                messageStore: this.messageStore,
                conversationStore: this.conversationStore
            });
            return;
        }

        // Add message to store if viewing this conversation
        if (this.messageStore && this.messageStore.currentConversationId === conversationId) {
            try {
                // Process message to ensure it has all required fields
                const processedMessage = {
                    ...message,
                    // Ensure id exists (try messageId as fallback or generate one)
                    id: message.id || data.messageId || `temp-${Date.now()}-${Math.random()}`,
                    // Ensure content exists
                    content: message.content || data.content || "",
                    // Ensure sentAt exists (try various date fields or use current time)
                    sentAt: message.sentAt || data.sendAt || message.createdAt || new Date().toISOString(),
                    // Ensure sender information exists
                    senderId: message.senderId || data.senderId,
                    senderName: message.senderName || data.senderName || "Unknown",
                    // Ensure conversationId is included in the message
                    conversationId: conversationId
                };

                this.messageStore.addMessage(processedMessage);

                // Emit a custom event that can be listened to for scrolling
                window.dispatchEvent(new CustomEvent('new-message-received', {
                    detail: {
                        conversationId,
                        messageId: processedMessage.id,
                        senderId: processedMessage.senderId,
                        senderName: processedMessage.senderName,
                        content: processedMessage.content,
                        timestamp: Date.now()
                    }
                }));
            } catch (error) {
                console.error('Error adding message to store:', error, message);
            }
        }


        // Show notification if not in current conversation
        if (this.$q && (!this.messageStore || this.messageStore.currentConversationId !== conversationId)) {
            this.$q.notify({
                type: 'info',
                message: `New message from ${message.senderName}`,
                caption: message.content.substring(0, 50) + '...',
                timeout: 3000,
                actions: [
                    {
                        label: 'View', color: 'white', handler: () => {
                            // Navigate to conversation
                            window.location.href = `#/chats?conversation=${conversationId}`
                        }
                    }
                ]
            })
        }
    }

    /**
     * Xử lý tin nhắn được chỉnh sửa
     */
    handleMessageEdited(data) {
        const message = data.message
        const conversationId = data.conversationId

        // Update message in store
        if (this.messageStore && this.messageStore.currentConversationId === conversationId) {
            this.messageStore.updateMessage(message.id, message)
        }
    }

    /**
     * Xử lý tin nhắn bị xóa
     */
    handleMessageDeleted(data) {
        const messageId = data.messageId
        const conversationId = data.conversationId

        // Remove message from store
        if (this.messageStore && this.messageStore.currentConversationId === conversationId) {
            this.messageStore.removeMessage(messageId)
        }
    }

    /**
     * Xử lý reaction được thêm
     */
    handleReactionAdded(data) {
        const { messageId, reaction, conversationId } = data

        // Update message reactions in store
        if (this.messageStore && this.messageStore.currentConversationId === conversationId) {
            this.messageStore.addReaction(messageId, reaction)
        }
    }

    /**
     * Xử lý reaction bị xóa
     */
    handleReactionRemoved(data) {
        const { messageId, userId, conversationId } = data

        // Update message reactions in store
        if (this.messageStore && this.messageStore.currentConversationId === conversationId) {
            this.messageStore.removeReaction(messageId, userId)
        }
    }

    /**
     * Xử lý typing indicator
     */
    handleTyping(data) {
        const { userEmail, isTyping, conversationId } = data

        if (this.messageStore && this.messageStore.currentConversationId === conversationId) {
            if (isTyping) {
                this.messageStore.addTypingUser(userEmail)
            } else {
                this.messageStore.removeTypingUser(userEmail)
            }
        }
    }

    /**
     * Xử lý lỗi từ server
     */
    handleError(data) {
        console.error('WebSocket error:', data)

        if (this.$q) {
            this.$q.notify({
                type: 'negative',
                message: data.message || 'WebSocket error occurred',
                timeout: 5000
            })
        }
    }

    /**
     * Gửi message qua WebSocket
     */
    send(message) {
        if (!this.isConnected.value || !this.ws) {
            console.error('WebSocket is not connected')
            return false
        }

        try {
            this.ws.send(JSON.stringify(message))
            return true
        } catch (error) {
            console.error('Error sending WebSocket message:', error)
            return false
        }
    }

    /**
     * Join conversation
     */
    joinConversation(conversationId) {
        return this.send({
            type: 'JOIN_CONVERSATION',
            conversationId: conversationId
        })
    }

    /**
     * Leave conversation
     */
    leaveConversation(conversationId) {
        return this.send({
            type: 'LEAVE_CONVERSATION',
            conversationId: conversationId
        })
    }

    /**
     * Register custom message handler
     */
    registerHandler(messageType, handler) {
        this.messageHandlers.set(messageType, handler)
    }

    /**
     * Unregister custom message handler
     */
    unregisterHandler(messageType) {
        this.messageHandlers.delete(messageType)
    }

    /**
     * Handle connection errors and attempt to reconnect
     * @param {Error} error - The error that occurred
     */
    handleConnectionError(error) {
        console.error('WebSocket connection error:', error.message)
        this.isConnected.value = false

        // Only show error notification if we're not already trying to reconnect
        if (this.reconnectAttempts === 0 && this.$q) {
            this.$q.notify({
                type: 'warning',
                message: 'Connection lost. Attempting to reconnect...',
                timeout: 3000
            })
        }

        this.attemptReconnect()
    }

    /**
     * Attempt to reconnect to WebSocket server with exponential backoff
     * @returns {void}
     */
    attemptReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            const errorMsg = 'Max reconnection attempts reached. Please refresh the page.'
            console.error(errorMsg)
            if (this.$q) {
                this.$q.notify({
                    type: 'negative',
                    message: errorMsg,
                    timeout: 0, // Don't auto-dismiss
                    actions: [{
                        label: 'Retry Now',
                        color: 'white',
                        handler: () => {
                            this.reconnectAttempts = 0
                            this.connect(this.token).catch(console.error)
                        }
                    }]
                })
            }
            return
        }

        const baseDelay = 1000 // Start with 1 second
        const maxDelay = 30000 // Max 30 seconds
        const jitter = Math.random() * 1000 // Add up to 1 second of jitter
        const delay = Math.min(baseDelay * Math.pow(2, this.reconnectAttempts), maxDelay) + jitter

        this.reconnectTimeout = setTimeout(() => {
            this.reconnectAttempts++
            this.connect(this.token).catch(error => {
                console.error('Reconnection attempt failed:', error)
            })
        }, delay)
    }

    /**
     * Start ping interval to keep connection alive
     */
    startPingInterval() {
        this.pingInterval = setInterval(() => {
            if (this.isConnected.value) {
                // Use HEARTBEAT instead of PING since the server doesn't recognize PING
                this.send({ type: 'HEARTBEAT', timestamp: Date.now() })
            }
        }, 30000) // Ping every 30 seconds
    }

    /**
     * Stop ping interval
     */
    stopPingInterval() {
        if (this.pingInterval) {
            clearInterval(this.pingInterval)
            this.pingInterval = null
        }
    }

    /**
     * Disconnect and cleanup
     */
    disconnect() {
        // Clear reconnect timeout
        if (this.reconnectTimeout) {
            clearTimeout(this.reconnectTimeout)
            this.reconnectTimeout = null
        }

        // Stop ping interval
        this.stopPingInterval()

        // Close WebSocket
        if (this.ws) {
            this.ws.close(1000, 'User disconnected')
            this.ws = null
        }

        // Reset state
        this.isConnected.value = false
        this.reconnectAttempts = 0
        this.token = null
    }

    /**
     * Get connection status
     */
    getStatus() {
        return {
            isConnected: this.isConnected.value,
            reconnectAttempts: this.reconnectAttempts,
            onlineUsers: this.onlineUsers.value
        }
    }
}

// Export singleton instance
export default new WebSocketService()