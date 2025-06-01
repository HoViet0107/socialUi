import { ref, shallowRef, onUnmounted } from 'vue'
import WebSocketService from 'src/services/WebsocketService'
import { useQuasar } from 'quasar'
import { eventBus } from 'src/utils/eventBus'

/**
 * Enhanced WebSocket composable for managing WebSocket connections
 * Provides better connection management and event handling
 */
export function useWebSocket(token) {
  const isConnected = ref(false)
  const connectionError = ref(null)
  const connectionAttempts = ref(0)
  const maxReconnectAttempts = 5
  const reconnectDelay = 3000
  const reconnectTimer = ref(null)
  const handlers = shallowRef({})

  const $q = useQuasar()

  // Connect to WebSocket with automatic reconnection
  const connect = async (authToken = token) => {
    try {
      clearTimeout(reconnectTimer.value)

      // Check if already connected
      if (WebSocketService.isConnected && WebSocketService.isConnected.value === true) {
        isConnected.value = true
        return
      }

      await WebSocketService.connect(authToken)
      isConnected.value = WebSocketService.isConnected.value
      connectionError.value = null
      connectionAttempts.value = 0

      eventBus.emit('ws:connected')

      // Notify only once on reconnect
      if (connectionAttempts.value > 0) {
        $q.notify({
          type: 'positive',
          message: 'Connection restored',
          timeout: 2000,
          position: 'bottom'
        })
      }
    } catch (error) {
      connectionError.value = error.message
      console.error('WebSocket connection failed:', error)

      // Only show notification for first failure
      if (connectionAttempts.value === 0) {
        $q.notify({
          type: 'warning',
          message: 'Connection interrupted',
          caption: 'Attempting to reconnect...',
          timeout: 3000
        })
      }

      // Attempt to reconnect with backoff
      connectionAttempts.value++
      if (connectionAttempts.value <= maxReconnectAttempts) {
        console.log(`Attempting to reconnect (${connectionAttempts.value}/${maxReconnectAttempts})...`)

        // Use exponential backoff for better reconnection strategy
        const delay = Math.min(reconnectDelay * Math.pow(1.5, connectionAttempts.value - 1), 30000)
        reconnectTimer.value = setTimeout(() => connect(authToken), delay)
      } else {
        $q.notify({
          type: 'negative',
          message: 'Could not establish connection',
          caption: 'Please check your network and reload the page',
          timeout: 0 // Persistent notification
        })
      }
    }
  }

  // Disconnect from WebSocket
  const disconnect = () => {
    clearTimeout(reconnectTimer.value)
    WebSocketService.disconnect()
    isConnected.value = false
    eventBus.emit('ws:disconnected')
  }

  // Register event handler with memory of registrations
  const registerHandler = (event, callback) => {
    if (!handlers.value[event]) {
      handlers.value[event] = []
    }

    handlers.value[event].push(callback)
    WebSocketService.registerHandler(event, callback)

    // Return unregister function
    return () => unregisterHandler(event, callback)
  }

  // Unregister event handler
  const unregisterHandler = (event, callback) => {
    if (!handlers.value[event]) return

    const index = handlers.value[event].indexOf(callback)
    if (index !== -1) {
      handlers.value[event].splice(index, 1)
      WebSocketService.unregisterHandler(event, callback)
    }
  }

  // Send message with reconnection attempt
  const sendMessage = (conversationId, content, type = 'TEXT') => {
    if (!isConnected.value) {
      // Attempt to reconnect before sending
      connect()
      throw new Error('Connection lost. Please try again after reconnecting')
    }

    return WebSocketService.sendMessage(conversationId, content, type)
  }

  // Join conversation with event emission
  const joinConversation = (conversationId) => {
    if (!conversationId) return

    try {
      if (isConnected.value) {
        WebSocketService.joinConversation(conversationId)
        eventBus.emit('ws:joined-conversation', { conversationId })
      } else {
        // Try to connect first
        connect().then(() => {
          WebSocketService.joinConversation(conversationId)
          eventBus.emit('ws:joined-conversation', { conversationId })
        })
      }
    } catch (error) {
      console.error('Error joining conversation:', error)
    }
  }

  // Leave conversation with event emission
  const leaveConversation = (conversationId) => {
    if (!conversationId) return

    try {
      if (isConnected.value) {
        WebSocketService.leaveConversation(conversationId)
        eventBus.emit('ws:left-conversation', { conversationId })
      }
    } catch (error) {
      console.error('Error leaving conversation:', error)
    }
  }

  // Get messages for conversation
  const getMessages = (conversationId) => {
    return WebSocketService.messages[conversationId] || []
  }

  // Cleanup on unmount
  onUnmounted(() => {
    // Clear any pending reconnection attempts
    clearTimeout(reconnectTimer.value)

    // Clean up all registered handlers
    Object.keys(handlers.value).forEach(event => {
      handlers.value[event].forEach(callback => {
        WebSocketService.unregisterHandler(event, callback)
      })
    })

    // Don't disconnect here as it might be used by other components
    // disconnect()
  })

  return {
    // State
    isConnected: WebSocketService.isConnected,
    connectionError,
    connectionAttempts,

    // Methods
    connect,
    disconnect,
    sendMessage,
    joinConversation,
    leaveConversation,
    getMessages,
    registerHandler,
    unregisterHandler,

    // Direct access to service data
    messages: WebSocketService.messages,
    onlineUsers: WebSocketService.onlineUsers
  }
}