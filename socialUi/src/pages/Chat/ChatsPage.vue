<template>
  <q-page class="chat-page">
    <div class="chat-container" :class="{ 'has-selected-chat': selectedConversationId }">
      <!-- Sidebar: Conversation List -->
      <ConversationList :conversations="conversations" :selectedConversationId="selectedConversationId"
        :loading="conversationsLoading" :currentUserId="currentUser?.id || null"
        @select-conversation="selectConversation" />

      <!-- Main Chat Area -->
      <div class="chat-main" v-if="selectedConversationId">
        <!-- Chat Header -->
        <ChatHeader :conversation="selectedConversation" @edit-conversation="handleEditConversation"
          @leave-conversation="handleLeaveConversation" />

        <!-- Messages Area -->

        <!-- Đảm bảo MessageList binding như này trong ChatsPage.vue -->
        <MessageList ref="messageListRef" :messages="sortedMessages" :currentUserId="currentUser?.id || null"
          :loading="messagesLoading" :hasMore="messageStore?.hasMore" @load-more="loadMoreMessages"
          @edit-message="handleEditMessage" />

        <!-- Message Input -->
        <MessageInput :conversationId="selectedConversationId" @message-sent="handleMessageSent" />
      </div>

      <!-- Empty State -->
      <div v-else class="chat-empty">
        <q-icon name="chat" size="64px" color="grey-5" />
        <p class="text-h6 text-grey-6 q-mt-md">Select a conversation to start chatting</p>
      </div>
    </div>

    <!-- Edit Message Dialog -->
    <EditMessageDialog :model-value="editMessageDialogOpen" :message="selectedMessage"
      @update:model-value="editMessageDialogOpen = $event" @save="saveEditedMessage" />
  </q-page>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { useQuasar } from 'quasar';
import { createPinia, getActivePinia, setActivePinia } from 'pinia';
import { UserServices, MessageServices, ConversationServices } from 'src/services/api';
import ConversationList from 'src/components/chat/ConversationList.vue';
import ChatHeader from 'src/components/chat/ChatHeader.vue';
import MessageList from 'src/components/chat/MessageList.vue';
import MessageInput from 'src/components/chat/MessageInput.vue';
import EditMessageDialog from 'src/components/chat/EditMessageDialog.vue';
import { getAuthToken, parseErrorResponse } from 'src/helpers/helperFunctions';
import { useMessageStore } from 'src/stores/messageStore';
import { useConversationStore } from 'src/stores/conversationStore';
import { useChatManager } from 'src/composable/useChatManager';
import { useWebSocket } from 'src/composable/useWebSocket';
import { eventBus } from 'src/utils/eventBus';
import WebsocketService from 'src/services/WebsocketService';
import { useRoute, useRouter } from 'vue-router';

const $q = useQuasar();
const token = getAuthToken();
const route = useRoute();
const router = useRouter();

// Store references
const messageStore = ref(null);
const conversationStore = ref(null);

const cleanupBackListeners = ref({})

// Track if we're handling a route change to avoid loops
const isHandlingRoute = ref(false);

// Initialize stores
const initializeStores = () => {
  const pinia = getActivePinia() || $q.inject('pinia');
  if (!pinia) {
    console.error('Pinia is not initialized');
    return false;
  }

  setActivePinia(pinia);
  messageStore.value = useMessageStore();
  conversationStore.value = useConversationStore();
  return true;
};

// Initialize WebSocket with auto-reconnect capability
const ws = useWebSocket(token);

// State
const currentUser = ref(null);
const messageListRef = ref(null);
const editMessageDialogOpen = ref(false);
const selectedMessage = ref(null);

// Initialize chat manager with our composable
const chatManager = useChatManager(token);

// Destructure values from chat manager for convenience
const {
  conversations,
  selectedConversationId,
  conversationsLoading,
  messagesLoading,
  sortedMessages,
  selectedConversation,
  initializeStores: initializeChatManagerStores,
  loadConversations,
  selectConversation: originalSelectConversation,
  loadMoreMessages,
  updateConversationLastMessage
} = chatManager;

// Wrap the selectConversation function to update URL
const selectConversation = (conversationId) => {
  if (conversationId && !isHandlingRoute.value) {
    router.replace(`/chat/${conversationId}`);
  }
  return originalSelectConversation(conversationId);
};

// Handle route changes
watch(() => route.params.conversationId, (newConversationId) => {
  if (newConversationId) {
    const conversationId = parseInt(newConversationId, 10);
    if (!isNaN(conversationId) && conversationId !== selectedConversationId.value) {
      isHandlingRoute.value = true;
      originalSelectConversation(conversationId).finally(() => {
        isHandlingRoute.value = false;
      });
    }
  } else if (selectedConversationId.value) {
    isHandlingRoute.value = true;
    isHandlingRoute.value = false;
  }
}, { immediate: true });

// Initialize WebSocket handlers
const initializeWebSocketHandlers = () => {
  if (!conversationStore.value || !messageStore.value) {
    console.error('Stores not available for WebSocket initialization');
    return;
  }

  try {
    ws.initialize({
      quasar: $q,
      conversationStore: conversationStore.value,
      messageStore: messageStore.value
    });

    // Register custom handlers directly using the enhanced WebSocket composable
    ws.registerHandler('NEW_MESSAGE', ({ message }) => {
      if (!message) return;

      // Ensure message has all required fields
      const processedMessage = {
        ...(message),
        id: message.id || `temp-${Date.now()}-${Math.random()}`,
        sentAt: message.sentAt || new Date().toISOString()
      };

      // Update conversation list even if not in current conversation
      updateConversationLastMessage(processedMessage);

      // If this is the current conversation, ensure message is in the message store
      if (selectedConversationId.value === processedMessage.conversationId) {
        messageStore.value.addMessage(processedMessage);
        // Scroll to the bottom when a new message is received in the current conversation
        setTimeout(() => messageListRef.value?.scrollToBottom(), 300);
      }
    });

    // Custom handler for conversation updates
    ws.registerHandler('CONVERSATION_UPDATED', ({ conversationId, updates }) => {
      if (!conversationId || !updates) return;

      // Find and update the conversation in our list
      const conversation = conversations.value.find(c => c.id === conversationId);
      if (conversation) {
        Object.assign(conversation, updates);
        // Update the store as well if available
        if (conversationStore.value) {
          conversationStore.value.updateConversation(conversationId, updates);
        }
        // Broadcast event
        eventBus.emit('conversation:updated', {
          id: conversationId,
          updates
        });
      }
    });

    // Participant change handlers
    ws.registerHandler('PARTICIPANT_ADDED', ({ conversationId }) => {
      if (selectedConversationId.value === conversationId) {
        // Reload conversation details
        loadConversations();
      }
    });

    // Handle participant removal
    ws.registerHandler('PARTICIPANT_REMOVED', ({ conversationId, userId }) => {
      if (selectedConversationId.value === conversationId && userId === currentUser.value?.id) {
        // User was removed from conversation
        loadConversations();
      }
    });

    // Handler for PARTICIPANTS_LEAVE_CONVERSATION
    ws.registerHandler('PARTICIPANTS_LEAVE_CONVERSATION', (data) => {
      try {
        console.log('Received PARTICIPANTS_LEAVE_CONVERSATION:', data);

        const { conversationId, userName } = data;

        // Check if the conversationId is valid
        if (selectedConversationId.value === conversationId) {
          handleParticipantLeftCurrentConversation(conversationId, userName);
        }

      } catch (error) {
        console.error('Error handling PARTICIPANTS_LEAVE_CONVERSATION:', error);
      }
    });

    // Handler for system messages
    ws.registerHandler('SYSTEM_MESSAGE', (data) => {
      try {
        console.log('Received SYSTEM_MESSAGE:', data);

        const { conversationId, message, type } = data;

        if (type === 'GROUP_DELETED') {
          handleGroupDeleted(conversationId, message);
        } else if (type === 'PARTICIPANT_LEFT') {
          handleParticipantLeftMessage(conversationId, message);
        }

      } catch (error) {
        console.error('Error handling SYSTEM_MESSAGE:', error);
      }
    });

    // Emit global event that WebSocket is ready
    eventBus.emit('ws:ready');

  } catch (error) {
    console.error('Failed to initialize WebSocket handlers:', error);
  }
};

const handleMessageSent = (message) => {
  const messageWithConvId = {
    ...message,
    conversationId: message.conversationId || selectedConversationId.value
  };

  updateConversationLastMessage(messageWithConvId);

  if (messageStore.value && selectedConversationId.value) {
    messageStore.value.addMessage(messageWithConvId);
  }

  nextTick(() => {
    messageListRef.value?.scrollToBottom();
  });
};

/**
 * WebSocket message event handler
 * Ensures scrolling happens on both sender and receiver side
 */
const handleWebSocketMessage = ({ detail: { conversationId } }) => {
  if (conversationId && selectedConversationId.value === +conversationId) {
    nextTick(() => messageListRef.value?.scrollToBottom());
  }
};

/**
 * Handle conversation edit by calling the useChatManager's editConversation method and
 * updating the conversation in the store. If there's an error, log it and show a notification.
 * Otherwise, show a positive notification.
 * @param {object} data - The updated conversation data (title, avatarUrl, etc.)
 */
const handleEditConversation = async (data) => {
  let hasError = false;
  try {
    await useChatManager.value.editConversation(selectedConversationId.value, data);
    conversationStore.value.updateConversation(selectedConversationId.value, data);
  } catch (error) {
    console.error('Error updating conversation:', error);
    hasError = true;
  } finally {
    $q.notify({
      type: hasError ? 'negative' : 'positive',
      message: hasError ? 'Failed to update conversation' : 'Conversation updated successfully'
    });
  }
};

/// -----------------------------------------
const handleLeaveConversation = async () => {
  $q.dialog({
    title: 'Leave Conversation',
    message: 'Are you sure you want to leave this conversation?',
    cancel: true,
    persistent: true
  }).onOk(async () => {
    try {
      const response = await ConversationServices.leaveConversation(
        selectedConversationId.value,
        token
      );
      // handle response from the server
      if (response && response.data) {
        const { message, conversationDeleted } = response.data;

        // Handle different cases
        if (message === 'Group has been deleted!' || conversationDeleted) {
          // ✅ CASE 1: Group deleted (< 3 members)
          handleGroupDeletedAfterLeave();
        } else {
          // ✅ CASE 2: Group still exists (>= 3 members)
          handleSuccessfulLeave(message);
        }
      } else {
        // Default success handling
        handleSuccessfulLeave('You have left the conversation!');
      }
    } catch (error) {
      console.error('Error leaving conversation:', error);
      let errorMessage = 'Failed to leave conversation';
      if (error.response) {
        const { status, data } = error.response;
        errorMessage = parseErrorResponse(status, data);
      }
      $q.notify({
        type: 'negative',
        message: errorMessage,
        timeout: 5000
      });
    }
  })
};

const handleGroupDeletedAfterLeave = () => {
  // Navigate về danh sách conversation
  setTimeout(() => {
    router.push('/chat');
    originalSelectConversation(null);
  }, 1500);

  // Show notification for group deletion
  $q.notify({
    type: 'warning',
    message: 'Group has been deleted because there are fewer than 3 members left',
    timeout: 5000,
    position: 'center',
    actions: [
      {
        label: 'OK',
        color: 'white',
        handler: () => { }
      }
    ]
  });

  // Reload conversation list để remove conversation đã bị xóa
  setTimeout(() => {
    loadConversations();
  }, 2000);
};

const handleSuccessfulLeave = (message) => {
  // Navigate về danh sách conversation
  router.push('/chat');
  originalSelectConversation(null);

  // Hiển thị thông báo thành công
  $q.notify({
    type: 'positive',
    message: message || 'You have left the conversation!',
    timeout: 3000
  });

  // Reload conversation list để cập nhật
  setTimeout(() => {
    loadConversations();
  }, 1000);
};

const loadConversationDetails = async (conversationId) => {
  try {
    // Gọi API để lấy thông tin conversation mới nhất
    const response = await ConversationServices.getConversationById(conversationId, token);

    if (response && response.data) {
      const updatedConversation = response.data;

      // Cập nhật trong conversation list
      const index = conversations.value.findIndex(c => c.id === conversationId);
      if (index !== -1) {
        conversations.value[index] = {
          ...conversations.value[index],
          ...updatedConversation
        };
      }

      // Cập nhật selected conversation nếu đang view
      if (selectedConversationId.value === conversationId) {
        // Trigger reactive update
        originalSelectConversation(conversationId);
      }
    }
  } catch (error) {
    console.error('Error loading conversation details:', error);
  }
};

const handleParticipantLeftMessage = (conversationId, message) => {
  console.log('Handling participant left message:', conversationId, message);

  // Nếu đang ở conversation hiện tại, thêm system message vào chat
  if (selectedConversationId.value === conversationId && messageStore.value) {
    const systemMessage = {
      id: `system-left-${Date.now()}-${Math.random()}`,
      content: message,
      sentAt: new Date().toISOString(),
      isSystem: true,
      type: 'SYSTEM',
      senderName: 'System',
      conversationId: conversationId,
      senderId: null // System message không có sender
    };

    // Thêm vào message store
    messageStore.value.addMessage(systemMessage);

    // Scroll to bottom
    nextTick(() => {
      if (messageListRef.value && messageListRef.value.scrollToBottom) {
        messageListRef.value.scrollToBottom();
      }
    });
  }
};






// -------------------------
const loadCurrentUser = async () => {
  try {
    const response = await UserServices.getUserByEmail(token);
    currentUser.value = response.data;
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: `Failed to load current user: ${error.message}`
    });
  }
};


/**
 * Handles editing a message in message list
 *
 * @param {Object} message The message object to be edited, or its ID
 */
const handleEditMessage = async (message) => {
  try {
    // Convert message to a plain object if necessary
    if (typeof message === 'object' && message !== null) {
      // Ensure message has all required properties
      const messageForEdit = {
        id: message.id || message.messageId,
        content: message.content || '',
        senderId: message.senderId,
        senderName: message.senderName
      };

      // Set selected message
      selectedMessage.value = messageForEdit;
    } else {
      console.error('ChatsPage: Invalid message object:', message);
      throw new Error('Invalid message object');
    }

    // Open the edit dialog
    editMessageDialogOpen.value = true;

    // Debug: Verify after a brief delay if the dialog state was set correctly
    setTimeout(() => {
      if (!editMessageDialogOpen.value) {
        console.error('ChatsPage: Dialog was not opened properly');
        // Try to open it again after a short delay
        setTimeout(() => {
          editMessageDialogOpen.value = true;
        }, 300);
      }
    }, 100);
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to edit message'
    });
    console.error('Error editing message:', error);
  }
};

/**
 * Save edited message
 * @param {Object} data - Data object containing message ID and new content
 */
const saveEditedMessage = async (data) => {
  try {
    const { messageId, newContent } = data;

    if (!messageId) {
      throw new Error('Message ID is missing');
    }

    if (!newContent || newContent.trim() === '') {
      throw new Error('Message content cannot be empty');
    }

    // Gọi API để cập nhật tin nhắn
    await MessageServices.editMessage(messageId, newContent, token);

    // Cập nhật tin nhắn trong store nếu có
    if (messageStore.value) {
      // Kiểm tra phương thức updateMessage hoặc updateMessageContent
      if (typeof messageStore.value.updateMessageContent === 'function') {
        messageStore.value.updateMessageContent(messageId, newContent);
      } else if (typeof messageStore.value.updateMessage === 'function') {
        messageStore.value.updateMessage(messageId, {
          content: newContent,
          edited: true,
          editedAt: new Date().toISOString()
        });
      } else {
        console.warn('No method available to update message in store');
      }
    }

    // Hiển thị thông báo thành công
    $q.notify({
      type: 'positive',
      message: 'Message edited successfully'
    });

    // Đóng dialog
    editMessageDialogOpen.value = false;
    selectedMessage.value = null;
  } catch (error) {
    let errorMessage = 'Failed to edit message';
    if (error.response && error.response.data && error.response.data.message) {
      errorMessage += `: ${error.response.data.message}`;
    }

    $q.notify({
      type: 'negative',
      message: errorMessage
    });
    console.error('Error editing message:', error);
  }
};

// ✅ THÊM: Handle khi có người rời conversation hiện tại
const handleParticipantLeftCurrentConversation = (conversationId, userName) => {
  // Hiển thị notification
  $q.notify({
    type: 'info',
    message: `${userName} left the conversation`,
    timeout: 3000,
    position: 'top'
  });

  // Reload participants nếu đang xem conversation này
  if (selectedConversationId.value === conversationId) {
    // Có thể reload conversation details để cập nhật participant count
    loadConversationDetails(conversationId);
  }
};

// ✅ THÊM: Handle khi group bị xóa
const handleGroupDeleted = (conversationId, message) => {
  console.log('Group deleted:', conversationId, message);

  // Nếu đang ở conversation bị xóa
  if (selectedConversationId.value === conversationId) {
    // Hiển thị thông báo
    $q.notify({
      type: 'warning',
      message: message || 'This group has been deleted',
      timeout: 5000,
      position: 'center'
    });

    // Navigate về danh sách conversation
    setTimeout(() => {
      router.push('/chat');
      originalSelectConversation(null);
    }, 2000);
  } else {
    // Chỉ hiển thị notification nhỏ
    $q.notify({
      type: 'info',
      message: `A group you were in has been deleted`,
      timeout: 3000
    });
  }

  // Reload conversation list để remove conversation đã bị xóa
  loadConversations();
};

// Lifecycle
onMounted(async () => {
  try {
    // Add event listener for WebSocket messages
    window.addEventListener('new-message-received', handleWebSocketMessage);

    // First, ensure Pinia is available
    let pinia;
    try {
      pinia = getActivePinia();
      if (!pinia) {
        console.warn('No active Pinia instance found, creating a new one');
        pinia = createPinia();
        setActivePinia(pinia);
      }
    } catch (error) {
      console.error('Error getting Pinia instance:', error);
      throw new Error('Failed to initialize application state');
    }

    // Initialize stores
    const storesInitialized = initializeStores();
    if (!storesInitialized) {
      throw new Error('Failed to initialize stores');
    }

    // Wait for stores to be properly initialized
    await new Promise((resolve, reject) => {
      const maxAttempts = 10;
      let attempts = 0;

      const checkStores = setInterval(() => {
        attempts++;

        if (messageStore.value && conversationStore.value) {
          // Initialize the chat manager stores with our initialized stores
          if (initializeChatManagerStores) {
            const { messageStore: msgStore, conversationStore: convStore } = initializeChatManagerStores();
            msgStore.value = messageStore.value;
            convStore.value = conversationStore.value;
          }

          clearInterval(checkStores);
          resolve(true);
        } else if (attempts >= maxAttempts) {
          clearInterval(checkStores);
          console.error('Stores did not initialize in time');
          reject(new Error('Application initialization timed out'));
        }
      }, 100);
    });

    // Load initial data
    try {
      await Promise.all([
        loadCurrentUser(),
        loadConversations()
      ]);
    } catch (error) {
      console.error('Error loading initial data:', error);
      // Continue anyway, we might be able to recover
    }

    // Initialize WebSocket handlers with stores
    try {
      // Make sure WebSocketService is properly initialized with our stores
      const wsService = window.WebSocketService || WebsocketService;
      if (wsService && typeof wsService.initialize === 'function') {
        wsService.initialize({
          quasar: $q,
          conversationStore: conversationStore.value,
          messageStore: messageStore.value
        });
      }

      // Now initialize our handlers
      initializeWebSocketHandlers();

      // Join conversations via WebSocket if we have a selected conversation
      // after a small delay to ensure connection is established
      setTimeout(() => {
        if (selectedConversationId.value) {
          ws.joinConversation(selectedConversationId.value);
        }
      }, 500);
    } catch (error) {
      console.error('Error initializing WebSocket:', error);
      // Notify user but don't block the UI
      $q.notify({
        type: 'warning',
        message: 'Real-time features may be limited',
        caption: 'Could not establish WebSocket connection',
        timeout: 3000
      });
    }

    // Add direct DOM event listener for back button clicks
    setTimeout(() => {
      const backBtn = document.getElementById('chat-back-button');
      if (backBtn) {
        backBtn.addEventListener('click', (e) => {
          e.stopPropagation();
        });
      }
    }, 1000); // Add with delay to ensure DOM is ready
  } catch (error) {
    console.error('Error in onMounted:', error);
    $q.notify({
      type: 'negative',
      message: 'Failed to initialize chat. Please refresh the page.',
      timeout: 5000
    });
  }
});

onUnmounted(() => {
  // Clean up all custom event listeners
  if (cleanupBackListeners.value && typeof cleanupBackListeners.value === 'function') {
    cleanupBackListeners.value();
  }

  // Leave current conversation
  if (selectedConversationId.value) {
    ws.leaveConversation(selectedConversationId.value);
  }

  // Remove WebSocket message event listener
  window.removeEventListener('new-message-received', handleWebSocketMessage);
});
</script>

<style lang="scss">
@import '../../css/chat-mobile.css';
@import '../../css/utility.css';

.chat-page {
  height: calc(100vh - 64px);
  padding: 0;
}

.chat-container {
  display: flex;
  height: 100%;
  background: #f0f2f5;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-left: 1px solid #e4e6eb;
}

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: white;
}

/* Mobile responsive styles moved to chat-mobile.css */
</style>