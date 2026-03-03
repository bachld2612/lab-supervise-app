package com.bachld.backend.service;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TrackingService {

    public List<String> getOpenBrowserTabs() {
        List<String> browserWindows = new ArrayList<>();

        User32.INSTANCE.EnumWindows(new WinUser.WNDENUMPROC() {
            @Override
            public boolean callback(WinDef.HWND hWnd, com.sun.jna.Pointer data) {
                if (User32.INSTANCE.IsWindowVisible(hWnd)) {

                    char[] buffer = new char[1024];
                    User32.INSTANCE.GetWindowText(hWnd, buffer, 1024);
                    String title = Native.toString(buffer);

                    if (!title.isEmpty()) {
                        browserWindows.add(title);
                    }
                }
                return true;
            }
        }, null);

        return browserWindows;
    }

}
