// import { UserReactionServices } from "src/services/api";
import { format } from "date-fns";
import { jwtDecode } from "jwt-decode";

// Helper function to get auth token
export function getAuthToken() {
  try {
    return JSON.parse(localStorage.getItem("authUser"));
  } catch (error) {
    console.error("Error parsing authUser:", error);
    return null;
  }
};

export function formatDate(isoString) {
  if (!isoString) return "N/A";
  return format(new Date(isoString), "dd/MM/yyyy HH:mm:ss");
};

export function isTokenExpired() {
  const token = getAuthToken();
  if (!token) return null;
  if (jwtDecode(token).exp < Date.now() / 1000) {
    return true;
  }
  return false;
}

export function isAdminUser() {
  const token = getAuthToken();
  if (!token) return null;
  const decodedToken = jwtDecode(token);
  return decodedToken.authorities[0] === 'ADMIN';
}

export function parseErrorResponse(status, data) {
  let errorMessage = 'An error occurred';
  if (status === 403) {
    errorMessage = 'You do not have permission to do this action';
  } else if (status === 404) {
    errorMessage = 'Resource not found';
  } else if (status === 400) {
    errorMessage = data.message || 'Invalid request';
  } else if (data && data.message) {
    errorMessage = data.message;
  }
  return errorMessage;
}