package com.bachld.backend.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory blacklist for revoked access tokens (keyed by JWT id "jti").
 * Entries are kept only until the access token would expire naturally
 * (max ~5 minutes), then lazily evicted. Lost on restart, which is acceptable
 * because access tokens are short-lived.
 */
@Service
public class TokenBlacklistService {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    /** Blacklist a token jti until its natural expiry (epoch millis). */
    public void blacklist(String jti, long expiresAtEpochMs) {
        if (jti == null) {
            return;
        }
        cleanup();
        blacklist.put(jti, expiresAtEpochMs);
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        Long exp = blacklist.get(jti);
        if (exp == null) {
            return false;
        }
        if (exp < System.currentTimeMillis()) {
            blacklist.remove(jti);
            return false;
        }
        return true;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        blacklist.entrySet().removeIf(e -> e.getValue() < now);
    }
}
