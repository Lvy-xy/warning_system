const TOKEN_KEY = "ws_token";
const USERNAME_KEY = "ws_username";
const REMEMBERED_USERNAME_KEY = "ws_remembered_username";

export function isLoggedIn() {
  return Boolean(localStorage.getItem(TOKEN_KEY));
}

export function getUsername() {
  return localStorage.getItem(USERNAME_KEY) || "";
}

export function getRememberedUsername() {
  return localStorage.getItem(REMEMBERED_USERNAME_KEY) || "";
}

export function loginSession(token, username, rememberMe) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USERNAME_KEY, username);
  if (rememberMe) {
    localStorage.setItem(REMEMBERED_USERNAME_KEY, username);
  } else {
    localStorage.removeItem(REMEMBERED_USERNAME_KEY);
  }
}

export function logoutSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USERNAME_KEY);
}
