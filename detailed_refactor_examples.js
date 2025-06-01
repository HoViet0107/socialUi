// ============================================
// 1. EXTRACT DATE UTILS (from multiple files)
// ============================================

// utils/common/dateUtils.js
import { format, formatDistanceToNow, isToday, isYesterday, differenceInMinutes } from 'date-fns';

export const dateUtils = {
  // ✅ EXTRACTED from messageFormatter.js + MessageList.vue + ConversationList.vue
  formatMessageTime(timestamp, options = {}) {
    if (!timestamp) return '';
    const date = timestamp instanceof Date ? timestamp : new Date(timestamp);
    const { showSeconds = false } = options;
    const timeFormat = showSeconds ? 'h:mm:ss a' : 'h:mm a';
    return format(date, timeFormat);
  },

  formatMessageDate(timestamp) {
    if (!timestamp) return '';
    const date = timestamp instanceof Date ? timestamp : new Date(timestamp);
    
    if (isToday(date)) return 'Today';
    if (isYesterday(date)) return 'Yesterday';
    return format(date, 'MMMM d, yyyy');
  },

  formatRelativeTime(timestamp) {
    if (!timestamp) return '';
    const date = timestamp instanceof Date ? timestamp : new Date(timestamp);
    return formatDistanceToNow(date, { addSuffix: true });
  },

  // ✅ EXTRACTED from MessageList.vue
  shouldShowDateSeparator(currentMessage, previousMessage, index) {
    if (index === 0) return true;
    if (!currentMessage || !previousMessage) return false;
    
    const currentDate = new Date(currentMessage.sentAt).toDateString();
    const previousDate = new Date(previousMessage.sentAt).toDateString();
    return currentDate !== previousDate;
  },

  // ✅ EXTRACTED from MessageList.vue
  shouldGroupWithPrevious(currentMessage, previousMessage, index) {
    if (index === 0 || !currentMessage || !previousMessage) return false;
    
    return currentMessage.senderId === previousMessage.senderId &&
      differenceInMinutes(new Date(currentMessage.sentAt), new Date(previousMessage.sentAt)) < 5;
  },

  // ✅ EXTRACTED from ConversationList.vue
  formatConversationTime(timestamp) {
    if (!timestamp) return '';
    
    const date = new Date(timestamp);
    const now = new Date();
    const diffInHours = (now - date) / (1000 * 60 * 60);
    
    if (diffInHours < 24) {
      return date.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
      });
    } else if (diffInHours < 168) { // 7 days
      return date.toLocaleDateString('en-US', { weekday: 'short' });
    } else {
      return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric'
      });
    }
  }
};

// ============================================
// 2. EXTRACT MESSAGE UTILS (from multiple files)
// ============================================

// utils/chat/messageUtils.js
export const messageUtils = {
  // ✅ EXTRACTED from MessageList.vue + ChatsPage.vue + WebSocketService.js
  isSystemMessage(message) {
    return (
      message.isSystem === true ||
      message.senderName === 'System' ||
      message.type === 'SYSTEM' ||
      message.senderId === null ||
      this.isSystemMessageContent(message.content)
    );
  },

  // ✅ EXTRACTED from MessageList.vue
  isSystemMessageContent(content) {
    if (!content) return false;
    
    const systemPatterns = [
      /left the group!?$/i,
      /created the group!?$/i,
      /added .+ to the group!?$/i,
      /removed .+ from the group!?$/i,
      /changed the conversation/i,
      /Group has been deleted/i,
      /joined the group!?$/i,
      /became admin/i,
      /is no longer admin/i
    ];
    
    return systemPatterns.some(pattern => pattern.test(content.trim()));
  },

  // ✅ EXTRACTED from MessageList.vue + ChatsPage.vue
  getSystemMessageIcon(message) {
    const content = message.content?.toLowerCase() || '';
    
    if (content.includes('left') || content.includes('removed')) {
      return 'person_remove';
    } else if (content.includes('added') || content.includes('joined')) {
      return 'person_add';
    } else if (content.includes('created')) {
      return 'group_add';
    } else if (content.includes('deleted')) {
      return 'delete';
    } else if (content.includes('changed') || content.includes('updated')) {
      return 'edit';
    } else if (content.includes('admin')) {
      return 'admin_panel_settings';
    } else {
      return 'info';
    }
  },

  // ✅ EXTRACTED from multiple WebSocket handlers
  processMessage(rawMessage, conversationId = null) {
    return {
      id: rawMessage.id || rawMessage.messageId || `temp-${Date.now()}-${Math.random()}`,
      content: rawMessage.content || '',
      sentAt: rawMessage.sentAt || rawMessage.sendAt || rawMessage.createdAt || new Date().toISOString(),
      senderId: rawMessage.senderId || rawMessage.sender?.id,
      senderName: rawMessage.senderName || rawMessage.sender?.name || 'Unknown',
      conversationId: conversationId || rawMessage.conversationId,
      isSystem: this.isSystemMessage(rawMessage),
      type: rawMessage.type || 'TEXT',
      edited: rawMessage.edited || false,
      editedAt: rawMessage.editedAt || null
    };
  },

  // ✅ EXTRACTED from ChatsPage.vue
  extractUserNameFromLeaveMessage(message) {
    const match = message.match(/^(.+)\s+left\s+the\s+group!?$/i);
    return match ? match[1].trim() : 'Someone';
  },

  // ✅ EXTRACTED from MessageList.vue
  getAvatarUrl(name) {
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(name || 'User')}&background=random`;
  },

  // ✅ EXTRACTED from MessageList.vue
  getStatusIcon(status) {
    switch (status) {
      case 'SENDING': return 'schedule';
      case 'SENT': return 'done';
      case 'DELIVERED': return 'done_all';
      case 'READ': return 'done_all';
      default: return 'error';
    }
  }
};

// ============================================
// 3. UNIFIED WEBSOCKET MANAGER
// ============================================

// services/websocket/WebSocketManager.js
import { ConnectionManager } from './ConnectionManager.js';
import { MessageHandler } from './MessageHandler.js';
import { EventBroadcaster } from './EventBroadcaster.js';

export class WebSocketManager {
  constructor() {
    this.connection = new ConnectionManager();
    this.messageHandler = new MessageHandler();
    this.eventBroadcaster = new EventBroadcaster();
    this.handlers = new Map();
  }

  async connect(token) {
    return this.connection.connect(token, {
      onOpen: () => this.eventBroadcaster.emit('connected'),
      onClose: () => this.eventBroadcaster.emit('disconnected'),
      onMessage: (data) => this.handleMessage(data),
      onError: (error) => this.eventBroadcaster.emit('error', error)
    });
  }

  disconnect() {
    this.connection.disconnect();
  }

  registerHandler(type, callback) {
    if (!this.handlers.has(type)) {
      this.handlers.set(type, []);
    }
    this.handlers.get(type).push(callback);
  }

  handleMessage(data) {
    // Use unified message handler
    this.messageHandler.handle(data, this.handlers);
  }

  send(data) {
    return this.connection.send(data);
  }

  joinConversation(conversationId) {
    return this.send({
      type: 'JOIN_CONVERSATION',
      conversationId
    });
  }

  leaveConversation(conversationId) {
    return this.send({
      type: 'LEAVE_CONVERSATION',
      conversationId
    });
  }
}

// ============================================
// 4. CHAT STATE COMPOSABLE
// ============================================

// composables/chat/useChatState.js
import { ref, computed } from 'vue';

export function useChatState() {
  // State
  const conversations = ref([]);
  const selectedConversationId = ref(null);
  const messages = ref([]);
  const loading = ref({
    conversations: false,
    messages: false
  });

  // Computed
  const selectedConversation = computed(() => {
    return conversations.value.find(c => c.id === selectedConversationId.value);
  });

  const unreadCount = computed(() => {
    return conversations.value.reduce((count, conv) => {
      return count + (conv.unreadCount || 0);
    }, 0);
  });

  const sortedMessages = computed(() => {
    return [...messages.value].sort((a, b) => {
      const timeA = new Date(a.sentAt || a.sendAt);
      const timeB = new Date(b.sentAt || b.sendAt);
      return timeA - timeB;
    });
  });

  // Actions
  const setSelectedConversation = (conversationId) => {
    selectedConversationId.value = conversationId;
  };

  const addMessage = (message) => {
    const existingIndex = messages.value.findIndex(m => m.id === message.id);
    if (existingIndex === -1) {
      messages.value.push(message);
    } else {
      messages.value[existingIndex] = { ...messages.value[existingIndex], ...message };
    }
  };

  const updateConversation = (conversationId, updates) => {
    const index = conversations.value.findIndex(c => c.id === conversationId);
    if (index !== -1) {
      conversations.value[index] = { ...conversations.value[index], ...updates };
    }
  };

  const removeConversation = (conversationId) => {
    conversations.value = conversations.value.filter(c => c.id !== conversationId);
  };

  return {
    // State
    conversations,
    selectedConversationId,
    messages,
    loading,

    // Computed
    selectedConversation,
    unreadCount,
    sortedMessages,

    // Actions
    setSelectedConversation,
    addMessage,
    updateConversation,
    removeConversation
  };
}

// ============================================
// 5. CHAT ACTIONS COMPOSABLE
// ============================================

// composables/chat/useChatActions.js
import { useErrorHandling } from '@/composables/common/useErrorHandling';
import { useNotifications } from '@/composables/common/useNotifications';
import { ConversationServices, MessageServices } from '@/services/api';

export function useChatActions(token) {
  const { handleError } = useErrorHandling();
  const { success, error: showError } = useNotifications();

  const sendMessage = async (conversationId, content, type = 'TEXT') => {
    try {
      const formData = new FormData();
      formData.append('content', content.trim());
      formData.append('status', 'SENT');
      formData.append('mediaType', type);

      const response = await MessageServices.sendMessage(conversationId, formData, token);
      
      success('Message sent');
      return response.data;
    } catch (err) {
      const errorMessage = handleError(err, 'send_message');
      showError('Failed to send message', errorMessage);
      throw err;
    }
  };

  const editMessage = async (messageId, newContent) => {
    try {
      await MessageServices.editMessage(messageId, newContent, token);
      success('Message edited successfully');
      return true;
    } catch (err) {
      const errorMessage = handleError(err, 'edit_message');
      showError('Failed to edit message', errorMessage);
      throw err;
    }
  };

  const leaveConversation = async (conversationId) => {
    try {
      const response = await ConversationServices.leaveConversation(conversationId, token);
      success('You have left the conversation');
      return response.data;
    } catch (err) {
      const errorMessage = handleError(err, 'leave_conversation');
      showError('Failed to leave conversation', errorMessage);
      throw err;
    }
  };

  return {
    sendMessage,
    editMessage,
    leaveConversation
  };
}

// ============================================
// 6. REFACTORED CHATSPAGE.VUE
// ============================================

/*
<script setup>
// ✅ CLEAN IMPORTS - single responsibility per composable
import { useChatState } from '@/composables/chat/useChatState';
import { useChatActions } from '@/composables/chat/useChatActions';
import { useWebSocketConnection } from '@/composables/websocket/useWebSocketConnection';
import { useWebSocketHandlers } from '@/composables/websocket/useWebSocketHandlers';

const token = getAuthToken();

// ✅ CLEAN STATE MANAGEMENT
const {
  conversations,
  selectedConversationId,
  messages,
  sortedMessages,
  selectedConversation,
  setSelectedConversation,
  addMessage
} = useChatState();

// ✅ CLEAN ACTIONS
const {
  sendMessage,
  editMessage,
  leaveConversation
} = useChatActions(token);

// ✅ CLEAN WEBSOCKET
const {
  connect,
  disconnect,
  isConnected
} = useWebSocketConnection(token);

const {
  registerHandlers
} = useWebSocketHandlers();

// ✅ SIMPLIFIED LIFECYCLE - 90% reduction
onMounted(async () => {
  await connect();
  registerHandlers();
  await loadConversations();
});

onUnmounted(() => {
  disconnect();
});

// ✅ CLEAN EVENT HANDLERS
const handleMessageSent = (message) => {
  addMessage(message);
};

const handleEditMessage = (message) => {
  selectedMessage.value = message;
  editMessageDialogOpen.value = true;
};

const handleLeaveConversation = async () => {
  await leaveConversation(selectedConversationId.value);
  setSelectedConversation(null);
};
</script>
*/