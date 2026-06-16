package com.bachld.service.vnc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class VncBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(VncBootstrapService.class);

    private final VncProperties props;
    private final CommandRunner runner;
    private final UltraVncDetector detector;
    private final UltraVncInstaller installer;
    private final UltraVncConfigService configService;
    private final WindowsFirewallService firewallService;
    private final WireGuardNetworkService networkService;

    public VncBootstrapService() {
        this.props = VncProperties.load();
        this.runner = new CommandRunner();
        this.detector = new UltraVncDetector();
        this.installer = new UltraVncInstaller(props, runner, detector);
        this.configService = new UltraVncConfigService(props);
        this.firewallService = new WindowsFirewallService(runner);
        this.networkService = new WireGuardNetworkService(runner);
    }

    public void ensureReady() {
        log.info("VNC bootstrap started (UltraVNC 1.8.x no-auth mode)");

        Optional<String> existingService = detector.findUltraVncServiceName();
        existingService.ifPresent(svc -> runner.run(List.of(
                "sc", "failure", svc, "reset=", "0", "actions=", "")));
        existingService.ifPresent(this::stopServiceQuick);

        killAllWinvnc();
        waitForPortClosed(props.getPort(), 5);

        Path winVncExe = installer.ensureInstalled();

        configService.overwriteConfig(winVncExe);

        log.info("Ensuring firewall rule for VPN subnet...");
        firewallService.ensureVncFirewallRule(props.getPort(), props.getVpnSubnet());

        networkService.ensureVpnRoute(props.getVpnSubnet());

        launchWinvnc(winVncExe);
        if (waitForPortListening(props.getPort(), 15)) {
            log.info("UltraVNC ready - port {} listening (AuthRequired=0)", props.getPort());
        } else {
            log.error("UltraVNC failed to listen on port {} after launch", props.getPort());
        }
    }

    public void stop() {
        log.info("Stopping UltraVNC runtime");
        detector.findUltraVncServiceName().ifPresent(this::stopServiceQuick);
        killAllWinvnc();
        waitForPortClosed(props.getPort(), 5);
    }

    private void launchWinvnc(Path winVncExe) {
        try {
            ProcessBuilder pb = new ProcessBuilder(winVncExe.toString(), "-run");
            pb.directory(winVncExe.getParent().toFile());
            pb.redirectInput(ProcessBuilder.Redirect.from(new File("NUL")));
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.start();
            log.info("winvnc.exe -run launched");
        } catch (IOException e) {
            log.error("Failed to launch winvnc.exe -run: {}", e.getMessage(), e);
        }
    }

    private void stopServiceQuick(String serviceName) {
        log.info("Stopping UltraVNC service '{}'...", serviceName);
        runner.run(List.of("sc", "stop", serviceName));
        if (waitForState(serviceName, "STOPPED", 3)) {
            log.info("Service '{}' stopped cleanly", serviceName);
        } else {
            log.info("Service '{}' did not stop in 3s - will force-kill winvnc.exe", serviceName);
        }
    }

    private void killAllWinvnc() {
        runner.run(List.of("taskkill", "/F", "/T", "/IM", "winvnc.exe"));
    }

    private boolean isPortListening(int port) {
        try (Socket s = new Socket("127.0.0.1", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean waitForPortListening(int port, int timeoutSeconds) {
        for (int i = 0; i < timeoutSeconds * 2; i++) {
            if (isPortListening(port)) return true;
            sleep(500);
        }
        return false;
    }

    private void waitForPortClosed(int port, int timeoutSeconds) {
        for (int i = 0; i < timeoutSeconds * 2; i++) {
            if (!isPortListening(port)) return;
            sleep(500);
        }
        log.warn("Port {} still listening after {}s - proceeding anyway", port, timeoutSeconds);
    }

    private boolean waitForState(String serviceName, String targetState, int timeoutSec) {
        for (int i = 0; i < timeoutSec * 2; i++) {
            CommandResult q = runner.run(List.of("sc", "query", serviceName));
            if (q.stdout.contains(targetState)) return true;
            sleep(500);
        }
        return false;
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
