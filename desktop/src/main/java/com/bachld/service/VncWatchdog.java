package com.bachld.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tự healing UltraVNC: nếu port 5900 không listen (winvnc bị kill / crash),
 * gọi {@link VncService#start()} để khởi động lại.
 *
 * <p>Lưu ý: restart sẽ sinh password MỚI → cần gọi lại bootstrap để update backend. Caller
 * pass vào {@link RecoveryHook} để xử lý việc đăng ký lại với backend.
 */
public class VncWatchdog {

    private static final Logger log = LoggerFactory.getLogger(VncWatchdog.class);

    private static final long CHECK_INTERVAL_SECONDS = 60;

    public interface RecoveryHook {
        void onPasswordChanged(String newPassword);
    }

    private final VncService vncService;
    private final RecoveryHook recoveryHook;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "vnc-watchdog");
                t.setDaemon(true);
                return t;
            });

    public VncWatchdog(VncService vncService, RecoveryHook recoveryHook) {
        this.vncService = vncService;
        this.recoveryHook = recoveryHook;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkHealth,
                CHECK_INTERVAL_SECONDS, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("VNC watchdog started (interval={}s)", CHECK_INTERVAL_SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private void checkHealth() {
        if (isPortListening()) return;

        log.warn("Watchdog: port 5900 not listening — restarting UltraVNC");
        String newPassword = vncService.start();
        if (newPassword != null && recoveryHook != null) {
            recoveryHook.onPasswordChanged(newPassword);
        }
    }

    private boolean isPortListening() {
        try (Socket probe = new Socket("127.0.0.1", 5900)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
