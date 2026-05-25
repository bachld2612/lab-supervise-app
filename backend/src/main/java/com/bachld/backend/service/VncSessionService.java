package com.bachld.backend.service;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    /** Consumes the token (one-time use) and returns the student IP, or null if invalid/expired. */
    public String consumeSession(String token) {
        SessionInfo info = sessions.remove(token);
        if (info == null || info.isExpired()) return null;
        return info.studentIp();
    }
}
