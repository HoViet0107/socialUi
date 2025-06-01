/**
 * Simple event bus implementation for Vue 3 components
 * Helps with cross-component communication without prop drilling
 */
import { reactive } from 'vue'

// Create a reactive object to serve as our event bus
export const eventBus = reactive({
    // Store event listeners
    _events: {},

    // Register an event listener
    on(event, callback) {
        if (!this._events[event]) {
            this._events[event] = []
        }
        this._events[event].push(callback)

        // Return unsubscribe function
        return () => {
            this.off(event, callback)
        }
    },

    // Remove an event listener
    off(event, callback) {
        if (!this._events[event]) return

        if (callback) {
            // Remove specific callback
            const index = this._events[event].indexOf(callback)
            if (index > -1) {
                this._events[event].splice(index, 1)
            }
        } else {
            // Remove all listeners for this event
            this._events[event] = []
        }
    },

    // Emit an event with optional data
    emit(event, ...args) {
        if (!this._events[event]) return

        // Call each listener with the provided arguments
        this._events[event].forEach(callback => {
            callback(...args)
        })
    }
})

// Export a composable for easier use with Vue 3
export function useEventBus() {
    return eventBus
}
