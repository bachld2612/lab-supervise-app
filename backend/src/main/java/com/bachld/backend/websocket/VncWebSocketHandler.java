package com.bachld.backend.websocket;

import com.bachld.backend.service.VncSessionService;
import com.bachld.backend.service.VncSessionService.VncSessionData;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VncWebSocketHandler extends BinaryWebSocketHandler {

    VncSessionService vncSessionService;

    @Value("${vnc.server.port:5900}")
    @NonFinal
    int vncPort;

    private record RelayContext(
            Socket vncSocket,
            Thread relayThread,
            AtomicBoolean handshakeDone,
            LinkedBlockingQueue<byte[]> wsQueue
    ) {}

    ConcurrentHashMap<String, RelayContext> relays = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession ws) throws Exception {
        String token = extractToken(ws.getUri());
        if (token == null) {
            ws.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        VncSessionData session = vncSessionService.consumeSession(token);
        if (session == null) {
            ws.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        String studentIp = session.studentIp();
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
            log.debug("VNC relay started -> {}:{}", studentIp, vncPort);
        } catch (Exception e) {
            log.warn("Cannot connect to VNC at {}:{} - {}", studentIp, vncPort, e.getMessage());
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
            return;
        }

        ctx.vncSocket().getOutputStream().write(bytes);
        ctx.vncSocket().getOutputStream().flush();
    }

    private void proxyAndRelay(
            WebSocketSession ws,
            Socket vncSocket,
            LinkedBlockingQueue<byte[]> wsQueue,
            AtomicBoolean handshakeDone
    ) {
        try {
            InputStream vncIn = vncSocket.getInputStream();
            OutputStream vncOut = vncSocket.getOutputStream();

            byte[] serverVersion = readExact(vncIn, 12);
            sendWs(ws, serverVersion);

            byte[] clientVersion = wsQueue.poll(10, TimeUnit.SECONDS);
            if (clientVersion == null) throw new IOException("Client version timeout");
            vncOut.write(clientVersion);
            vncOut.flush();

            int numTypes = vncIn.read() & 0xFF;
            log.debug("VNC offered {} security type(s)", numTypes);
            if (numTypes == 0) {
                int len = readInt(vncIn);
                byte[] msg = readExact(vncIn, len);
                log.warn("VNC server error: {}", new String(msg, StandardCharsets.UTF_8));
                ws.close(CloseStatus.SERVER_ERROR);
                return;
            }

            byte[] types = readExact(vncIn, numTypes);
            log.debug("VNC security types: {}", Arrays.toString(types));
            if (!performNoAuth(vncIn, vncOut, types)) {
                ws.close(CloseStatus.SERVER_ERROR);
                return;
            }

            sendWs(ws, new byte[]{1, 1});
            byte[] clientTypeChoice = wsQueue.poll(10, TimeUnit.SECONDS);
            if (clientTypeChoice == null) throw new IOException("Client security choice timeout");

            sendWs(ws, new byte[]{0, 0, 0, 0});
            handshakeDone.set(true);

            byte[] queued;
            while ((queued = wsQueue.poll()) != null) {
                vncOut.write(queued);
                vncOut.flush();
            }

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

    private boolean performNoAuth(InputStream vncIn, OutputStream vncOut, byte[] types) throws IOException {
        boolean hasNone = false;
        for (byte t : types) {
            if ((t & 0xFF) == 1) {
                hasNone = true;
                break;
            }
        }

        if (!hasNone) {
            log.warn("VNC server did not offer no-auth security type. Offered={}", Arrays.toString(types));
            return false;
        }

        vncOut.write(1);
        vncOut.flush();

        byte[] result = readExact(vncIn, 4);
        int code = ((result[0] & 0xFF) << 24) | ((result[1] & 0xFF) << 16)
                | ((result[2] & 0xFF) << 8) | (result[3] & 0xFF);
        if (code != 0) {
            log.warn("VNC no-auth rejected by server. code={}", code);
            return false;
        }

        return true;
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
        return ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16)
                | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
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
    public void afterConnectionClosed(WebSocketSession ws, CloseStatus status) {
        cleanup(ws.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession ws, Throwable exception) {
        cleanup(ws.getId());
    }

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
