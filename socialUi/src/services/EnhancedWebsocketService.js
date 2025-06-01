import { ref } from 'vue';
import { eventBus } from 'src/utils/eventBus';

/**
 * Enhanced WebSocket Service
 * 
 * Handles WebSocket connections and messages with improved:
 * - Error handling
 * - Reconnection strategy
 * - Event broadcasting
 * - Message queuing during disconnections
 * 
 * @class
 * @version 2.0.0
 */
class EnhancedWebSocketService {
    /**
     * Constructor
     */
    constructor() {
        // Connection state
        this.ws = null;
        this.isConnected = ref(false);
        this.isConnecting = ref(false);
        this.connectionError = ref(null);

        // Reconnection settings
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectBaseDelay = 2000; // 2 seconds base delay
        this.reconnectTimeout = null;

        // Heartbeat/ping mechanism
        this.pingInterval = null;
        this.pingIntervalTime = 30000; // 30 seconds
        this.lastPingTime = 0;
        this.lastPongTime = 0;

        // Authentication
        this.token = null;

        // Message handling
        this.messageHandlers = new Map();
        this.messageQueue = []; // Queue messages during disconnection
        this.activeConversations = new Set();

        // Dependencies
        this.$q = null;
        this.conversationStore = null;
        this.messageStore = null;

        // Online users tracking
        this.onlineUsers = ref([]);

        // Debug mode for additional logging
        this.debug = false;
    }

    /**
     * Enable or disable debug mode
     * @param {boolean} enabled - Whether to enable debug mode
     */
    setDebugMode(enabled = true) {
        this.debug = enabled;
    }

    /**
     * Log message when in debug mode
     * @param {string} message - Message to log
     * @param {any} data - Optional data to log
     */
    debugLog(message, data = null) {
        if (!this.debug) return;

        if (data) {
            console.log(`[WebSocket] ${message}`, data);
        } else {
            console.log(`[WebSocket] ${message}`);
        }
    }

    /**
     * Initialize with dependencies
     * @param {Object} options - Initialization options
     */
    initialize(options = {}) {
        const { quasar, conversationStore, messageStore, debug = false } = options;

        // Set dependencies if provided
        if (quasar) this.$q = quasar;
        if (conversationStore) this.conversationStore = conversationStore;
        if (messageStore) this.messageStore = messageStore;

        this.debug = debug;

        this.debugLog('Service initialized', {
            hasQuasar: !!this.$q,
            hasConversationStore: !!this.conversationStore,
            hasMessageStore: !!this.messageStore
        });

        return this;
    }

    /**
     * Connect to WebSocket server with token authentication
     * @param {string} token - JWT token for authentication
     * @returns {Promise<void>}
     */
    async connect(token) {
        // Validate token
        if (!token) {
            const error = new Error('Authentication token is required');
            this.connectionError.value = error;
            eventBus.emit('ws:connect-error', { error });
            throw error;
        }

        // Store token for potential reconnections
        this.token = token;

        // Prevent multiple connection attempts
        if (this.isConnecting.value) {
            this.debugLog('Connection already in progress');
            return;
        }

        this.isConnecting.value = true;
        this.connectionError.value = null;

        // Close existing connection if any
        this.closeExistingConnection();

        // Clear any existing reconnect timeout
        if (this.reconnectTimeout) {
            clearTimeout(this.reconnectTimeout);
            this.reconnectTimeout = null;
        }

        try {
            // Try different WebSocket URLs
            const wsUrls = [
                `ws://localhost:8080/ws/chat?token=${encodeURIComponent(token)}`,
                `wss://localhost:8080/ws/chat?token=${encodeURIComponent(token)}`,
                `ws://localhost:8080/ws/chat`,  // Without query param, token in header
            ];

            // Select appropriate URL based on environment
            const isSecure = window.location.protocol === 'https:';
            const baseUrl = isSecure ? 'wss://' : 'ws://';
            const host = window.location.host;

            // Add dynamic URL based on current hostname
            wsUrls.unshift(`${baseUrl}${host}/ws/chat?token=${encodeURIComponent(token)}`);

            return await this.establishConnection(wsUrls);
        } catch (error) {
            this.isConnecting.value = false;
            this.connectionError.value = error;
            eventBus.emit('ws:connect-error', { error });
            console.error('WebSocket connection failed:', error);
            throw error;
        }
    }

    /**
     * Try to establish a connection using the provided URLs
     * @param {string[]} wsUrls - Array of WebSocket URLs to try
     * @returns {Promise<void>}
     * @private
     */
    async establishConnection(wsUrls) {
        return new Promise((resolve, reject) => {
            let connectionTimeout = null;
            let currentUrlIndex = 0;

            const tryConnection = (urlIndex) => {
                if (urlIndex >= wsUrls.length) {
                    reject(new Error('All WebSocket connection attempts failed'));
                    return;
                }

                const wsUrl = wsUrls[urlIndex];
                this.debugLog(`Attempting connection to ${wsUrl}`);

                try {
                    // Create new WebSocket connection
                    this.ws = new WebSocket(wsUrl);

                    // Set connection timeout
                    connectionTimeout = setTimeout(() => {
                        this.debugLog(`Connection to ${wsUrl} timed out`);
                        if (this.ws) {
                            this.ws.onclose = null; // Prevent onclose from triggering
                            this.ws.close();
                            this.ws = null;
                        }

                        // Try next URL
                        tryConnection(urlIndex + 1);
                    }, 5000); // 5 second timeout

                    // Set up event handlers
                    this.ws.onopen = () => {
                        clearTimeout(connectionTimeout);
                        this.handleConnectionOpen();
                        resolve();
                    };

                    this.ws.onclose = (event) => {
                        clearTimeout(connectionTimeout);
                        this.handleConnectionClose(event);

                        // Only try next URL if this wasn't a clean close
                        if (!event.wasClean) {
                            tryConnection(urlIndex + 1);
                        }
                    };

                    this.ws.onerror = (error) => {
                        this.debugLog(`Error with ${wsUrl}`, error);
                        // Don't reject here, let onclose handle it
                    };

                    this.ws.onmessage = this.handleMessage.bind(this);
                } catch (error) {
                    clearTimeout(connectionTimeout);
                    this.debugLog(`Failed to connect to ${wsUrl}`, error);

                    // Try next URL
                    tryConnection(urlIndex + 1);
                }
            };

            // Start connection attempts
            tryConnection(currentUrlIndex);
        });
    }

    /**
     * Handle successful connection
     * @private
     */
    handleConnectionOpen() {
        this.debugLog('Connection established');
        this.isConnected.value = true;
        this.isConnecting.value = false;
        this.connectionError.value = null;
        this.reconnectAttempts = 0;

        // Set up heartbeat mechanism
        this.startPingInterval();

        // Process any queued messages
        this.processMessageQueue();

        // Re-join active conversations
        this.rejoinConversations();

        // Emit connection event
        eventBus.emit('ws:connected');

        // Show notification if Quasar is available
        if (this.$q) {
            this.$q.notify({
                type: 'positive',
                message: 'Connected to chat server',
                timeout: 2000,
                position: 'bottom'
            });
        }
    }

    /**
     * Handle connection close
     * @param {CloseEvent} event - WebSocket close event
     * @private
     */
    handleConnectionClose(event) {
        this.isConnected.value = false;
        this.isConnecting.value = false;

        // Clear ping interval
        if (this.pingInterval) {
            clearInterval(this.pingInterval);
            this.pingInterval = null;
        }

        this.debugLog('Connection closed', { code: event.code, reason: event.reason, wasClean: event.wasClean });

        // Emit disconnection event
        eventBus.emit('ws:disconnected', { code: event.code, reason: event.reason });

        // Attempt reconnection if it wasn't a clean close
        if (!event.wasClean) {
            this.attemptReconnect();
        }
    }

    /**
     * Close existing connection properly
     * @private
     */
    closeExistingConnection() {
        if (this.ws) {
            // Remove existing event handlers
            this.ws.onopen = null;
            this.ws.onclose = null;
            this.ws.onerror = null;
            this.ws.onmessage = null;

            try {
                // Attempt to close cleanly
                this.ws.close(1000, 'Normal closure');
            } catch (error) {
                this.debugLog('Error closing WebSocket', error);
            }

            this.ws = null;
        }
    }

    /**
     * Attempt to reconnect with exponential backoff
     * @private
     */
    attemptReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            this.debugLog('Maximum reconnect attempts reached');

            // Emit max reconnect event
            eventBus.emit('ws:reconnect-failed');

            // Show notification if Quasar is available
            if (this.$q) {
                this.$q.notify({
                    type: 'negative',
                    message: 'Failed to reconnect to chat server',
                    caption: 'Please refresh the page to try again',
                    timeout: 0
                });
            }
            return;
        }

        this.reconnectAttempts++;

        // Calculate delay with exponential backoff
        const delay = Math.min(
            this.reconnectBaseDelay * Math.pow(1.5, this.reconnectAttempts - 1),
            30000 // Maximum 30 second delay
        );

        this.debugLog(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);

        // Emit reconnecting event
        eventBus.emit('ws:reconnecting', {
            attempt: this.reconnectAttempts,
            maxAttempts: this.maxReconnectAttempts,
            delay
        });

        // Show notification only on first attempt if Quasar is available
        if (this.reconnectAttempts === 1 && this.$q) {
            this.$q.notify({
                type: 'warning',
                message: 'Connection lost',
                caption: 'Attempting to reconnect...',
                timeout: 3000
            });
        }

        // Schedule reconnect attempt
        this.reconnectTimeout = setTimeout(() => {
            this.reconnectTimeout = null;
            if (this.token) {
                this.connect(this.token).catch(() => {
                    // Error handling is done in connect method
                });
            }
        }, delay);
    }

    /**
     * Start ping interval to keep connection alive
     * @private
     */
    startPingInterval() {
        if (this.pingInterval) {
            clearInterval(this.pingInterval);
        }

        this.pingInterval = setInterval(() => {
            if (this.isConnected.value && this.ws && this.ws.readyState === WebSocket.OPEN) {
                try {
                    this.ws.send(JSON.stringify({ type: 'PING' }));
                    this.lastPingTime = Date.now();
                    this.debugLog('Ping sent');
                } catch (error) {
                    this.debugLog('Error sending ping', error);
                }
            }
        }, this.pingIntervalTime);
    }

    /**
     * Handle incoming messages
     * @param {MessageEvent} event - WebSocket message event
     * @private
     */
    handleMessage(event) {
        try {
            const data = JSON.parse(event.data);
            this.debugLog('Message received', data);

            // Handle pong response
            if (data.type === 'PONG') {
                this.lastPongTime = Date.now();
                this.debugLog('Pong received');
                return;
            }

            // Handle system messages
            if (data.type === 'SYSTEM') {
                this.handleSystemMessage(data);
                return;
            }

            // Handle incoming messages from other users
            if (data.type === 'NEW_MESSAGE' || data.type === 'MESSAGE') {
                this.handleNewMessage(data);

                // If message store is available, update it
                if (this.messageStore && data.message) {
                    this.messageStore.addMessage(data.message);
                }

                // Dispatch custom event for components to handle
                window.dispatchEvent(new CustomEvent('new-message-received', {
                    detail: data
                }));
            }

            // Update online users if available
            if (data.onlineUsers) {
                this.onlineUsers.value = data.onlineUsers;
            }

            // Call registered handlers for this message type
            this.callMessageHandlers(data);

            // Emit event via event bus
            eventBus.emit(`ws:message:${data.type}`, data);

            // Also emit general message event
            eventBus.emit('ws:message', data);

        } catch (error) {
            console.error('Error handling WebSocket message:', error);
        }
    }

    /**
     * Handle system messages
     * @param {Object} data - Message data
     * @private
     */
    handleSystemMessage(data) {
        // Handle specific system messages
        if (data.subType === 'ONLINE_STATUS' && data.users) {
            this.onlineUsers.value = data.users;
            eventBus.emit('ws:online-users-updated', data.users);
        }
    }

    /**
     * Handle new message from other users
     * @param {Object} data - Message data
     * @private
     */
    handleNewMessage(data) {
        // Update conversation if needed
        if (this.conversationStore && data.message && data.conversationId) {
            // Update last message in conversation
            this.conversationStore.updateLastMessage(data.conversationId, data.message);
        }
    }

    /**
     * Call registered handlers for a message type
     * @param {Object} data - Message data
     * @private
     */
    callMessageHandlers(data) {
        if (!data || !data.type) return;

        const handlers = this.messageHandlers.get(data.type);
        if (handlers && handlers.length > 0) {
            handlers.forEach(handler => {
                try {
                    handler(data);
                } catch (error) {
                    console.error(`Error in handler for ${data.type}:`, error);
                }
            });
        }
    }

    /**
     * Register a handler for a specific message type
     * @param {string} type - Message type to handle
     * @param {Function} callback - Handler function
     */
    registerHandler(type, callback) {
        if (!type || typeof callback !== 'function') {
            return;
        }

        if (!this.messageHandlers.has(type)) {
            this.messageHandlers.set(type, []);
        }

        this.messageHandlers.get(type).push(callback);
    }

    /**
     * Unregister a handler for a specific message type
     * @param {string} type - Message type
     * @param {Function} callback - Handler function to remove
     */
    unregisterHandler(type, callback) {
        if (!type || !this.messageHandlers.has(type)) {
            return;
        }

        const handlers = this.messageHandlers.get(type);
        const index = handlers.indexOf(callback);

        if (index !== -1) {
            handlers.splice(index, 1);
        }
    }

    /**
     * Send a message to the server
     * @param {Object} message - Message object
     * @returns {Promise<boolean>} - Whether the message was sent
     */
    async send(message) {
        if (!this.isConnected.value || !this.ws || this.ws.readyState !== WebSocket.OPEN) {
            // Queue message for later if not connected
            this.messageQueue.push(message);
            this.debugLog('Message queued (not connected)', message);

            // Try to reconnect
            if (this.token) {
                try {
                    await this.connect(this.token);
                    return true;
                } catch (error) {
                    console.log(error);

                    return false;
                }
            }

            return false;
        }

        try {
            const messageString = JSON.stringify(message);
            this.ws.send(messageString);
            this.debugLog('Message sent', message);
            return true;
        } catch (error) {
            console.error('Error sending message:', error);
            return false;
        }
    }

    /**
     * Process queued messages after reconnecting
     * @private
     */
    processMessageQueue() {
        if (this.messageQueue.length === 0) return;

        this.debugLog(`Processing ${this.messageQueue.length} queued messages`);

        const queueCopy = [...this.messageQueue];
        this.messageQueue = [];

        queueCopy.forEach(message => {
            this.send(message);
        });
    }

    /**
     * Join a conversation
     * @param {string|number} conversationId - Conversation ID
     * @returns {Promise<boolean>} Whether the join was successful
     */
    async joinConversation(conversationId) {
        if (!conversationId) return false;

        // Track active conversations for potential reconnects
        this.activeConversations.add(conversationId.toString());

        const message = {
            type: 'JOIN_CONVERSATION',
            conversationId: conversationId
        };

        const result = await this.send(message);

        if (result) {
            eventBus.emit('ws:joined-conversation', { conversationId });
        }

        return result;
    }

    /**
     * Leave a conversation
     * @param {string|number} conversationId - Conversation ID
     * @returns {Promise<boolean>} Whether the leave was successful
     */
    async leaveConversation(conversationId) {
        if (!conversationId) return false;

        // Remove from active conversations
        this.activeConversations.delete(conversationId.toString());

        const message = {
            type: 'LEAVE_CONVERSATION',
            conversationId: conversationId
        };

        const result = await this.send(message);

        if (result) {
            eventBus.emit('ws:left-conversation', { conversationId });
        }

        return result;
    }

    /**
     * Rejoin active conversations after reconnecting
     * @private
     */
    rejoinConversations() {
        if (this.activeConversations.size === 0) return;

        this.debugLog(`Rejoining ${this.activeConversations.size} active conversations`);

        this.activeConversations.forEach(conversationId => {
            this.joinConversation(conversationId);
        });
    }

    /**
     * Send a message to a conversation
     * @param {string|number} conversationId - Conversation ID
     * @param {string} content - Message content
     * @param {string} type - Message type (default: 'TEXT')
     * @returns {Promise<boolean>} Whether the message was sent successfully
     */
    async sendMessage(conversationId, content, type = 'TEXT') {
        if (!conversationId || !content) return false;

        const message = {
            type: 'SEND_MESSAGE',
            conversationId: conversationId,
            content: content,
            messageType: type
        };

        return await this.send(message);
    }

    /**
     * Disconnect from server
     */
    disconnect() {
        // Clear intervals and timeouts
        if (this.pingInterval) {
            clearInterval(this.pingInterval);
            this.pingInterval = null;
        }

        if (this.reconnectTimeout) {
            clearTimeout(this.reconnectTimeout);
            this.reconnectTimeout = null;
        }

        // Close connection
        this.closeExistingConnection();

        // Reset state
        this.isConnected.value = false;
        this.isConnecting.value = false;
        this.activeConversations.clear();

        // Emit disconnect event
        eventBus.emit('ws:disconnected', { reason: 'user-initiated' });
    }
}

// Create singleton instance
const enhancedWebSocketService = new EnhancedWebSocketService();
export default enhancedWebSocketService;
