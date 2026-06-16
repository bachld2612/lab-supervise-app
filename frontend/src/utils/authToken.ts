// In-memory access token store (NOT persisted — refresh token lives in an
// HttpOnly cookie). Kept outside React so axios interceptors and STOMP hooks
// can read the current token synchronously.

let accessToken: string | null = null;

export const getAccessToken = (): string | null => accessToken;

export const setAccessToken = (token: string | null): void => {
  accessToken = token;
};

export const clearAccessToken = (): void => {
  accessToken = null;
};
