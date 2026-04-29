package com.bachld.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.Enumeration;

public class VpnUtil {

    private static final Logger logger = LoggerFactory.getLogger(VpnUtil.class);

    /**
     * Returns the IPv4 address of any active VPN tunnel, or null if none is connected.
     *
     * Detection order (first match wins):
     *  1. isPointToPoint() — standard tunnel flag (OpenVPN tun, WireGuard, Cisco AnyConnect on Linux/macOS)
     *  2. Tailscale CGNAT range 100.64.0.0/10 — Tailscale on Windows uses WinTUN which does NOT
     *     set isPointToPoint(), so IP-range detection is the reliable fallback
     *  3. Interface name / display name matching — catches TAP-mode OpenVPN and other named adapters
     *     on Windows that set neither flag
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

                    if (iface.isPointToPoint()) {
                        logger.info("VPN detected (point-to-point): {} -> {}", iface.getName(), ip);
                        return ip;
                    }

                    if (isTailscaleRange(addr)) {
                        logger.info("VPN detected (Tailscale CGNAT): {} -> {}", iface.getName(), ip);
                        return ip;
                    }

                    if (isVpnByName(iface)) {
                        logger.info("VPN detected (by name): {} ({}) -> {}", iface.getName(), iface.getDisplayName(), ip);
                        return ip;
                    }
                }
            }
        } catch (SocketException e) {
            logger.warn("Failed to enumerate network interfaces: {}", e.getMessage());
        }
        return null;
    }

    /** Tailscale CGNAT subnet: 100.64.0.0/10 → 100.64.x.x – 100.127.x.x */
    private static boolean isTailscaleRange(InetAddress addr) {
        byte[] b = addr.getAddress();
        int first  = b[0] & 0xFF;
        int second = b[1] & 0xFF;
        return first == 100 && second >= 64 && second <= 127;
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

        return name.startsWith("tun") || name.startsWith("tap") || name.startsWith("wg")
                || name.equals("tailscale0")
                || display.contains("openvpn") || display.contains("tap-windows")
                || display.contains("wireguard") || display.contains("tailscale")
                || display.contains("cisco anyconnect") || display.contains("cisco vpn");
    }
}