package com.bachld.service;

import com.bachld.service.vnc.VncBootstrapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;

public class VncService {

    private static final Logger log = LoggerFactory.getLogger(VncService.class);
    private static final int VNC_PORT = 5900;

    private static final VncService INSTANCE = new VncService();

    private VncService() {}

    public static VncService getInstance() {
        return INSTANCE;
    }

    public synchronized boolean start() {
        log.info("VncService.start - ensuring UltraVNC is ready");
        try {
            new VncBootstrapService().ensureReady();
        } catch (Exception e) {
            log.error("VNC bootstrap failed: {}", e.getMessage(), e);
            return false;
        }
        return isPortListening();
    }

    public synchronized void stop() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            return;
        }

        log.info("VncService.stop - stopping UltraVNC");
        try {
            new VncBootstrapService().stop();
        } catch (Exception e) {
            log.warn("Failed to stop UltraVNC cleanly: {}", e.getMessage(), e);
        }
    }

    private boolean isPortListening() {
        try (Socket s = new Socket("127.0.0.1", VNC_PORT)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
