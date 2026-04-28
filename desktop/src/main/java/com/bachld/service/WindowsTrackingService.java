package com.bachld.service;

import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser.WinEventProc;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WindowsTrackingService uses JNA to hook into Windows OS events.
 * It listens for EVENT_SYSTEM_FOREGROUND to detect when the user switches applications.
 * 
 * Performance: Event-driven approach ensures near 0% CPU usage.
 */
public class WindowsTrackingService {
    private static final Logger log = LoggerFactory.getLogger(WindowsTrackingService.class);
    
    // Windows constants for hook
    private static final int EVENT_SYSTEM_FOREGROUND = 0x0003;
    private static final int EVENT_OBJECT_NAMECHANGE = 0x800C;
    private static final int WINEVENT_OUTOFCONTEXT = 0x0000;
    private static final int OBJID_WINDOW = 0x00000000;

    private final WebSocketService webSocketService;
    private HANDLE hHook; // Using HANDLE as HWINEVENTHOOK might not be directly exposed in some JNA versions
    private HANDLE hNameChangeHook;
    private WinEventProc listener;
    private final AtomicReference<String> lastApp = new AtomicReference<>("");
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public WindowsTrackingService(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    /**
     * Starts the Windows event hook in a background thread.
     * Note: This requires a Windows message loop to function.
     */
    public void start() {
        Thread hookThread = new Thread(() -> {
            log.info("Starting Windows Event Hook for foreground tracking...");
            
            // Define the event processor
            listener = new WinEventProc() {
                @Override
                public void callback(HANDLE hWinEventHook, WinDef.DWORD event, HWND hwnd, 
                                     WinDef.LONG idObject, WinDef.LONG idChild, 
                                     WinDef.DWORD dwEventThread, WinDef.DWORD dwmsEventTime) {
                    if (hwnd != null) {
                        try {
                            int eventCode = event.intValue();
                            if (eventCode == EVENT_SYSTEM_FOREGROUND) {
                                handleWindowChange(hwnd);
                            } else if (eventCode == EVENT_OBJECT_NAMECHANGE) {
                                // Only process if it's the main window title changing (OBJID_WINDOW)
                                if (idObject.intValue() == OBJID_WINDOW) {
                                    // And only if this window is currently in the foreground
                                    HWND foregroundWindow = User32.INSTANCE.GetForegroundWindow();
                                    if (hwnd.equals(foregroundWindow)) {
                                        handleWindowChange(hwnd);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error processing window change: {}", e.getMessage());
                        }
                    }
                }
            };

            // Register the hooks
            hHook = User32.INSTANCE.SetWinEventHook(
                    EVENT_SYSTEM_FOREGROUND, EVENT_SYSTEM_FOREGROUND,
                    null, listener, 0, 0, WINEVENT_OUTOFCONTEXT);

            hNameChangeHook = User32.INSTANCE.SetWinEventHook(
                    EVENT_OBJECT_NAMECHANGE, EVENT_OBJECT_NAMECHANGE,
                    null, listener, 0, 0, WINEVENT_OUTOFCONTEXT);

            if (hHook == null || hNameChangeHook == null) {
                log.error("Failed to set WinEventHook. Native error code: {}", Kernel32.INSTANCE.GetLastError());
                return;
            }

            // Windows message loop (Vital for hooks)
            WinUser.MSG msg = new WinUser.MSG();
            while (User32.INSTANCE.GetMessage(msg, null, 0, 0) != 0) {
                User32.INSTANCE.TranslateMessage(msg);
                User32.INSTANCE.DispatchMessage(msg);
            }
            
            log.info("Windows Event Hook thread exiting.");
        }, "WindowsTrackingThread");
        
        hookThread.setDaemon(true);
        hookThread.start();
    }

    /**
     * Handles the window change event with debouncing.
     */
    private void handleWindowChange(HWND hwnd) {
        String procName = getProcessName(hwnd);
        String title = getWindowTitle(hwnd);
        
        // Final tracking name: procName - title (e.g. brave.exe - YouTube)
        final String currentAppInfo = (title != null && !title.isEmpty()) 
                ? title 
                : procName;

        if (currentAppInfo == null || currentAppInfo.equals("Unknown")) return;

        // Skip if same as last app to avoid redundant events
        if (!currentAppInfo.equals(lastApp.get())) {
            lastApp.set(currentAppInfo);
            
            // Optimization: Debounce 500ms as per real-time-tracking.md
            scheduler.schedule(() -> {
                // Verify the app/window is still in focus after the delay
                HWND activeHwnd = User32.INSTANCE.GetForegroundWindow();
                String activeProc = getProcessName(activeHwnd);
                String activeTitle = getWindowTitle(activeHwnd);
                String activeAppInfo = (activeTitle != null && !activeTitle.isEmpty()) 
                        ? activeTitle 
                        : activeProc;
                
                if (currentAppInfo.equals(activeAppInfo)) {
                    log.info("Application Focus Change: {}", currentAppInfo);
                    webSocketService.sendPCInfo(currentAppInfo);
                }
            }, 100, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Retrieves the window title (tab name).
     */
    private String getWindowTitle(HWND hwnd) {
        if (hwnd == null) return "";
        char[] buffer = new char[1024];
        int length = User32.INSTANCE.GetWindowText(hwnd, buffer, buffer.length);
        return length > 0 ? new String(buffer, 0, length).trim() : "";
    }

    /**
     * Retrieves the executable name of the process owning the given window.
     */
    private String getProcessName(HWND hwnd) {
        if (hwnd == null) return "Unknown";
        
        IntByReference processId = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, processId);
        
        HANDLE process = Kernel32.INSTANCE.OpenProcess(
                Kernel32.PROCESS_QUERY_LIMITED_INFORMATION, 
                false, processId.getValue());
        
        if (process != null) {
            try {
                char[] buffer = new char[1024];
                IntByReference size = new IntByReference(buffer.length);
                if (Kernel32.INSTANCE.QueryFullProcessImageName(process, 0, buffer, size)) {
                    String path = new String(buffer, 0, size.getValue());
                    // Return just the executable file name (e.g. chrome.exe)
                    return path.substring(path.lastIndexOf('\\') + 1);
                }
            } finally {
                Kernel32.INSTANCE.CloseHandle(process);
            }
        }
        return "Unknown";
    }

    /**
     * Stops the hook and shuts down the scheduler.
     */
    public void stop() {
        if (hHook != null) {
            User32.INSTANCE.UnhookWinEvent(hHook);
            hHook = null;
        }
        if (hNameChangeHook != null) {
            User32.INSTANCE.UnhookWinEvent(hNameChangeHook);
            hNameChangeHook = null;
        }
        log.info("Windows Event Hooks removed.");
        scheduler.shutdown();
    }
}
