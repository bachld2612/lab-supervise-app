package com.bachld.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.Enumeration;

public class VpnUtil {

    private static final Logger logger = LoggerFactory.getLogger(VpnUtil.class);

    /**
     * Returns the IPv4 address of the active WireGuard VPN tunnel, or null if not connected.
     *
     * Detection order (first match wins):
     *  1. WireGuard subnet 10.0.0.0/16 — matches the configured AllowedIPs range;
     *     most reliable on Windows where WinTUN does NOT set isPointToPoint()
     *  2. isPointToPoint() — standard tunnel flag (WireGuard on Linux/macOS)
     *  3. Interface name / display name matching — fallback for named adapters
     */
    public static String getActiveVpnIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) return null;

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                if (!iface.isUp()) continue;
                if (iface.isLoopback()) continue;

                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!(addr instanceof Inet4Address) || addr.isLoopbackAddress()) continue;

                    String ip = addr.getHostAddress();

                    if (isWireGuardRange(addr)) {
                        logger.info("WireGuard VPN detected (IP range 10.0.0.0/16): {} -> {}", iface.getName(), ip);
                        return ip;
                    }

                    if (iface.isPointToPoint()) {
                        logger.info("WireGuard VPN detected (point-to-point): {} -> {}", iface.getName(), ip);
                        return ip;
                    }

                    if (isVpnByName(iface)) {
                        logger.info("WireGuard VPN detected (by name): {} ({}) -> {}", iface.getName(), iface.getDisplayName(), ip);
                        return ip;
                    }
                }
            }
        } catch (SocketException e) {
            logger.warn("Failed to enumerate network interfaces: {}", e.getMessage());
        }
        return null;
    }

    /** WireGuard subnet: 10.0.0.0/16 → 10.0.x.x */
    private static boolean isWireGuardRange(InetAddress addr) {
        byte[] b = addr.getAddress();
        return (b[0] & 0xFF) == 10 && (b[1] & 0xFF) == 0;
    }

    /**
     * Name-based fallback: matches known VPN adapter names on Windows.
     * Explicitly excludes hypervisor and container bridges to avoid false positives.
     */
    private static boolean isVpnByName(NetworkInterface iface) {
        String name    = iface.getName().toLowerCase();
        String display = iface.getDisplayName().toLowerCase();

        if (display.contains("vmware") || display.contains("virtualbox")
                || display.contains("hyper-v") || display.contains("hyper v")
                || display.contains("virtual ethernet")
                || name.startsWith("docker") || name.startsWith("veth")
                || name.startsWith("br-") || name.startsWith("virbr")) {
            return false;
        }

        return name.startsWith("wg")
                || display.contains("wireguard")
                || display.contains("wiretunnel")
                || name.startsWith("tun") || name.startsWith("tap")
                || display.contains("openvpn") || display.contains("tap-windows")
                || display.contains("cisco anyconnect") || display.contains("cisco vpn");
    }
}