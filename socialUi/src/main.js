import { createApp } from 'vue'
import { Quasar } from 'quasar'
import { createPinia } from 'pinia'

// Import Quasar css
import '@quasar/extras/roboto-font/roboto-font.css'
import '@quasar/extras/material-icons/material-icons.css'
import 'quasar/src/css/index.sass'

// Import App and plugins
import App from './App.vue'
import router from './router'
import Toast from 'vue-toastification'
import 'vue-toastification/dist/index.css'

// Create app instance
const app = createApp(App)

// Initialize Pinia first
const pinia = createPinia()

// Create Quasar instance with config
const quasarConfig = {
  plugins: {},
  config: {}
}

// Apply plugins in correct order
app.use(pinia)
app.use(Quasar, quasarConfig)
app.use(router)
app.use(Toast)

// Add error handler for unhandled promise rejections
window.addEventListener('unhandledrejection', (event) => {
  console.error('Unhandled promise rejection:', event.reason)
  if (app.config.globalProperties.$q) {
    app.config.globalProperties.$q.notify({
      type: 'negative',
      message: 'An unexpected error occurred',
      timeout: 5000
    })
  }
})

// Mount the app
app.mount('#app')

// Initialize WebSocket after the app is mounted
const initWebSocket = () => {
  const token = localStorage.getItem('jwt_token')
  if (!token) {
    console.warn('No JWT token found. WebSocket connection not established.')
    return
  }

  try {
    import('./services/WebsocketService')
      .then(module => {
        const WebSocketService = module.default
        const webSocketService = new WebSocketService()
        webSocketService.connect(token)
        // Make WebSocket service available globally
        app.config.globalProperties.$websocket = webSocketService
      })
      .catch(error => {
        console.error('Failed to load WebSocket service:', error)
      })
  } catch (error) {
    console.error('Failed to initialize WebSocket:', error)
  }
}

// Initialize WebSocket after a short delay to ensure app is mounted
setTimeout(initWebSocket, 1000)