package com.bachld.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VncWatchdog {

    private static final Logger log = LoggerFactory.getLogger(VncWatchdog.class);
    private static final long CHECK_INTERVAL_SECONDS = 60;
    private static final int VNC_PORT = 5900;

    private final VncService vncService;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "vnc-watchdog");
                t.setDaemon(true);
                return t;
            });

    public VncWatchdog(VncService vncService) {
        this.vncService = vncService;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkHealth,
                0, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("VNC watchdog started (interval={}s)", CHECK_INTERVAL_SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private void checkHealth() {
        if (isPortListening()) return;

        log.warn("Watchdog: port {} not listening - restarting UltraVNC", VNC_PORT);
        if (!vncService.start()) {
            log.error("Watchdog: UltraVNC restart failed");
        }
    }

    private boolean isPortListening() {
        try (Socket probe = new Socket("127.0.0.1", VNC_PORT)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
