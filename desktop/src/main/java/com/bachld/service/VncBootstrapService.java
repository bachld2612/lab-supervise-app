package com.bachld.service;

import com.bachld.client.VncBootstrapApiClient;
import com.bachld.exception.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sends the local UltraVNC password to the backend so it can be stored encrypted
 * on the student's PersonalComputer record.
 */
public class VncBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(VncBootstrapService.class);
    private static final long RETRY_INTERVAL_SECONDS = 15;

    private final VncBootstrapApiClient apiClient;
    private final AtomicReference<String> pendingPassword = new AtomicReference<>();
    private final AtomicBoolean retryLoopStarted = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "vnc-bootstrap-retry");
                t.setDaemon(true);
                return t;
            });

    public VncBootstrapService(VncBootstrapApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * One-shot POST /api/vnc/v1/register.
     */
    public boolean registerPassword(String password) {
        if (password == null || password.isBlank()) {
            log.warn("VncBootstrapService: no password to register (winvnc start may have failed)");
            return false;
        }
        try {
            apiClient.registerPassword(password);
            log.info("VNC password registered with backend");
            return true;
        } catch (RestClientException e) {
            log.warn("Backend VNC registration failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Overwrites the backend password and keeps retrying until the registration succeeds.
     */
    public void registerPasswordWithRetry(String password) {
        if (password == null || password.isBlank()) {
            log.warn("VncBootstrapService: no password to register with retry");
            return;
        }

        pendingPassword.set(password);
        startRetryLoop();
        submitImmediateRetry();
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private void startRetryLoop() {
        if (!retryLoopStarted.compareAndSet(false, true)) {
            return;
        }

        scheduler.scheduleWithFixedDelay(
                this::registerPendingPasswordSafely,
                RETRY_INTERVAL_SECONDS,
                RETRY_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
        log.info("VNC password registration retry loop started (interval={}s)", RETRY_INTERVAL_SECONDS);
    }

    private void submitImmediateRetry() {
        try {
            scheduler.execute(this::registerPendingPasswordSafely);
        } catch (RejectedExecutionException e) {
            log.warn("VNC password retry scheduler is stopped");
        }
    }

    private void registerPendingPasswordSafely() {
        String password = pendingPassword.get();
        if (password == null || password.isBlank()) {
            return;
        }

        boolean registered = registerPassword(password);
        if (registered && pendingPassword.compareAndSet(password, null)) {
            log.info("Pending VNC password registration completed");
        }
    }
}
