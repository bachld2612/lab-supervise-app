package com.bachld.backend.websocket;

import com.bachld.backend.service.VncSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class VncWebSocketHandler extends BinaryWebSocketHandler {

    private final VncSessionService vncSessionService;

    @Value("${vnc.server.port:5900}")
    private int vncPort;

    @Value("${vnc.server.password:}")
    private String vncPassword;

    private record RelayContext(
        Socket vncSocket,
        Thread relayThread,
        AtomicBoolean handshakeDone,
        LinkedBlockingQueue<byte[]> wsQueue
    ) {}

    private final ConcurrentHashMap<String, RelayContext> relays = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession ws) throws Exception {
        String token = extractToken(ws.getUri());
        if (token == null) { ws.close(CloseStatus.POLICY_VIOLATION); return; }

        String studentIp = vncSessionService.consumeSession(token);
        if (studentIp == null) { ws.close(CloseStatus.POLICY_VIOLATION); return; }

        try {
            Socket vncSocket = new Socket(studentIp, vncPort);
            vncSocket.setTcpNoDelay(true);

            LinkedBlockingQueue<byte[]> wsQueue = new LinkedBlockingQueue<>();
            AtomicBoolean handshakeDone = new AtomicBoolean(false);

            Thread relay = new Thread(
                () -> proxyAndRelay(ws, vncSocket, wsQueue, handshakeDone),
                "vnc-" + ws.getId()
            );
            relay.setDaemon(true);
            relay.start();

            relays.put(ws.getId(), new RelayContext(vncSocket, relay, handshakeDone, wsQueue));
            log.debug("VNC relay started → {}:{}", studentIp, vncPort);
        } catch (Exception e) {
            log.warn("Cannot connect to VNC at {}:{} — {}", studentIp, vncPort, e.getMessage());
            ws.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession ws, BinaryMessage message) throws Exception {
        RelayContext ctx = relays.get(ws.getId());
        if (ctx == null || ctx.vncSocket().isClosed()) return;

        byte[] bytes = toBytes(message);

        if (!ctx.handshakeDone().get()) {
            ctx.wsQueue().offer(bytes);
        } else {
            ctx.vncSocket().getOutputStream().write(bytes);
            ctx.vncSocket().getOutputStream().flush();
        }
    }

    private void proxyAndRelay(WebSocketSession ws, Socket vncSocket,
                                LinkedBlockingQueue<byte[]> wsQueue,
                                AtomicBoolean handshakeDone) {
        try {
            InputStream vncIn = vncSocket.getInputStream();
            OutputStream vncOut = vncSocket.getOutputStream();

            // Step 1: Version exchange
            byte[] serverVersion = readExact(vncIn, 12);
            sendWs(ws, serverVersion);

            byte[] clientVersion = wsQueue.poll(10, TimeUnit.SECONDS);
            if (clientVersion == null) throw new IOException("Client version timeout");
            vncOut.write(clientVersion);
            vncOut.flush();

            // Step 2: Security negotiation — handle internally, hide from browser
            int numTypes = vncIn.read() & 0xFF;
            log.debug("VNC offered {} security type(s)", numTypes);
            if (numTypes == 0) {
                // Server error: read reason string
                int len = readInt(vncIn);
                byte[] msg = readExact(vncIn, len);
                log.warn("VNC server error: {}", new String(msg, StandardCharsets.UTF_8));
                ws.close(CloseStatus.SERVER_ERROR);
                return;
            }
            byte[] types = readExact(vncIn, numTypes);
            log.debug("VNC security types: {}", java.util.Arrays.toString(types));

            boolean authOk = performVncAuth(vncIn, vncOut, types);
            if (!authOk) {
                ws.close(CloseStatus.SERVER_ERROR);
                return;
            }

            // Step 3: Tell browser "no auth needed" (security type 1 = None)
            sendWs(ws, new byte[]{1, 1});                // [count=1][type=1]

            byte[] clientTypeChoice = wsQueue.poll(10, TimeUnit.SECONDS);
            if (clientTypeChoice == null) throw new IOException("Client security choice timeout");

            sendWs(ws, new byte[]{0, 0, 0, 0});         // auth result = OK

            // Step 4: Switch to relay mode
            handshakeDone.set(true);

            // Drain any WS messages queued during handshake
            byte[] queued;
            while ((queued = wsQueue.poll()) != null) {
                vncOut.write(queued);
                vncOut.flush();
            }

            // Step 5: Relay VNC → WS indefinitely
            byte[] buf = new byte[65536];
            int n;
            while (ws.isOpen() && (n = vncIn.read(buf)) != -1) {
                synchronized (ws) {
                    if (ws.isOpen()) ws.sendMessage(new BinaryMessage(buf, 0, n, true));
                }
            }

        } catch (Exception e) {
            log.debug("VNC proxy ended ({}): {}", ws.getId(), e.getMessage());
        } finally {
            try { ws.close(); } catch (Exception ignored) {}
            cleanup(ws.getId());
        }
    }

    /** Handles VNC security negotiation internally. Returns true if auth succeeded. */
    private boolean performVncAuth(InputStream vncIn, OutputStream vncOut, byte[] types) throws Exception {
        boolean hasNone = false;
        boolean hasVncAuth = false;
        for (byte t : types) {
            int type = t & 0xFF;
            if (type == 1) hasNone = true;
            if (type == 2) hasVncAuth = true;
        }

        if (hasNone) {
            vncOut.write(1); vncOut.flush();
            // RFB 3.8: server sends SecurityResult even for type None
            byte[] result = readExact(vncIn, 4);
            int code = ((result[0] & 0xFF) << 24) | ((result[1] & 0xFF) << 16)
                     | ((result[2] & 0xFF) << 8) | (result[3] & 0xFF);
            return code == 0;
        }

        if (hasVncAuth) {
            vncOut.write(2); vncOut.flush();

            byte[] challenge = readExact(vncIn, 16);
            byte[] response = desEncrypt(challenge, vncPassword != null ? vncPassword : "");
            vncOut.write(response); vncOut.flush();

            byte[] result = readExact(vncIn, 4);
            int code = ((result[0] & 0xFF) << 24) | ((result[1] & 0xFF) << 16)
                     | ((result[2] & 0xFF) << 8) | (result[3] & 0xFF);

            if (code != 0) {
                try {
                    int len = readInt(vncIn);
                    byte[] msg = readExact(vncIn, len);
                    log.warn("VNC auth failed: {}", new String(msg, StandardCharsets.UTF_8));
                } catch (Exception ignored) {}
                return false;
            }
            return true;
        }

        log.warn("No supported VNC security type found");
        return false;
    }

    private byte[] desEncrypt(byte[] data, String password) throws Exception {
        byte[] key = new byte[8];
        byte[] pwdBytes = password.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(pwdBytes, 0, key, 0, Math.min(8, pwdBytes.length));
        for (int i = 0; i < 8; i++) key[i] = reverseBits(key[i]);

        SecretKeySpec keySpec = new SecretKeySpec(key, "DES");
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }

    private byte reverseBits(byte b) {
        int v = b & 0xFF, r = 0;
        for (int i = 0; i < 8; i++) { r = (r << 1) | (v & 1); v >>= 1; }
        return (byte) r;
    }

    private byte[] readExact(InputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int read = 0;
        while (read < n) {
            int r = in.read(buf, read, n - read);
            if (r == -1) throw new IOException("VNC stream closed");
            read += r;
        }
        return buf;
    }

    private int readInt(InputStream in) throws IOException {
        byte[] b = readExact(in, 4);
        return ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16) | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
    }

    private void sendWs(WebSocketSession ws, byte[] data) throws IOException {
        synchronized (ws) {
            if (ws.isOpen()) ws.sendMessage(new BinaryMessage(data));
        }
    }

    private byte[] toBytes(BinaryMessage msg) {
        ByteBuffer payload = msg.getPayload();
        byte[] bytes = new byte[payload.remaining()];
        payload.get(bytes);
        return bytes;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession ws, CloseStatus status) { cleanup(ws.getId()); }

    @Override
    public void handleTransportError(WebSocketSession ws, Throwable exception) { cleanup(ws.getId()); }

    private void cleanup(String sessionId) {
        RelayContext ctx = relays.remove(sessionId);
        if (ctx != null) {
            try { ctx.vncSocket().close(); } catch (Exception ignored) {}
            ctx.relayThread().interrupt();
        }
    }

    private String extractToken(URI uri) {
        if (uri == null || uri.getQuery() == null) return null;
        for (String param : uri.getQuery().split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0])) return kv[1];
        }
        return null;
    }
}
