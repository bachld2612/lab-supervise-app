package com.bachld.service;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Linux X11 window tracking service using JNA + Xlib — no external tools required.
 *
 * Detection strategy mirrors Windows:
 *  - EVENT_SYSTEM_FOREGROUND equivalent: PropertyNotify on root window, atom = _NET_ACTIVE_WINDOW
 *  - EVENT_OBJECT_NAMECHANGE equivalent: PropertyNotify on active window, atom = _NET_WM_NAME
 *
 * Thread safety: all Xlib calls run on a single dedicated thread (LinuxX11TrackingThread).
 * The scheduler only reads AtomicReference — no cross-thread Xlib calls.
 */
public class LinuxX11TrackingService implements TrackingService {

    private static final Logger log = LoggerFactory.getLogger(LinuxX11TrackingService.class);

    // X11 event types
    private static final int PROPERTY_NOTIFY = 28;

    // X11 event mask bits
    private static final long PROPERTY_CHANGE_MASK = 1L << 22; // 0x400000

    // X11 predefined atom IDs (Xatom.h)
    private static final long XA_STRING   = 31L;
    private static final long XA_CARDINAL = 6L;
    private static final long XA_WINDOW   = 33L;

    private final WebSocketService webSocketService;
    private volatile boolean running = false;
    private Thread trackingThread;
    private final AtomicReference<String> lastApp = new AtomicReference<>("");
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // ── JNA: minimal Xlib interface ───────────────────────────────────────────

    interface Xlib extends Library {
        Xlib INSTANCE = Native.load("X11", Xlib.class);

        int    XInitThreads();
        Pointer XOpenDisplay(String displayName);
        int    XCloseDisplay(Pointer display);
        long   XDefaultRootWindow(Pointer display);
        int    XSelectInput(Pointer display, long window, NativeLong eventMask);
        int    XPending(Pointer display);
        int    XNextEvent(Pointer display, XEvent event);
        long   XInternAtom(Pointer display, String atomName, boolean onlyIfExists);
        int    XGetWindowProperty(
                   Pointer display, long window, long property,
                   NativeLong longOffset, NativeLong longLength,
                   boolean delete, long reqType,
                   NativeLongByReference actualTypeReturn,
                   IntByReference actualFormatReturn,
                   NativeLongByReference nitemsReturn,
                   NativeLongByReference bytesAfterReturn,
                   PointerByReference propReturn);
        int    XFree(Pointer data);
        int    XFlush(Pointer display);
    }

    // ── JNA structures ────────────────────────────────────────────────────────

    /**
     * Generic XEvent container. XEvent is a C union whose largest member is
     * `long pad[24]`, so 24 × Native.LONG_SIZE bytes covers every variant.
     */
    public static class XEvent extends Structure {
        public byte[] data = new byte[24 * Native.LONG_SIZE];

        public XEvent() {}

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("data");
        }

        /** Event type is always the first 4 bytes of every XEvent variant. */
        public int getType() {
            return getPointer().getInt(0);
        }

        public XPropertyEvent asPropertyEvent() {
            return new XPropertyEvent(getPointer());
        }
    }

    /**
     * XPropertyEvent — C layout on 64-bit Linux:
     *
     *  offset  0: int type          (4 bytes)
     *  offset  4: [4-byte padding]
     *  offset  8: unsigned long serial  (8 bytes)
     *  offset 16: Bool send_event   (int, 4 bytes)
     *  offset 20: [4-byte padding]
     *  offset 24: Display* display  (8 bytes)
     *  offset 32: Window window     (unsigned long, 8 bytes)
     *  offset 40: Atom atom         (unsigned long, 8 bytes)
     *  offset 48: Time time         (unsigned long, 8 bytes)
     *  offset 56: int state         (4 bytes)
     *  total: 64 bytes
     *
     * JNA inserts the required padding automatically via ALIGN_DEFAULT.
     */
    public static class XPropertyEvent extends Structure {
        public int        type;
        public NativeLong serial;
        public int        send_event;
        public Pointer    display;
        public NativeLong window;
        public NativeLong atom;
        public NativeLong time;
        public int        state;

        public XPropertyEvent() {}

        public XPropertyEvent(Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("type", "serial", "send_event", "display",
                                 "window", "atom", "time", "state");
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public LinuxX11TrackingService(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @Override
    public void start() {
        running = true;
        trackingThread = new Thread(this::runEventLoop, "LinuxX11TrackingThread");
        trackingThread.setDaemon(true);
        trackingThread.start();
    }

    @Override
    public void stop() {
        running = false;
        if (trackingThread != null) {
            trackingThread.interrupt();
        }
        scheduler.shutdown();
    }

    // ── Event loop (single-threaded Xlib access) ──────────────────────────────

    private void runEventLoop() {
        Xlib x11;
        try {
            x11 = Xlib.INSTANCE;
        } catch (UnsatisfiedLinkError e) {
            log.error("libX11 not found — X11 tracking unavailable. Is libx11-dev installed?");
            return;
        }

        x11.XInitThreads();

        Pointer display = x11.XOpenDisplay(null); // uses $DISPLAY env var
        if (display == null) {
            log.error("XOpenDisplay failed — is $DISPLAY set?");
            return;
        }

        try {
            long root = x11.XDefaultRootWindow(display);

            // Intern all atoms once at startup
            long atomNetActiveWindow = x11.XInternAtom(display, "_NET_ACTIVE_WINDOW", false);
            long atomNetWmName       = x11.XInternAtom(display, "_NET_WM_NAME",        false);
            long atomWmName          = x11.XInternAtom(display, "WM_NAME",             false);
            long atomNetWmPid        = x11.XInternAtom(display, "_NET_WM_PID",         false);
            long atomUtf8String      = x11.XInternAtom(display, "UTF8_STRING",         false);

            // Subscribe to root window for active-window changes
            x11.XSelectInput(display, root, new NativeLong(PROPERTY_CHANGE_MASK));
            x11.XFlush(display);

            log.info("Linux X11 tracking started (event-driven)");

            long subscribedWindowId = 0L;
            XEvent event = new XEvent();

            while (running) {
                if (x11.XPending(display) > 0) {
                    x11.XNextEvent(display, event);
                    event.read();

                    if (event.getType() != PROPERTY_NOTIFY) continue;

                    XPropertyEvent pe = event.asPropertyEvent();
                    long atom   = pe.atom.longValue();
                    long window = pe.window.longValue();

                    if (atom == atomNetActiveWindow) {
                        // Foreground app changed
                        long newWindowId = readActiveWindowId(x11, display, root, atomNetActiveWindow);
                        if (newWindowId != 0 && newWindowId != subscribedWindowId) {
                            if (subscribedWindowId != 0) {
                                x11.XSelectInput(display, subscribedWindowId, new NativeLong(0));
                            }
                            subscribedWindowId = newWindowId;
                            // Subscribe to title changes on the new active window
                            x11.XSelectInput(display, newWindowId, new NativeLong(PROPERTY_CHANGE_MASK));
                            x11.XFlush(display);

                            onWindowChanged(x11, display, newWindowId,
                                atomNetWmName, atomWmName, atomNetWmPid, atomUtf8String);
                        }

                    } else if ((atom == atomNetWmName || atom == atomWmName)
                               && window == subscribedWindowId) {
                        // Title changed on the current active window (e.g. browser tab switch)
                        onWindowChanged(x11, display, subscribedWindowId,
                            atomNetWmName, atomWmName, atomNetWmPid, atomUtf8String);
                    }

                } else {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

        } finally {
            x11.XCloseDisplay(display);
            log.info("Linux X11 tracking stopped");
        }
    }

    // ── Window change handler ─────────────────────────────────────────────────

    private void onWindowChanged(Xlib x11, Pointer display, long windowId,
                                  long atomNetWmName, long atomWmName,
                                  long atomNetWmPid, long atomUtf8String) {
        String title = readWindowTitle(x11, display, windowId, atomNetWmName, atomWmName, atomUtf8String);
        String proc  = readProcessName(x11, display, windowId, atomNetWmPid);

        String appInfo = (title != null && !title.isEmpty()) ? title : proc;
        if (appInfo == null || appInfo.isEmpty()) return;
        if (appInfo.equals(lastApp.get())) return;

        lastApp.set(appInfo);

        // 100 ms debounce: only dispatch if still the latest change
        scheduler.schedule(() -> {
            if (appInfo.equals(lastApp.get())) {
                log.info("Application Focus Change: {}", appInfo);
                webSocketService.sendPCInfo(appInfo);
            }
        }, 100, TimeUnit.MILLISECONDS);
    }

    // ── Xlib property readers ─────────────────────────────────────────────────

    private long readActiveWindowId(Xlib x11, Pointer display, long root, long atomNetActiveWindow) {
        NativeLongByReference actualType   = new NativeLongByReference();
        IntByReference         actualFormat = new IntByReference();
        NativeLongByReference  nItems       = new NativeLongByReference();
        NativeLongByReference  bytesAfter   = new NativeLongByReference();
        PointerByReference     propReturn   = new PointerByReference();

        int rc = x11.XGetWindowProperty(
            display, root, atomNetActiveWindow,
            new NativeLong(0), new NativeLong(1), false, XA_WINDOW,
            actualType, actualFormat, nItems, bytesAfter, propReturn);

        if (rc == 0 && propReturn.getValue() != null) {
            Pointer ptr = propReturn.getValue();
            long windowId = ptr.getNativeLong(0).longValue();
            x11.XFree(ptr);
            return windowId;
        }
        return 0L;
    }

    private String readWindowTitle(Xlib x11, Pointer display, long window,
                                    long atomNetWmName, long atomWmName, long atomUtf8String) {
        NativeLongByReference actualType   = new NativeLongByReference();
        IntByReference         actualFormat = new IntByReference();
        NativeLongByReference  nItems       = new NativeLongByReference();
        NativeLongByReference  bytesAfter   = new NativeLongByReference();
        PointerByReference     propReturn   = new PointerByReference();

        // Prefer _NET_WM_NAME (UTF-8)
        int rc = x11.XGetWindowProperty(
            display, window, atomNetWmName,
            new NativeLong(0), new NativeLong(1024), false, atomUtf8String,
            actualType, actualFormat, nItems, bytesAfter, propReturn);

        if (rc == 0 && propReturn.getValue() != null) {
            Pointer ptr = propReturn.getValue();
            int len = (int) nItems.getValue().longValue();
            if (len > 0) {
                String title = new String(ptr.getByteArray(0, len), StandardCharsets.UTF_8).trim();
                x11.XFree(ptr);
                return title;
            }
            x11.XFree(ptr);
        }

        // Fallback: WM_NAME (Latin-1 / ASCII)
        rc = x11.XGetWindowProperty(
            display, window, atomWmName,
            new NativeLong(0), new NativeLong(1024), false, XA_STRING,
            actualType, actualFormat, nItems, bytesAfter, propReturn);

        if (rc == 0 && propReturn.getValue() != null) {
            Pointer ptr = propReturn.getValue();
            String title = ptr.getString(0);
            x11.XFree(ptr);
            return title != null ? title.trim() : null;
        }

        return null;
    }

    private String readProcessName(Xlib x11, Pointer display, long window, long atomNetWmPid) {
        NativeLongByReference actualType   = new NativeLongByReference();
        IntByReference         actualFormat = new IntByReference();
        NativeLongByReference  nItems       = new NativeLongByReference();
        NativeLongByReference  bytesAfter   = new NativeLongByReference();
        PointerByReference     propReturn   = new PointerByReference();

        int rc = x11.XGetWindowProperty(
            display, window, atomNetWmPid,
            new NativeLong(0), new NativeLong(1), false, XA_CARDINAL,
            actualType, actualFormat, nItems, bytesAfter, propReturn);

        if (rc == 0 && propReturn.getValue() != null) {
            Pointer ptr = propReturn.getValue();
            int pid = ptr.getNativeLong(0).intValue();
            x11.XFree(ptr);

            if (pid > 0) {
                try {
                    return Files.readString(Path.of("/proc/" + pid + "/comm")).trim();
                } catch (IOException ignored) {}
            }
        }
        return null;
    }
}