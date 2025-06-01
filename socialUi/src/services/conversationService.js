import api from './api';

const ConversationServices = {
  // Get list of conversations with pagination
  getConversations({ page = 1, limit = 10 } = {}, token) {
    return api.get('/conversations', {
      params: { page, limit },
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
  },

  // Get single conversation details
  getConversationById(id, token) {
    return api.get(`/conversations/${id}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
  },

  // Mark conversation as read
  markAsRead(conversationId, token) {
    return api.put(`/conversations/${conversationId}/read`, null, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
  },

  // Send message in conversation
  sendMessage(conversationId, message, token) {
    return api.post(`/conversations/${conversationId}/messages`, { message }, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
  },

  // Delete conversation
  deleteConversation(conversationId, token) {
    return api.delete(`/conversations/${conversationId}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
  }
};

export default ConversationServices;
