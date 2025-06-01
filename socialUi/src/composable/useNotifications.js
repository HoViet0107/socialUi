import { useQuasar } from 'quasar';

/**
 * Composable for consistent notification management across the application
 * Provides standardized notification types and behaviors
 */
export function useNotifications() {
    const $q = useQuasar();

    /**
     * Default notification configuration that can be overridden
     */
    const defaultOptions = {
        position: 'bottom',
        timeout: 3000,
        actions: []
    };

    /**
     * Show a success notification
     * @param {string} message - Message to display
     * @param {Object} options - Optional configuration
     */
    const success = (message, options = {}) => {
        $q.notify({
            type: 'positive',
            color: 'positive',
            icon: 'check_circle',
            message,
            ...defaultOptions,
            ...options
        });
    };

    /**
     * Show an error notification
     * @param {string} message - Error message to display
     * @param {string} caption - Optional additional details
     * @param {Object} options - Optional configuration
     */
    const error = (message, caption = '', options = {}) => {
        $q.notify({
            type: 'negative',
            color: 'negative',
            icon: 'error',
            message,
            caption,
            timeout: caption ? 5000 : defaultOptions.timeout,
            ...defaultOptions,
            ...options
        });
    };

    /**
     * Show an info notification
     * @param {string} message - Message to display
     * @param {Object} options - Optional configuration
     */
    const info = (message, options = {}) => {
        $q.notify({
            type: 'info',
            color: 'info',
            icon: 'info',
            message,
            ...defaultOptions,
            ...options
        });
    };

    /**
     * Show a warning notification
     * @param {string} message - Warning message to display
     * @param {string} caption - Optional additional details
     * @param {Object} options - Optional configuration
     */
    const warning = (message, caption = '', options = {}) => {
        $q.notify({
            type: 'warning',
            color: 'warning',
            icon: 'warning',
            message,
            caption,
            ...defaultOptions,
            ...options
        });
    };

    /**
     * Show a notification for a loading operation
     * @param {string} message - Message to display while loading
     * @returns {Function} Function to dismiss the notification when done
     */
    const loading = (message = 'Loading...') => {
        const notification = $q.notify({
            type: 'ongoing',
            message,
            position: 'bottom',
            timeout: 0,
            spinner: true,
            color: 'grey-7',
            actions: []
        });

        return {
            dismiss: () => notification(),
            update: (newMessage) => notification({
                message: newMessage
            })
        };
    };

    /**
     * Show a notification requiring user confirmation
     * @param {string} message - Message to display
     * @param {Object} options - Optional configuration
     * @returns {Promise} Promise that resolves with user response
     */
    const confirm = (message, options = {}) => {
        return new Promise((resolve) => {
            const notification = $q.notify({
                message,
                timeout: 0,
                color: 'primary',
                position: 'center',
                actions: [
                    { label: options.okLabel || 'Yes', color: 'white', handler: () => resolve(true) },
                    { label: options.cancelLabel || 'No', color: 'white', handler: () => resolve(false) }
                ],
                ...options
            });

            // Auto dismiss after timeout if specified
            if (options.autoTimeout) {
                setTimeout(() => {
                    notification();
                    resolve(false);
                }, options.autoTimeout);
            }
        });
    };

    /**
     * Display connection status notifications
     * @param {string} status - 'connected', 'disconnected', 'error', etc.
     */
    const connectionStatus = (status, details = '') => {
        switch (status) {
            case 'connected':
                success('Connected to server');
                break;
            case 'disconnected':
                info('Disconnected from server');
                break;
            case 'reconnecting':
                warning('Connection lost, attempting to reconnect...', '', { timeout: 0 });
                break;
            case 'error':
                error('Connection error', details);
                break;
            default:
                info(`Connection status: ${status}`);
        }
    };

    return {
        success,
        error,
        info,
        warning,
        loading,
        confirm,
        connectionStatus
    };
}

export default useNotifications;
