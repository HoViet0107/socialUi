/**
 * Message formatting utilities for chat applications
 */

import { format, formatDistanceToNow, isToday, isYesterday } from 'date-fns';

export const messageFormatter = {
    /**
     * Format a timestamp for display in message bubbles
     * @param {string|Date} timestamp - Date object or ISO string
     * @param {Object} options - Configuration options
     * @returns {string} Formatted time string
     */
    formatMessageTime(timestamp, options = {}) {
        if (!timestamp) return '';

        const date = timestamp instanceof Date ? timestamp : new Date(timestamp);
        const { showSeconds = false } = options;

        // Default time format based on showSeconds option
        const timeFormat = showSeconds ? 'h:mm:ss a' : 'h:mm a';

        return format(date, timeFormat);
    },

    /**
     * Format a timestamp for display in message groups or headers
     * @param {string|Date} timestamp - Date object or ISO string
     * @returns {string} Formatted date string (Today, Yesterday, or date)
     */
    formatMessageDate(timestamp) {
        if (!timestamp) return '';

        const date = timestamp instanceof Date ? timestamp : new Date(timestamp);

        if (isToday(date)) {
            return 'Today';
        } else if (isYesterday(date)) {
            return 'Yesterday';
        } else {
            return format(date, 'MMM d, yyyy');
        }
    },

    /**
     * Format a relative time for last seen/activity indicators
     * @param {string|Date} timestamp - Date object or ISO string
     * @returns {string} Formatted relative time (e.g., "2 hours ago")
     */
    formatRelativeTime(timestamp) {
        if (!timestamp) return '';

        const date = timestamp instanceof Date ? timestamp : new Date(timestamp);
        return formatDistanceToNow(date, { addSuffix: true });
    },

    /**
     * Process message text to handle special formatting
     * @param {string} text - Raw message text
     * @returns {string} Processed message text with links, emojis, etc.
     */
    processMessageText(text) {
        if (!text) return '';

        // Convert URLs to clickable links
        const urlRegex = /(https?:\/\/[^\s]+)/g;
        let processedText = text.replace(urlRegex, url => {
            return `<a href="${url}" target="_blank" rel="noopener noreferrer">${url}</a>`;
        });

        // Add any other text processing here (markdown, emojis, etc.)

        return processedText;
    },

    /**
     * Group messages by date for better UI organization
     * @param {Array} messages - Array of message objects
     * @param {string} dateField - Field name containing the timestamp
     * @returns {Array} Array of message groups with date headers
     */
    groupMessagesByDate(messages, dateField = 'sentAt') {
        if (!Array.isArray(messages) || messages.length === 0) {
            return [];
        }

        const groups = [];
        let currentDate = null;
        let currentGroup = null;

        messages.forEach(message => {
            const messageDate = message[dateField] instanceof Date
                ? message[dateField]
                : new Date(message[dateField]);

            const dateString = format(messageDate, 'yyyy-MM-dd');

            if (dateString !== currentDate) {
                currentDate = dateString;
                currentGroup = {
                    date: this.formatMessageDate(messageDate),
                    dateValue: messageDate,
                    messages: []
                };
                groups.push(currentGroup);
            }

            currentGroup.messages.push(message);
        });

        return groups;
    }
};

export default messageFormatter;
