package com.bachld.service;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Scans for a specific set of WiFi SSIDs using the Windows WLAN API (wlanapi.dll) via JNA.
 * Called synchronously from a background thread during login to verify physical presence.
 */
public class WifiScannerService {
    private static final Logger log = LoggerFactory.getLogger(WifiScannerService.class);

    // Milliseconds to wait after WlanScan before reading results (scan is async)
    private static final int SCAN_WAIT_MS = 2500;

    // Byte offsets inside WLAN_AVAILABLE_NETWORK (628 bytes, Windows SDK)
    private static final int OFF_SSID_LEN   = 512;
    private static final int OFF_SSID_BYTES = 516;
    private static final int NETWORK_SIZE   = 628;

    // List header: dwNumberOfItems(4) + dwIndex(4) = 8 bytes
    // WLAN_INTERFACE_INFO: GUID(16) + WCHAR[256](512) + isState(4) = 532 bytes
    private static final int LIST_HDR       = 8;
    private static final int INTERFACE_SIZE = 532;

    interface Wlanapi extends Library {
        Wlanapi INSTANCE = Native.load("wlanapi", Wlanapi.class);

        int WlanOpenHandle(int dwClientVersion, Pointer pReserved,
                           IntByReference pdwNegotiatedVersion, PointerByReference phClientHandle);
        int WlanCloseHandle(Pointer hClientHandle, Pointer pReserved);
        int WlanEnumInterfaces(Pointer hClientHandle, Pointer pReserved,
                               PointerByReference ppInterfaceList);
        int WlanScan(Pointer hClientHandle, Pointer pInterfaceGuid,
                     Pointer pDot11Ssid, Pointer pIeData, Pointer pReserved);
        int WlanGetAvailableNetworkList(Pointer hClientHandle, Pointer pInterfaceGuid,
                                        int dwFlags, Pointer pReserved,
                                        PointerByReference ppAvailableNetworkList);
        void WlanFreeMemory(Pointer pMemory);
    }

    /**
     * Blocks until a matching SSID is found or the scan timeout expires.
     * Should be called from a background thread (e.g. SwingWorker.doInBackground).
     *
     * @param targetSsids list of SSIDs to look for
     * @return the first matching SSID found, or null if none found
     */
    public String findMatchingSsid(List<String> targetSsids) {
        if (targetSsids == null || targetSsids.isEmpty()) return null;

        PointerByReference phClient = new PointerByReference();
        IntByReference negotiated = new IntByReference();

        int err = Wlanapi.INSTANCE.WlanOpenHandle(2, null, negotiated, phClient);
        if (err != 0) {
            log.warn("WlanOpenHandle failed: {}", err);
            return null;
        }

        Pointer hClient = phClient.getValue();
        try {
            PointerByReference ppIfList = new PointerByReference();
            err = Wlanapi.INSTANCE.WlanEnumInterfaces(hClient, null, ppIfList);
            if (err != 0) {
                log.warn("WlanEnumInterfaces failed: {}", err);
                return null;
            }

            Pointer pIfList = ppIfList.getValue();
            try {
                int numIfaces = pIfList.getInt(0);
                if (numIfaces == 0) return null;

                // Optimistic pass: check cached scan results (instant, common happy path)
                String match = checkAllInterfaces(hClient, pIfList, numIfaces, targetSsids);
                if (match != null) {
                    log.info("WiFi match found in cache: {}", match);
                    return match;
                }

                // Fresh scan
                for (int i = 0; i < numIfaces; i++) {
                    Pointer pIface = pIfList.share(LIST_HDR + (long) i * INTERFACE_SIZE);
                    Wlanapi.INSTANCE.WlanScan(hClient, pIface, null, null, null);
                }
                Thread.sleep(SCAN_WAIT_MS);

                match = checkAllInterfaces(hClient, pIfList, numIfaces, targetSsids);
                if (match != null) {
                    log.info("WiFi match found after scan: {}", match);
                } else {
                    log.info("No matching WiFi SSID found");
                }
                return match;

            } finally {
                Wlanapi.INSTANCE.WlanFreeMemory(pIfList);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            Wlanapi.INSTANCE.WlanCloseHandle(hClient, null);
        }
    }

    private String checkAllInterfaces(Pointer hClient, Pointer pIfList, int numIfaces, List<String> targetSsids) {
        for (int i = 0; i < numIfaces; i++) {
            Pointer pIface = pIfList.share(LIST_HDR + (long) i * INTERFACE_SIZE);
            String match = checkInterface(hClient, pIface, targetSsids);
            if (match != null) return match;
        }
        return null;
    }

    private String checkInterface(Pointer hClient, Pointer pIfaceInfo, List<String> targetSsids) {
        PointerByReference ppNetList = new PointerByReference();
        int err = Wlanapi.INSTANCE.WlanGetAvailableNetworkList(hClient, pIfaceInfo, 0, null, ppNetList);
        if (err != 0) return null;

        Pointer pNetList = ppNetList.getValue();
        try {
            int numNetworks = pNetList.getInt(0);
            StringBuilder found = new StringBuilder("WiFi networks visible (").append(numNetworks).append("):");
            String match = null;
            for (int j = 0; j < numNetworks; j++) {
                Pointer pNet = pNetList.share(LIST_HDR + (long) j * NETWORK_SIZE);
                String ssid = readSsid(pNet);
                if (!ssid.isEmpty()) {
                    boolean isTarget = targetSsids.contains(ssid);
                    found.append("\n  ").append(j + 1).append(". '").append(ssid).append("'")
                         .append(isTarget ? " <-- MATCH" : "");
                    if (isTarget && match == null) match = ssid;
                }
            }
            log.info(found.toString());
            log.info("Target SSIDs: {}", targetSsids);
            return match;
        } finally {
            Wlanapi.INSTANCE.WlanFreeMemory(pNetList);
        }
    }

    private String readSsid(Pointer pNet) {
        int len = Math.max(0, Math.min(pNet.getInt(OFF_SSID_LEN), 32));
        if (len == 0) return "";
        return new String(pNet.getByteArray(OFF_SSID_BYTES, len), StandardCharsets.UTF_8).trim();
    }
}
