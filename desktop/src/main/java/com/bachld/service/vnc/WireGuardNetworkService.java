package com.bachld.service.vnc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class WireGuardNetworkService {

    private static final Logger log = LoggerFactory.getLogger(WireGuardNetworkService.class);

    private final CommandRunner runner;

    public WireGuardNetworkService(CommandRunner runner) {
        this.runner = runner;
    }

    public void ensureVpnRoute(String vpnSubnet) {
        log.info("Checking WireGuard VPN route for subnet {}", vpnSubnet);

        String normalizedSubnet = normalizeSubnet(vpnSubnet);

        // Check if route already exists
        CommandResult check = runner.run(List.of("powershell.exe", "-Command",
                String.format("Get-NetRoute | Where-Object { $_.DestinationPrefix -eq '%s' -or $_.DestinationPrefix -eq '%s' } | Select-Object -First 1",
                        vpnSubnet, normalizedSubnet)));

        if (check.stdout != null && !check.stdout.isBlank()) {
            log.info("VPN route for {} already present", normalizedSubnet);
            return;
        }

        addRouteViaWireGuardAdapter(normalizedSubnet);
    }

    private void addRouteViaWireGuardAdapter(String subnet) {
        CommandResult adapterQuery = runner.run(List.of("powershell.exe", "-Command",
                "Get-NetAdapter | Where-Object { $_.InterfaceDescription -like '*WireGuard*' -or $_.Name -like '*WireGuard*' } | Select-Object -First 1 -ExpandProperty Name"));

        String ifName = adapterQuery.stdout == null ? "" : adapterQuery.stdout.trim();
        if (ifName.isEmpty()) {
            log.info("No WireGuard adapter detected — VPN route management skipped");
            return;
        }

        log.info("WireGuard adapter '{}' found — adding route for {}", ifName, subnet);
        CommandResult add = runner.run(List.of("powershell.exe", "-Command",
                String.format("New-NetRoute -DestinationPrefix '%s' -InterfaceAlias '%s' -NextHop '0.0.0.0' -ErrorAction SilentlyContinue",
                        subnet, ifName)));

        if (add.success()) {
            log.info("VPN route added: {} via {}", subnet, ifName);
        } else {
            log.warn("Could not add VPN route (exit={})", add.exitCode);
        }
    }

    // 10.0.1.0/16 → 10.0.0.0/16 (mask bits to get network address)
    private String normalizeSubnet(String subnet) {
        try {
            String[] parts = subnet.split("/");
            if (parts.length != 2) return subnet;
            int prefix = Integer.parseInt(parts[1].trim());
            String[] octets = parts[0].trim().split("\\.");
            if (octets.length != 4) return subnet;
            long ip = (Long.parseLong(octets[0]) << 24) | (Long.parseLong(octets[1]) << 16)
                    | (Long.parseLong(octets[2]) << 8) | Long.parseLong(octets[3]);
            long mask = prefix == 0 ? 0L : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
            long network = ip & mask;
            return String.format("%d.%d.%d.%d/%d",
                    (network >> 24) & 0xFF, (network >> 16) & 0xFF,
                    (network >> 8) & 0xFF, network & 0xFF, prefix);
        } catch (Exception e) {
            return subnet;
        }
    }
}
