package com.bachld.service;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ScreenLockService {

    private static final Logger log = LoggerFactory.getLogger(ScreenLockService.class);
    private static final long LOCK_REAPPLY_INTERVAL_MS = 400;
    private static final ScreenLockService INSTANCE = new ScreenLockService();

    private final ExecutorService inputExecutor = Executors.newSingleThreadExecutor(daemonThreadFactory("screen-lock-input"));
    private final ScheduledExecutorService lockWatchdog = Executors.newSingleThreadScheduledExecutor(daemonThreadFactory("screen-lock-watchdog"));
    private final List<JWindow> overlays = new ArrayList<>();
    private volatile boolean lockRequested;
    private boolean inputBlocked;
    private Timer keepOnTopTimer;
    private ScheduledFuture<?> lockWatchdogTask;

    private ScreenLockService() {
        Runtime.getRuntime().addShutdownHook(daemonThreadFactory("screen-lock-cleanup").newThread(this::cleanupOnShutdown));
    }

    public static ScreenLockService getInstance() {
        return INSTANCE;
    }

    public void setLocked(boolean active) {
        Future<?> task = inputExecutor.submit(() -> {
            if (active) {
                lock();
            } else {
                unlock();
            }
        });

        try {
            task.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Screen lock command did not complete cleanly: {}", e.getMessage(), e);
        }
    }

    private void lock() {
        if (GraphicsEnvironment.isHeadless()) {
            log.warn("Cannot lock screen in headless environment");
            return;
        }

        if (lockRequested) {
            enforceOverlayVisibility();
            startLockWatchdog();
            reapplyInputBlock(false);
            return;
        }

        lockRequested = true;
        runOnEventThreadAndWait(this::showOverlays);
        if (isWindows()) {
            inputBlocked = setInputBlocked(true, true);
        } else {
            log.warn("Input blocking is only supported on Windows");
        }
        startLockWatchdog();
        log.info("Screen locked by remote command");
    }

    private void unlock() {
        lockRequested = false;
        stopLockWatchdog();
        if (inputBlocked) {
            setInputBlocked(false, true);
            inputBlocked = false;
        } else if (isWindows()) {
            setInputBlocked(false, true);
        }

        runOnEventThreadAndWait(this::hideOverlays);
        log.info("Screen unlocked by remote command");
    }

    private void cleanupOnShutdown() {
        try {
            Future<?> task = inputExecutor.submit(this::unlock);
            task.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Screen lock cleanup failed: {}", e.getMessage(), e);
        } finally {
            lockWatchdog.shutdownNow();
            inputExecutor.shutdownNow();
        }
    }

    private void startLockWatchdog() {
        if (lockWatchdogTask != null && !lockWatchdogTask.isDone()) {
            return;
        }

        lockWatchdogTask = lockWatchdog.scheduleWithFixedDelay(() -> {
            Future<?> task = inputExecutor.submit(() -> {
                if (!lockRequested) {
                    return;
                }
                enforceOverlayVisibility();
                reapplyInputBlock(false);
            });

            try {
                task.get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.debug("Screen lock watchdog reapply skipped: {}", e.getMessage());
            }
        }, LOCK_REAPPLY_INTERVAL_MS, LOCK_REAPPLY_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void stopLockWatchdog() {
        if (lockWatchdogTask != null) {
            lockWatchdogTask.cancel(false);
            lockWatchdogTask = null;
        }
    }

    private void reapplyInputBlock(boolean warnOnFailure) {
        if (!isWindows()) {
            return;
        }

        boolean result = setInputBlocked(true, warnOnFailure);
        if (result && !inputBlocked) {
            log.debug("Re-applied screen input lock");
        }
        inputBlocked = result;
    }

    private void showOverlays() {
        hideOverlays();

        GraphicsDevice[] devices = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getScreenDevices();

        for (GraphicsDevice device : devices) {
            Rectangle bounds = device.getDefaultConfiguration().getBounds();
            JWindow overlay = new JWindow(device.getDefaultConfiguration());
            overlay.setAlwaysOnTop(true);
            overlay.setFocusableWindowState(true);
            overlay.setBounds(bounds);
            overlay.setContentPane(createOverlayContent());
            overlay.addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowLostFocus(WindowEvent e) {
                    enforceOverlayVisibility();
                }

                @Override
                public void windowDeactivated(WindowEvent e) {
                    enforceOverlayVisibility();
                }
            });
            overlay.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentHidden(ComponentEvent e) {
                    enforceOverlayVisibility();
                }
            });
            overlays.add(overlay);
            overlay.setVisible(true);
            overlay.toFront();
            overlay.requestFocus();
        }

        keepOnTopTimer = new Timer(1000, e -> enforceOverlayVisibility());
        keepOnTopTimer.setRepeats(true);
        keepOnTopTimer.start();
    }

    private JPanel createOverlayContent() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(new Color(15, 23, 42));
        root.setBorder(BorderFactory.createLineBorder(new Color(30, 41, 59), 2));

        JLabel icon = new JLabel("\uD83D\uDD12", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 96));
        icon.setForeground(new Color(226, 232, 240));
        root.add(icon, new GridBagConstraints());
        return root;
    }

    private void hideOverlays() {
        if (keepOnTopTimer != null) {
            keepOnTopTimer.stop();
            keepOnTopTimer = null;
        }

        for (JWindow overlay : overlays) {
            overlay.setVisible(false);
            overlay.dispose();
        }
        overlays.clear();
    }

    private void enforceOverlayVisibility() {
        if (!lockRequested) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            bringOverlaysToFront();
        } else {
            SwingUtilities.invokeLater(this::bringOverlaysToFront);
        }
    }

    private void bringOverlaysToFront() {
        if (!lockRequested) {
            return;
        }
        for (Window overlay : overlays) {
            if (!overlay.isVisible()) {
                overlay.setVisible(true);
            }
            overlay.setAlwaysOnTop(true);
            overlay.toFront();
            overlay.requestFocus();
        }
    }

    private boolean setInputBlocked(boolean active, boolean warnOnFailure) {
        try {
            boolean result = User32BlockInput.INSTANCE.BlockInput(active);
            if (!result && warnOnFailure) {
                log.warn("BlockInput({}) returned false", active);
            }
            return result;
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            if (warnOnFailure) {
                log.warn("Windows BlockInput API is unavailable: {}", e.getMessage());
            }
            return false;
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private void runOnEventThreadAndWait(Runnable runnable) {
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                runnable.run();
            } else {
                SwingUtilities.invokeAndWait(runnable);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not update screen lock overlay", e);
        }
    }

    private interface User32BlockInput extends StdCallLibrary {
        User32BlockInput INSTANCE = Native.load("user32", User32BlockInput.class);

        boolean BlockInput(boolean active);
    }

    private static ThreadFactory daemonThreadFactory(String threadNamePrefix) {
        ThreadFactory defaultFactory = Executors.defaultThreadFactory();
        AtomicInteger counter = new AtomicInteger(1);

        return runnable -> {
            Thread thread = defaultFactory.newThread(runnable);
            thread.setName(threadNamePrefix + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
