// Debug utility
export const debug = {
  log: function(component, action, data) {
    console.log(`[${component}] ${action}`, data || '');
  }
};
