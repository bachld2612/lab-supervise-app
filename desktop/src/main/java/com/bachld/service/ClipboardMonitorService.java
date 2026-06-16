package com.bachld.service;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClipboardMonitorService implements TrackingService {

    private static final Logger log = LoggerFactory.getLogger(ClipboardMonitorService.class);

    private static final int ACTION_COPY = 1;
    private static final int ACTION_PASTE = 2;
    private static final int ACTION_CUT = 3;
    private static final int MAX_TEXT_BYTES = 10 * 1024;

    private static final int WH_KEYBOARD_LL = 13;
    private static final int WM_KEYDOWN = 0x0100;
    private static final int WM_SYSKEYDOWN = 0x0104;
    private static final int WM_QUIT = 0x0012;
    private static final int VK_CONTROL = 0x11;
    private static final int VK_SHIFT = 0x10;
    private static final int VK_C = 0x43;
    private static final int VK_V = 0x56;
    private static final int VK_X = 0x58;
    private static final int VK_INSERT = 0x2D;
    private static final int VK_DELETE = 0x2E;

    private final WebSocketService webSocketService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private volatile boolean running;
    private volatile String lastClipboardText;
    private volatile String lastSentFingerprint;
    private volatile long lastSentAt;
    private volatile int pendingClipboardAction = ACTION_COPY;
    private volatile long pendingClipboardActionAt;
    private Thread keyboardHookThread;
    private int keyboardHookThreadId;
    private HANDLE keyboardHook;
    private LowLevelKeyboardProc keyboardProc;

    public ClipboardMonitorService(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @Override
    public void start() {
        lastClipboardText = readClipboardText();
        running = true;
        startClipboardWatcher();
        startKeyboardHook();
    }

    @Override
    public void stop() {
        running = false;
        scheduler.shutdownNow();
        if (keyboardHook != null) {
            User32Ex.INSTANCE.UnhookWindowsHookEx(keyboardHook);
            keyboardHook = null;
        }
        if (keyboardHookThreadId != 0) {
            User32Ex.INSTANCE.PostThreadMessageW(keyboardHookThreadId, WM_QUIT, new WinDef.WPARAM(0), new WinDef.LPARAM(0));
        }
    }

    private void startClipboardWatcher() {
        scheduler.scheduleWithFixedDelay(() -> {
            if (!running) return;
            String text = readClipboardText();
            if (text == null || Objects.equals(text, lastClipboardText)) {
                return;
            }
            lastClipboardText = text;
            int action = recentPendingAction();
            sendClipboardEvent(action, text);
        }, 300, 500, TimeUnit.MILLISECONDS);
    }

    private void startKeyboardHook() {
        keyboardHookThread = new Thread(() -> {
            keyboardHookThreadId = Kernel32.INSTANCE.GetCurrentThreadId();
            keyboardProc = (nCode, wParam, info) -> {
                if (nCode >= 0 && isKeyDownMessage(wParam)) {
                    handleKeyDown(info.vkCode);
                }
                return User32Ex.INSTANCE.CallNextHookEx(keyboardHook, nCode, wParam, info);
            };

            keyboardHook = User32Ex.INSTANCE.SetWindowsHookExW(WH_KEYBOARD_LL, keyboardProc, null, 0);
            if (keyboardHook == null) {
                log.warn("Could not install clipboard keyboard hook. Native error={}", Kernel32.INSTANCE.GetLastError());
                return;
            }

            WinUser.MSG msg = new WinUser.MSG();
            while (User32Ex.INSTANCE.GetMessageW(msg, null, 0, 0)) {
                User32.INSTANCE.TranslateMessage(msg);
                User32.INSTANCE.DispatchMessage(msg);
            }
        }, "ClipboardKeyboardHookThread");
        keyboardHookThread.setDaemon(true);
        keyboardHookThread.start();
    }

    private boolean isKeyDownMessage(WinDef.WPARAM wParam) {
        int message = wParam.intValue();
        return message == WM_KEYDOWN || message == WM_SYSKEYDOWN;
    }

    private void handleKeyDown(int vkCode) {
        boolean ctrl = isPressed(VK_CONTROL);
        boolean shift = isPressed(VK_SHIFT);

        if ((ctrl && (vkCode == VK_C || vkCode == VK_INSERT))) {
            markPendingAction(ACTION_COPY);
            sendAfterClipboardUpdates(ACTION_COPY);
        } else if ((ctrl && vkCode == VK_X) || (shift && vkCode == VK_DELETE)) {
            markPendingAction(ACTION_CUT);
            sendAfterClipboardUpdates(ACTION_CUT);
        } else if ((ctrl && vkCode == VK_V) || (shift && vkCode == VK_INSERT)) {
            sendAfterClipboardUpdates(ACTION_PASTE);
        }
    }

    private boolean isPressed(int virtualKey) {
        return (User32Ex.INSTANCE.GetAsyncKeyState(virtualKey) & 0x8000) != 0;
    }

    private void markPendingAction(int action) {
        pendingClipboardAction = action;
        pendingClipboardActionAt = System.currentTimeMillis();
    }

    private int recentPendingAction() {
        long ageMs = System.currentTimeMillis() - pendingClipboardActionAt;
        return ageMs <= 1500 ? pendingClipboardAction : ACTION_COPY;
    }

    private void sendAfterClipboardUpdates(int action) {
        try {
            scheduler.schedule(() -> {
                String text = readClipboardText();
                if (text != null) {
                    sendClipboardEvent(action, text);
                }
            }, 150, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ignored) {
        }
    }

    private void sendClipboardEvent(int action, String text) {
        String fingerprint = action + ":" + text;
        long now = System.currentTimeMillis();
        if (fingerprint.equals(lastSentFingerprint) && now - lastSentAt < 1000) {
            return;
        }
        lastSentFingerprint = fingerprint;
        lastSentAt = now;

        String appName = getForegroundApplicationName();
        webSocketService.sendClipboardEvent(appName, action, text);
    }

    private String readClipboardText() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)
                    || clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)
                    || !clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                return null;
            }
            Object value = clipboard.getData(DataFlavor.stringFlavor);
            if (!(value instanceof String text) || text.isEmpty()) {
                return null;
            }
            if (text.getBytes(StandardCharsets.UTF_8).length > MAX_TEXT_BYTES) {
                return null;
            }
            return text;
        } catch (Exception e) {
            return null;
        }
    }

    private String getForegroundApplicationName() {
        HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        String title = getWindowTitle(hwnd);
        if (title != null && !title.isBlank()) {
            return title;
        }
        return getProcessName(hwnd);
    }

    private String getWindowTitle(HWND hwnd) {
        if (hwnd == null) return "";
        char[] buffer = new char[1024];
        int length = User32.INSTANCE.GetWindowText(hwnd, buffer, buffer.length);
        return length > 0 ? new String(buffer, 0, length).trim() : "";
    }

    private String getProcessName(HWND hwnd) {
        if (hwnd == null) return "Unknown";
        IntByReference processId = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, processId);
        HANDLE process = Kernel32.INSTANCE.OpenProcess(Kernel32.PROCESS_QUERY_LIMITED_INFORMATION, false, processId.getValue());
        if (process != null) {
            try {
                char[] buffer = new char[1024];
                IntByReference size = new IntByReference(buffer.length);
                if (Kernel32.INSTANCE.QueryFullProcessImageName(process, 0, buffer, size)) {
                    String path = new String(buffer, 0, size.getValue());
                    return path.substring(path.lastIndexOf('\\') + 1);
                }
            } finally {
                Kernel32.INSTANCE.CloseHandle(process);
            }
        }
        return "Unknown";
    }

    interface User32Ex extends Library {
        User32Ex INSTANCE = Native.load("user32", User32Ex.class);

        HANDLE SetWindowsHookExW(int idHook, LowLevelKeyboardProc lpfn, WinDef.HMODULE hMod, int dwThreadId);

        WinDef.LRESULT CallNextHookEx(HANDLE hhk, int nCode, WinDef.WPARAM wParam, KbdLlHookStruct lParam);

        boolean UnhookWindowsHookEx(HANDLE hhk);

        boolean GetMessageW(WinUser.MSG lpMsg, HWND hWnd, int wMsgFilterMin, int wMsgFilterMax);

        boolean PostThreadMessageW(int idThread, int msg, WinDef.WPARAM wParam, WinDef.LPARAM lParam);

        short GetAsyncKeyState(int vKey);
    }

    interface LowLevelKeyboardProc extends Callback {
        WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, KbdLlHookStruct lParam);
    }

    @Structure.FieldOrder({"vkCode", "scanCode", "flags", "time", "dwExtraInfo"})
    public static class KbdLlHookStruct extends Structure {
        public int vkCode;
        public int scanCode;
        public int flags;
        public int time;
        public BaseTSD.ULONG_PTR dwExtraInfo;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("vkCode", "scanCode", "flags", "time", "dwExtraInfo");
        }
    }
}
