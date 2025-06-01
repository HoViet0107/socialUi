import { getActivePinia, setActivePinia } from 'pinia';
import { useQuasar } from 'quasar';
import { ref, onMounted } from 'vue';

/**
 * Initializes multiple Pinia stores with error handling
 * @param {Array<Function>} storeGetters - Array of store getter functions (e.g., [useMessageStore, useConversationStore])
 * @returns {Object} Object containing initialized stores and success status
 */
export const initializeStores = (storeGetters = []) => {
  const $q = useQuasar();
  
  try {
    // Get the Pinia instance from the app
    const pinia = getActivePinia();
    if (!pinia) {
      throw new Error('Pinia is not initialized');
    }
    
    // Ensure the pinia instance is active
    setActivePinia(pinia);
    
    // Initialize stores
    const stores = storeGetters.map(getStore => {
      const store = getStore();
      
      // Initialize store if needed
      if (store && typeof store.initialize === 'function' && !store.isInitialized) {
        store.initialize();
      }
      
      return store;
    });
    
    return {
      success: true,
      stores,
      error: null
    };
    
  } catch (error) {
    console.error('Error initializing stores:', error);
    
    // Show error notification if Quasar is available
    if ($q) {
      $q.notify({
        type: 'negative',
        message: 'Failed to initialize application state',
        caption: error.message,
        timeout: 5000
      });
    }
    
    return {
      success: false,
      stores: [],
      error: error.message
    };
  }
};

/**
 * Composable function to initialize stores in a component
 * @param {Array<Function>} storeGetters - Array of store getter functions
 * @returns {Object} Object containing initialized stores and loading state
 */
export const useStoreInitializer = (storeGetters = []) => {
  const stores = ref([]);
  const isLoading = ref(true);
  const error = ref(null);
  
  onMounted(() => {
    const result = initializeStores(storeGetters);
    stores.value = result.stores;
    error.value = result.error;
    isLoading.value = false;
  });
  
  return {
    stores,
    isLoading,
    error
  };
};
