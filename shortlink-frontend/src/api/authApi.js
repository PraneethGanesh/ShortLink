import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
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

export async function requestRegistration(email) {
  try {
    const { data } = await client.post("/api/v1/auth/registration/request", { email });
    return data.message;
  } catch (error) {
    throw new Error(extractErrorMessage(error, "Registration request failed."), { cause: error });
  }
}

export async function validateRegistrationToken(token) {
  try {
    const { data } = await client.get("/api/v1/auth/registration/validate", {
      params: { token },
    });
    return data;
  } catch (error) {
    throw new Error(extractErrorMessage(error, "Registration token validation failed."), { cause: error });
  }
}

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
    throw new Error(extractErrorMessage(error, "Registration failed."), { cause: error });
  }
}

export async function login({ email, password, totpCode }) {
  try {
    const { data } = await client.post("/api/v1/auth/login", { email, password, totpCode });
    if (data.accessToken) {
      storeToken(data.accessToken);
    }
    return data;
  } catch (error) {
    throw new Error(extractErrorMessage(error, "Login failed."), { cause: error });
  }
}

export async function getCurrentUser() {
  try {
    const { data } = await client.get("/api/v1/users/me");
    return data;
  } catch (error) {
    throw new Error(extractErrorMessage(error, "Unable to load current user."), { cause: error });
  }
}

export async function createShortUrl({ longUrl, customAlias, expiresAt }) {
  try {
    const payload = {
      longUrl,
      customAlias: customAlias || null,
      expiresAt: expiresAt || null,
    };
    const { data } = await client.post("/api/v1/urls", payload);
    return data;
  } catch (error) {
    throw new Error(extractErrorMessage(error, "Unable to create short URL."), { cause: error });
  }
}

export async function listShortUrls({ page = 0, size = 20 } = {}) {
  try {
    const { data } = await client.get("/api/v1/urls", {
      params: { page, size, sort: "createdAt,desc" },
    });
    return data;
  } catch (error) {
    throw new Error(extractErrorMessage(error, "Unable to load short URLs."), { cause: error });
  }
}

export async function setShortUrlEnabled(id, enabled) {
  try {
    const { data } = await client.patch(`/api/v1/urls/${id}/enabled`, null, {
      params: { enabled },
    });
    return data;
  } catch (error) {
    throw new Error(extractErrorMessage(error, "Unable to update short URL."), { cause: error });
  }
}

export async function deleteShortUrl(id) {
  try {
    await client.delete(`/api/v1/urls/${id}`);
  } catch (error) {
    throw new Error(extractErrorMessage(error, "Unable to delete short URL."), { cause: error });
  }
}

export async function setupTotp() {
  try {
    const { data } = await client.post("/api/v1/auth/2fa/setup");
    return data;
  } catch (error) {
    throw new Error(extractErrorMessage(error, "Unable to set up two-factor authentication."), { cause: error });
  }
}

export async function enableTotp(code) {
  try {
    const { data } = await client.post("/api/v1/auth/2fa/enable", { code });
    return data;
  } catch (error) {
    throw new Error(extractErrorMessage(error, "Unable to enable two-factor authentication."), { cause: error });
  }
}

export async function disableTotp(code) {
  try {
    const { data } = await client.post("/api/v1/auth/2fa/disable", { code });
    return data;
  } catch (error) {
    throw new Error(extractErrorMessage(error, "Unable to disable two-factor authentication."), { cause: error });
  }
}

export function logout() {
  clearToken();
}

export default client;