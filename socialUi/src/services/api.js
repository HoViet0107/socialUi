import axios from "axios";

const API_URL = "http://localhost:8080/api/v1";

export const api = axios.create({
    baseURL: API_URL,
    headers: {
        "Content-Type": "application/json",
    },
});

// Helper function to create auth config
const authConfig = (token) => ({
    headers: { Authorization: `Bearer ${token}` }
})


// Helper function to format date for API
const formatDateForApi = (date) => {
    if (!date) return null;
    return date.toISOString().replace('Z', '')  // Remove the 'Z' to match Java's LocalDateTime format
}

export const UserServices = {
    login(data) {
        return api.post(`/auth/login`, data);
    },
    register(data) {
        return api.post(`/auth/register`, data);
    },
    getUserById(userId) {
        return api.get(`/user/${userId}`);
    },
    getUserByEmail(token) {
        return api.get(`/user/profile`, authConfig(token));
    },
    editUser(data, token) {
        return api.put(`/user/profile`, data, authConfig(token));
    },
};

export const FeedItemServices = {
    getFeedItems(data) {
        return api.post(`/feed-items/fetch`, data);
    },
    getFeedItemReactionCount(feedItemId, token) {
        return api.get(`/feed-items/detail/${feedItemId}`, authConfig(token));
    },
    createFeedItem(data, token) {
        return api.post(`/feed-items`, data, authConfig(token));
    },
    editFeedItem(data, token) {
        return api.put(`/feed-items`, data, authConfig(token));
    }
};

export const ConversationServices = {
    // Get user conversations
    getUserConversations(token) {
        return api.get(`/conversations`, authConfig(token));
    },

    getConversationById(conversationId, token) {
        return api.get(`/conversations/${conversationId}`, authConfig(token));
    },

    createConversation(data, token) {
        return api.post(`/conversations`, data, authConfig(token));
    },

    editConversation(conversationId, data, token) {
        return api.put(`/conversations/${conversationId}`, data, authConfig(token));
    },

    deleteConversation(conversationId, token) {
        return api.put(`/conversations/${conversationId}/delete`, {}, authConfig(token));
    },

    addParticipantsToConversation(conversationId, data, token) {
        return api.post(`/conversations/${conversationId}/participants`, data, authConfig(token));
    },

    leaveConversation(conversationId, token) {
        return api.put(`/conversations/${conversationId}/participants/leave`, {}, authConfig(token));
    },

    getParticipantsInConversation(conversationId, token) {
        return api.get(`/conversations/${conversationId}/participants`, authConfig(token));
    }
};

export const MessageServices = {
    getMessagesForConversation(conversationId, cursor, token, size = 20) {
        // rest-api
        const params = new URLSearchParams();
        if (cursor) {
            const formattedCursor = formatDateForApi(new Date(cursor));
            params.append('cursor', formattedCursor);
        }
        params.append('size', size);
        return api.get(`/messages/${conversationId}?${params.toString()}`, authConfig(token));
    },
    sendMessage(conversationId, data, token) {
        // web-socket
        const config = {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'multipart/form-data'
            }
        };
        return api.post(`/messages/${conversationId}`, data, config);
    },
    editMessage(messageId, newContent, token) {
        // web-socket
        const params = new URLSearchParams();
        params.append('newContent', newContent);
        return api.put(`/messages/${messageId}?${params.toString()}`, {}, authConfig(token));
    },
    deleteMessage(messageId, token) {
        // web-socket
        return api.put(`/messages/${messageId}/delete`, {}, authConfig(token));
    },
    addReaction(messageId, reactionType, token) {
        // web-socket
        const params = new URLSearchParams();
        params.append('reactionType', reactionType);
        return api.post(`/messages/${messageId}/reactions?${params.toString()}`, {}, authConfig(token));
    },
    removeReaction(messageId, token) {
        // web-socket
        return api.delete(`/messages/${messageId}/reactions`, authConfig(token));
    }
};