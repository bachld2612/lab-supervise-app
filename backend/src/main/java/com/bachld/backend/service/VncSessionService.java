package com.bachld.backend.service;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages short-lived, one-time VNC relay tokens.
 */
@Service
public class VncSessionService {

    private record SessionInfo(String studentIp, long expiresAt) {
        boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }

    private final ConcurrentHashMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public String createSession(String studentIp) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new SessionInfo(studentIp, System.currentTimeMillis() + 30_000));
        return token;
    }

    public record VncSessionData(String studentIp) {}

    /** Consumes the token once and returns session data, or null if invalid/expired. */
    public VncSessionData consumeSession(String token) {
        SessionInfo info = sessions.remove(token);
        if (info == null || info.isExpired()) return null;
        return new VncSessionData(info.studentIp());
    }
}
