// Models for Conversation requests and responses

export class CreateConversationRequest {
    constructor(data = {}) {
        this.name = data.name || '';
        this.participantIds = data.participantIds || [];
        this.isGroup = data.isGroup || false;
    }
}

export class EditConversationRequest {
    constructor(data = {}) {
        this.name = data.name || '';
        this.avatarUrl = data.avatarUrl || '';
    }
}

export class ConversationRequest {
    constructor(data = {}) {
        this.id = data.id || null;
        this.name = data.name || '';
        this.avatarUrl = data.avatarUrl || '';
        this.isGroup = data.isGroup || false;
        this.participants = data.participants || [];
        this.lastMessage = data.lastMessage || null;
        this.unreadCount = data.unreadCount || 0;
        this.lastActivity = data.lastActivity || null;
    }
}

export class ConversationParticipant {
    constructor(data = {}) {
        this.id = data.id || null;
        this.name = data.name || '';
        this.avatarUrl = data.avatarUrl || '';
        this.isAdmin = data.isAdmin || false;
    }
}

export class ConversationSearchParams {
    constructor(data = {}) {
        this.cursor = data.cursor || null; // ISO 8601 date string
        this.size = data.size || 10;
        this.keyword = data.keyword || '';
    }
}

// Cursor-based pagination response
export class CursorResponse {
    constructor(data = {}) {
        this.items = data.items || [];
        this.nextCursor = data.nextCursor || null;
        this.hasMore = data.hasMore || false;
    }
}

// Helper function to format date to ISO 8601
export const formatDateToISO = (date) => {
    return date instanceof Date ? date.toISOString() : null;
};
