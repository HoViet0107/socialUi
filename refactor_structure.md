# 🔧 Cấu trúc Refactor - Tách Code Thành Modules

## 📁 **Cấu trúc thư mục mới:**

```
src/
├── composables/
│   ├── chat/
│   │   ├── useChatState.js           # ✅ State management riêng
│   │   ├── useChatActions.js         # ✅ Actions (send, edit, delete)
│   │   ├── useChatNavigation.js      # ✅ Navigation logic
│   │   └── useChatValidation.js      # ✅ Validation logic
│   ├── websocket/
│   │   ├── useWebSocketConnection.js # ✅ Connection management
│   │   ├── useWebSocketHandlers.js   # ✅ Message handlers
│   │   └── useWebSocketEvents.js     # ✅ Event registration
│   └── common/
│       ├── useNotifications.js       # ✅ Đã có - cần extend
│       └── useErrorHandling.js       # ✅ Error handling unified
├── utils/
│   ├── chat/
│   │   ├── messageUtils.js           # ✅ Message processing
│   │   ├── conversationUtils.js      # ✅ Conversation helpers
│   │   └── systemMessageUtils.js     # ✅ System message logic
│   ├── websocket/
│   │   ├── connectionUtils.js        # ✅ Connection helpers
│   │   └── messageQueueUtils.js      # ✅ Message queue logic
│   └── common/
│       ├── dateUtils.js              # ✅ Date formatting
│       ├── validationUtils.js        # ✅ Common validation
│       └── formatUtils.js            # ✅ Text/UI formatting
├── services/
│   ├── websocket/
│   │   ├── WebSocketManager.js       # ✅ Main manager
│   │   ├── ConnectionManager.js      # ✅ Connection only
│   │   ├── MessageHandler.js         # ✅ Message handling
│   │   └── EventBroadcaster.js       # ✅ Event broadcasting
│   └── api/
│       └── [existing API services]
└── stores/
    ├── chat/
    │   ├── messageStore.js           # ✅ Simplified
    │   └── conversationStore.js      # ✅ Simplified
    └── websocket/
        └── connectionStore.js        # ✅ WS connection state
```

---

## 🔍 **Phân tích code issues:**

### **1. ChatsPage.vue - ISSUES:**
- ❌ **600+ lines** - quá dài
- ❌ **Multiple responsibilities**: WebSocket, routing, state, UI
- ❌ **Duplicate handlers**: 3 different WebSocket handlers
- ❌ **Complex onMounted**: 150+ lines initialization
- ❌ **Mixed error handling**: scattered throughout

### **2. WebSocket Services - ISSUES:**
- ❌ **3 WebSocket services** làm same thing
- ❌ **Duplicate connection logic**
- ❌ **Inconsistent error handling**
- ❌ **Mixed responsibilities**

### **3. Stores - ISSUES:**
- ❌ **Overlapping functionality**
- ❌ **Too many responsibilities per store**
- ❌ **Inconsistent data structure**

### **4. Message/Conversation Utils - ISSUES:**
- ❌ **Repeated date formatting** in multiple files
- ❌ **Similar validation logic** scattered
- ❌ **System message detection** duplicated

---

## 🛠️ **REFACTOR PLAN:**

## **Phase 1: Extract Utils & Helpers**

### **1.1 Date & Format Utils**
```javascript
// utils/common/dateUtils.js
export const dateUtils = {
  formatMessageTime: (timestamp) => { /* extracted from messageFormatter */ },
  formatMessageDate: (timestamp) => { /* extracted from messageFormatter */ },
  formatRelativeTime: (timestamp) => { /* extracted from messageFormatter */ },
  shouldShowDateSeparator: (currentMsg, prevMsg) => { /* from MessageList */ },
  shouldGroupWithPrevious: (currentMsg, prevMsg) => { /* from MessageList */ }
}
```

### **1.2 Message Utils**
```javascript
// utils/chat/messageUtils.js
export const messageUtils = {
  isSystemMessage: (message) => { /* from MessageList + ChatsPage */ },
  getSystemMessageIcon: (message) => { /* from MessageList */ },
  processMessage: (rawMessage) => { /* standardize message format */ },
  extractUserNameFromMessage: (message) => { /* from ChatsPage */ },
  validateMessage: (message) => { /* validation logic */ }
}
```

### **1.3 Conversation Utils**
```javascript
// utils/chat/conversationUtils.js
export const conversationUtils = {
  updateLastMessage: (conversation, message) => { /* extract from stores */ },
  getDefaultAvatar: (conversation) => { /* from ConversationList */ },
  canLeaveConversation: (conversation, user) => { /* validation */ },
  formatParticipantCount: (count) => { /* formatting */ }
}
```

---

## **Phase 2: Refactor WebSocket Services**

### **2.1 Unified WebSocket Manager**
```javascript
// services/websocket/WebSocketManager.js
class WebSocketManager {
  constructor() {
    this.connection = new ConnectionManager()
    this.messageHandler = new MessageHandler()
    this.eventBroadcaster = new EventBroadcaster()
  }
  
  async connect(token) { /* unified connection */ }
  registerHandler(type, callback) { /* unified registration */ }
  sendMessage(data) { /* unified sending */ }
}
```

### **2.2 Connection Manager**
```javascript
// services/websocket/ConnectionManager.js
export class ConnectionManager {
  connect(token) { /* pure connection logic */ }
  reconnect() { /* reconnection with backoff */ }
  disconnect() { /* clean disconnect */ }
  ping() { /* heartbeat */ }
}
```

### **2.3 Message Handler**
```javascript
// services/websocket/MessageHandler.js
export class MessageHandler {
  handleNewMessage(data) { /* unified message handling */ }
  handleSystemMessage(data) { /* system message logic */ }
  handleParticipantLeave(data) { /* participant events */ }
  processMessageQueue() { /* queue management */ }
}
```

---

## **Phase 3: Extract Composables**

### **3.1 Chat State Management**
```javascript
// composables/chat/useChatState.js
export function useChatState() {
  const conversations = ref([])
  const selectedConversationId = ref(null)
  const messages = ref([])
  
  // Pure state getters/setters
  const selectedConversation = computed(() => { /* logic */ })
  const hasUnreadMessages = computed(() => { /* logic */ })
  
  return {
    conversations, selectedConversationId, messages,
    selectedConversation, hasUnreadMessages
  }
}
```

### **3.2 Chat Actions**
```javascript
// composables/chat/useChatActions.js
export function useChatActions() {
  const { notify } = useNotifications()
  const { validateMessage } = messageUtils
  
  const sendMessage = async (content, conversationId) => {
    // Pure action logic without side effects
  }
  
  const editMessage = async (messageId, content) => {
    // Pure edit logic
  }
  
  const leaveConversation = async (conversationId) => {
    // Pure leave logic
  }
  
  return { sendMessage, editMessage, leaveConversation }
}
```

### **3.3 WebSocket Connection**
```javascript
// composables/websocket/useWebSocketConnection.js
export function useWebSocketConnection(token) {
  const isConnected = ref(false)
  const connectionError = ref(null)
  
  const connect = async () => {
    // Use ConnectionManager
  }
  
  const disconnect = () => {
    // Clean disconnect
  }
  
  return { isConnected, connectionError, connect, disconnect }
}
```

### **3.4 WebSocket Handlers**
```javascript
// composables/websocket/useWebSocketHandlers.js
export function useWebSocketHandlers() {
  const registerHandlers = (wsManager) => {
    wsManager.registerHandler('NEW_MESSAGE', handleNewMessage)
    wsManager.registerHandler('PARTICIPANTS_LEAVE_CONVERSATION', handleParticipantLeave)
    // ... other handlers
  }
  
  const handleNewMessage = (data) => {
    // Use MessageHandler
  }
  
  const handleParticipantLeave = (data) => {
    // Use systemMessageUtils
  }
  
  return { registerHandlers }
}
```

---

## **Phase 4: Simplified Components**

### **4.1 Refactored ChatsPage.vue**
```vue
<script setup>
// Single responsibility: Coordinate chat UI
import { useChatState } from '@/composables/chat/useChatState'
import { useChatActions } from '@/composables/chat/useChatActions'
import { useWebSocketConnection } from '@/composables/websocket/useWebSocketConnection'
import { useWebSocketHandlers } from '@/composables/websocket/useWebSocketHandlers'
import { useChatNavigation } from '@/composables/chat/useChatNavigation'

const { conversations, selectedConversationId, messages } = useChatState()
const { sendMessage, editMessage, leaveConversation } = useChatActions()
const { connect, disconnect, isConnected } = useWebSocketConnection(token)
const { registerHandlers } = useWebSocketHandlers()
const { selectConversation, handleBack } = useChatNavigation()

// Clean, focused lifecycle
onMounted(async () => {
  await connect()
  registerHandlers(wsManager)
  await loadConversations()
})

onUnmounted(() => {
  disconnect()
})
</script>
```

### **4.2 Simplified MessageList.vue**
```vue
<script setup>
import { dateUtils } from '@/utils/common/dateUtils'
import { messageUtils } from '@/utils/chat/messageUtils'

const { 
  formatDate, formatTime, 
  shouldShowDateSeparator, shouldGroupWithPrevious 
} = dateUtils

const { isSystemMessage, getSystemMessageIcon } = messageUtils

// Remove duplicate date/message logic
// Focus only on UI rendering
</script>
```

---

## **Phase 5: Error Handling & Validation**

### **5.1 Unified Error Handler**
```javascript
// composables/common/useErrorHandling.js
export function useErrorHandling() {
  const { notify } = useNotifications()
  
  const handleChatError = (error, context) => {
    // Standardized error handling for chat
  }
  
  const handleWebSocketError = (error, context) => {
    // Standardized error handling for WebSocket
  }
  
  const handleAPIError = (error, context) => {
    // Standardized error handling for API
  }
  
  return { handleChatError, handleWebSocketError, handleAPIError }
}
```

### **5.2 Validation Utils**
```javascript
// utils/common/validationUtils.js
export const validationUtils = {
  validateMessage: (content) => { /* message validation */ },
  validateConversation: (conversation, user) => { /* conversation access */ },
  validateParticipant: (participant, conversation) => { /* participant validation */ }
}
```

---

## **📊 Expected Results:**

### **Before Refactor:**
- ❌ ChatsPage.vue: **600+ lines**
- ❌ **3 WebSocket services** with duplicated code
- ❌ **Scattered error handling**
- ❌ **Mixed responsibilities**
- ❌ **Hard to test & maintain**

### **After Refactor:**
- ✅ ChatsPage.vue: **~150 lines** (composition focused)
- ✅ **1 unified WebSocket manager** with clear separation
- ✅ **Centralized error handling**
- ✅ **Single responsibility** per module
- ✅ **Easy to test** individual functions
- ✅ **Reusable composables** across components
- ✅ **Consistent data structures**

---

## **🔄 Migration Steps:**

1. **Week 1**: Extract utils & helpers (Phase 1)
2. **Week 2**: Refactor WebSocket services (Phase 2)  
3. **Week 3**: Create composables (Phase 3)
4. **Week 4**: Refactor components (Phase 4)
5. **Week 5**: Add error handling & testing (Phase 5)

## **💡 Key Benefits:**

- **90% code reduction** in main components
- **Reusable logic** across multiple components
- **Easier testing** with isolated functions
- **Better error handling** with consistent patterns
- **Improved maintainability** with clear separation of concerns
- **Type safety** potential with TypeScript migration