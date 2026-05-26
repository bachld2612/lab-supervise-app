package com.bachld.service.vnc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class WindowsFirewallService {

    private static final Logger log = LoggerFactory.getLogger(WindowsFirewallService.class);
    private static final String RULE_DISPLAY_NAME = "TLU Lab UltraVNC Inbound";

    private final CommandRunner runner;

    public WindowsFirewallService(CommandRunner runner) {
        this.runner = runner;
    }

    public void ensureVncFirewallRule(int port, String vpnSubnet) {
        log.info("Ensuring firewall rule for VNC port {} restricted to VPN subnet {}", port, vpnSubnet);

        // Remove any existing rule with the same display name
        runner.run(List.of("powershell.exe", "-Command",
                String.format("Remove-NetFirewallRule -DisplayName '%s' -ErrorAction SilentlyContinue",
                        RULE_DISPLAY_NAME)));

        // Create new rule restricted to VPN subnet
        String psCmd = String.format(
                "New-NetFirewallRule -DisplayName '%s' -Direction Inbound -Action Allow " +
                "-Protocol TCP -LocalPort %d -RemoteAddress %s -Profile Any",
                RULE_DISPLAY_NAME, port, vpnSubnet);

        CommandResult result = runner.run(List.of("powershell.exe", "-Command", psCmd));

        if (result.success()) {
            log.info("Firewall rule '{}' created — port={} remoteAddress={}", RULE_DISPLAY_NAME, port, vpnSubnet);
        } else {
            log.warn("PowerShell firewall rule failed (exit={}) — falling back to netsh", result.exitCode);
            ensureViaNetsh(port, vpnSubnet);
        }
    }

    private void ensureViaNetsh(int port, String vpnSubnet) {
        runner.run(List.of("netsh", "advfirewall", "firewall", "delete", "rule", "name=TLULabVNC"));
        CommandResult r = runner.run(List.of("netsh", "advfirewall", "firewall", "add", "rule",
                "name=TLULabVNC", "protocol=TCP", "dir=in",
                "localport=" + port, "remoteip=" + vpnSubnet,
                "action=allow", "enable=yes"));
        if (r.success()) {
            log.info("Firewall rule created via netsh (port={} subnet={})", port, vpnSubnet);
        } else {
            log.warn("netsh firewall rule also failed (exit={})", r.exitCode);
        }
    }
}
