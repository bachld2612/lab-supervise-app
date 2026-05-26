package com.bachld.backend.service;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý session token VNC ngắn hạn (30s).
 * Mỗi session chứa cả IP máy sinh viên + password VNC plaintext của máy đó —
 * password chỉ tồn tại trong memory trong vòng 30 giây, không log, không persist.
 */
@Service
public class VncSessionService {

    private record SessionInfo(String studentIp, String vncPassword, long expiresAt) {
        boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }

    private final ConcurrentHashMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public String createSession(String studentIp, String vncPassword) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new SessionInfo(studentIp, vncPassword, System.currentTimeMillis() + 30_000));
        return token;
    }

    /** Kết quả khi WebSocket tiêu thụ session token. */
    public record VncSessionData(String studentIp, String vncPassword) {}

    /** Consumes the token (one-time use) and returns the session data, or null if invalid/expired. */
    public VncSessionData consumeSession(String token) {
        SessionInfo info = sessions.remove(token);
        if (info == null || info.isExpired()) return null;
        return new VncSessionData(info.studentIp(), info.vncPassword());
    }
}
