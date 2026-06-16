package com.bachld.config;

/**
 * Renews the short-lived access token using the refresh-token cookie.
 * Returns the new access token, or null if refresh failed.
 */
@FunctionalInterface
public interface TokenRefresher {
    String refresh();
}
