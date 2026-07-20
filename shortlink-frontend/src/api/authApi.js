import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8190";
const TOKEN_STORAGE_KEY = "identityservice.accessToken";

const client = axios.create({
  baseURL: API_BASE_URL,
  headers: { "Content-Type": "application/json" },
});

// Attach the stored JWT to every outgoing request that needs it.
client.interceptors.request.use((config) => {
  const token = getStoredToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

function extractErrorMessage(error, fallback) {
  const data = error?.response?.data;
  if (data?.message) return data.message;
  if (data?.fieldErrors) {
    const firstFieldMessage = Object.values(data.fieldErrors)[0];
    if (firstFieldMessage) return firstFieldMessage;
  }
  if (error?.message === "Network Error") {
    return "Can't reach the server. Check your connection and try again.";
  }
  return fallback;
}

export function getStoredToken() {
  return localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function storeToken(token) {
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
}

export function isAuthenticated() {
  return Boolean(getStoredToken());
}

/**
 * Step 1 of the flow: kick off registration for an email address.
 * Returns the backend's message (e.g. confirmation that a link was sent).
 */
export async function requestRegistration(email) {
  try {
    const { data } = await client.post("/api/v1/auth/registration/request", { email });
    return data.message;
  } catch (error) {
    throw new Error(
  extractErrorMessage(error, "Message"),
  { cause: error }
);
  }
}

/**
 * Step 2: confirm the token from the registration link is still valid
 * before showing the "set your password" form.
 */
export async function validateRegistrationToken(token) {
  try {
    const { data } = await client.get("/api/v1/auth/registration/validate", {
      params: { token },
    });
    return data;
  } catch (error) {
      throw new Error(
          extractErrorMessage(error, "Message"),
          { cause: error }
      );
  }
}

/**
 * Step 3: finish registration with the chosen name/password.
 */
export async function completeRegistration({ token, name, password, phoneNumber }) {
  try {
    const { data } = await client.post("/api/v1/auth/registration/complete", {
      token,
      name,
      password,
      phoneNumber,
    });
    return data;
  } catch (error) {
      throw new Error(
          extractErrorMessage(error, "Message"),
          { cause: error }
      );
  }
}

/**
 * Step 4: log in and persist the JWT for subsequent requests.
 */
export async function login({ email, password }) {
  try {
    const { data } = await client.post("/api/v1/auth/login", { email, password });
    storeToken(data.accessToken);
    return data;
  } catch (error) {
      throw new Error(
          extractErrorMessage(error, "Message"),
          { cause: error }
      );
  }
}

/**
 * Step 5: fetch the authenticated user for the protected dashboard.
 */
export async function getCurrentUser() {
  try {
    const { data } = await client.get("/api/v1/users/me");
    return data;
  } catch (error) {
      throw new Error(
          extractErrorMessage(error, "Message"),
          { cause: error }
      );
  }
}

export function logout() {
  clearToken();
}

export default client;
