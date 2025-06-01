/**
 * Mobile navigation helper utilities
 * 
 * Provides consistent navigation handling for mobile views
 * Especially useful for handling complex mobile navigation patterns
 */

import { eventBus } from './eventBus';

export const mobileNavHelper = {
    /**
     * Navigate back in mobile chat view (legacy method for backward compatibility)
     */
    goBack() {
        return this.goBackFromChat();
    },

    /**
     * Navigate back in mobile chat view
     * 
     * @param {Object} options Configuration options
     * @param {Object} options.conversation The current conversation (optional)
     * @param {Function} options.emit Vue emit function (optional)
     * @param {boolean} options.useEventBus Whether to use the event bus (default: true)
     * @param {boolean} options.useDomEvents Whether to use DOM events (default: true)
     */
    goBackFromChat(options = {}) {
        console.log('Mobile navigation: Going back from chat');
        const {
            conversation,
            emit,
            useEventBus = true,
            useDomEvents = true
        } = options;

        try {
            // Use multiple methods for redundancy in case one fails

            // 1. Use the event bus (preferred approach)
            if (useEventBus) {
                eventBus.emit('chat:back-clicked', { conversation });
            }

            // 2. Direct DOM manipulation for immediate visual feedback
            document.querySelectorAll('.chat-container.has-selected-chat').forEach(el => {
                console.log('Removing has-selected-chat class from:', el);
                el.classList.remove('has-selected-chat');
            });

            // 3. Emit Vue event if provided
            if (emit && typeof emit === 'function') {
                emit('back');
            }

            // 4. Dispatch DOM event for broader reach
            if (useDomEvents) {
                document.dispatchEvent(new CustomEvent('chat-back-clicked', {
                    bubbles: true,
                    detail: { conversation }
                }));

                // Also support legacy event name
                document.dispatchEvent(new CustomEvent('chat-back-button-clicked'));
            }

            console.log('All back navigation methods executed successfully');
            return true;
        } catch (error) {
            console.error('Error in mobile back navigation:', error);

            // Fallback approach - try direct class manipulation
            try {
                document.querySelector('.chat-container')?.classList.remove('has-selected-chat');
                return true;
            } catch (fallbackError) {
                console.error('Fallback navigation also failed:', fallbackError);
                return false;
            }
        }
    },

    /**
     * Set up back button listeners for a component
     * 
     * @param {Object} options Configuration options
     * @param {Function} options.handleBack Callback function when back is pressed
     * @param {HTMLElement} options.backButton DOM element for the back button (optional)
     * @param {string} options.buttonId ID of the back button to look for (optional)
     * @param {boolean} options.listenToBrowser Whether to listen for browser back button (default: true)
     * @param {boolean} options.useEventBus Whether to use the event bus (default: true)
     * @returns {Function} Cleanup function to remove all listeners
     */
    setupBackListeners(options) {
        const {
            handleBack,
            backButton,
            buttonId,
            listenToBrowser = true,
            useEventBus = true
        } = options;

        if (!handleBack || typeof handleBack !== 'function') {
            console.error('Invalid handleBack callback provided');
            return () => { };
        }

        // Store cleanup functions
        const cleanupFns = [];

        // Function to create event listener and return remover
        const createListener = (element, event, handler) => {
            element.addEventListener(event, handler);
            return () => element.removeEventListener(event, handler);
        };

        // 1. Listen for custom DOM events
        cleanupFns.push(createListener(document, 'chat-header-back-clicked', handleBack));
        cleanupFns.push(createListener(document, 'chat-back-clicked', handleBack));
        cleanupFns.push(createListener(document, 'chat-back-button-clicked', handleBack));

        // 2. Add direct DOM event listener for back button if specified
        if (backButton && backButton instanceof HTMLElement) {
            cleanupFns.push(createListener(backButton, 'click', (e) => {
                e.stopPropagation();
                handleBack(e);
            }));
        } else if (buttonId) {
            // Try to find the button by ID
            setTimeout(() => {
                const btn = document.getElementById(buttonId);
                if (btn) {
                    cleanupFns.push(createListener(btn, 'click', (e) => {
                        e.stopPropagation();
                        handleBack(e);
                    }));
                }
            }, 500); // Add with delay to ensure DOM is ready
        }

        // 3. Listen for browser back button
        if (listenToBrowser) {
            cleanupFns.push(createListener(window, 'popstate', handleBack));
        }

        // 4. Listen for event bus events
        if (useEventBus) {
            const unsubscribe = eventBus.on('chat:back-clicked', handleBack);
            cleanupFns.push(unsubscribe);
        }

        // Return cleanup function
        return () => {
            cleanupFns.forEach(cleanup => {
                if (typeof cleanup === 'function') {
                    cleanup();
                }
            });
            console.log('All back listeners cleaned up');
        };
    }
};

export default mobileNavHelper;
