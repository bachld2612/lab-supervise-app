package com.bachld.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VncService {

    private static final Logger log = LoggerFactory.getLogger(VncService.class);

    private static final String[] BUNDLED_FILES = {
            "winvnc.exe", "vnchooks.dll", "ddengine64.dll", "logmessages.dll"
    };
    private static final String CHARSET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int VNC_PORT = 5900;

    private Path vncDir;
    private Process vncProcess;
    private String currentPassword;

    /**
     * Always starts the bundled UltraVNC with an app-generated password.
     * Any existing VNC instances (system-installed or previous runs) are stopped first
     * so the app fully controls port 5900 and the password.
     */
    public synchronized String start() {
        log.info("VncService.start — os={} admin={}", System.getProperty("os.name", ""), isRunningAsAdmin());

        ensureFirewallRule();

        // Stop everything that could be occupying port 5900.
        stopAndDisableVncServices();
        stopBundledServer();
        killAllWinvncProcesses();
        killProcessListeningOnVncPort();

        if (!waitUntilPortFree()) {
            log.warn("Port 5900 still occupied after cleanup — cannot start bundled UltraVNC");
            return null;
        }

        return startBundledVnc();
    }

    public synchronized void stop() {
        if (vncProcess != null) {
            vncProcess.destroyForcibly();
            vncProcess = null;
        }
        stopBundledServer();
        killAllWinvncProcesses();
        currentPassword = null;
        log.info("UltraVNC stopped");
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    // ── Service management ────────────────────────────────────────────────────

    private void stopAndDisableVncServices() {
        for (String name : detectVncServiceNames()) {
            runLogged("sc", "stop", name);
            // Disable auto-restart so the service does not respawn winvnc.exe after we kill it.
            runLogged("sc", "config", name, "start=", "disabled");
            log.info("Stopped and disabled system VNC service '{}'", name);
        }
    }

    private List<String> detectVncServiceNames() {
        List<String> found = new ArrayList<>();
        try {
            Process p = new ProcessBuilder("sc", "query", "type=", "all", "state=", "all")
                    .redirectErrorStream(true).start();
            String output = new String(p.getInputStream().readAllBytes(), Charset.defaultCharset());
            p.waitFor(10, TimeUnit.SECONDS);

            String pendingName = null;
            for (String line : output.split("\\R")) {
                String t = line.trim();
                if (t.startsWith("SERVICE_NAME:")) {
                    pendingName = t.substring("SERVICE_NAME:".length()).trim();
                } else if (t.startsWith("DISPLAY_NAME:") && pendingName != null) {
                    String displayName = t.substring("DISPLAY_NAME:".length()).trim().toLowerCase();
                    String svcName = pendingName.toLowerCase();
                    if (svcName.contains("uvnc") || svcName.contains("ultravnc") || svcName.contains("winvnc")
                            || displayName.contains("ultravnc") || displayName.contains("winvnc")
                            || displayName.contains("uvnc")) {
                        found.add(pendingName);
                        log.info("Detected VNC service: {} (display: {})", pendingName, displayName);
                    }
                    pendingName = null;
                }
            }
        } catch (Exception e) {
            log.warn("Could not scan service list: {}", e.getMessage());
        }
        return found;
    }

    // ── Bundled UltraVNC startup ──────────────────────────────────────────────

    private String startBundledVnc() {
        if (getClass().getResource("/vnc/winvnc.exe") == null) {
            log.error("Bundled winvnc.exe not found in JAR — add UltraVNC binaries to src/main/resources/vnc/");
            return null;
        }

        String password = loadOrGeneratePassword();
        try {
            byte[] encrypted = encryptVncPassword(password);
            vncDir = extractBundled(encrypted);

            ProcessBuilder pb = new ProcessBuilder(vncDir.resolve("winvnc.exe").toString(), "-run");
            pb.directory(vncDir.toFile());
            pb.inheritIO();
            vncProcess = pb.start();

            // Wait up to 10 seconds for port 5900 to become available.
            boolean ready = false;
            for (int i = 0; i < 100; i++) {
                try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                if (!vncProcess.isAlive()) break;
                try (Socket probe = new Socket("127.0.0.1", VNC_PORT)) { ready = true; break; }
                catch (IOException ignored) {}
            }

            if (!vncProcess.isAlive()) {
                log.warn("Bundled winvnc exited immediately (exit={})", vncProcess.exitValue());
                return null;
            }

            if (!ready) {
                log.warn("Bundled winvnc alive but port 5900 not listening after 10s — aborting");
                vncProcess.destroyForcibly();
                vncProcess = null;
                return null;
            }

            long ourPid = vncProcess.pid();
            long portPid = getPidListeningOnVncPort();
            if (portPid == ourPid) {
                log.info("Bundled UltraVNC ready on port 5900 — pid={} dir={}", ourPid, vncDir);
            } else {
                log.warn("Port 5900 served by pid={} (not our pid={})", portPid, ourPid);
            }

            currentPassword = password;
            return password;

        } catch (Exception e) {
            log.error("Could not start bundled UltraVNC: {}", e.getMessage(), e);
            return null;
        }
    }

    // ── Firewall ──────────────────────────────────────────────────────────────

    private void ensureFirewallRule() {
        runLogged("netsh", "advfirewall", "firewall", "delete", "rule", "name=TLULabVNC");
        runLogged("netsh", "advfirewall", "firewall", "add", "rule",
                "name=TLULabVNC", "protocol=TCP", "dir=in",
                "localport=5900", "action=allow", "enable=yes");
        log.info("Firewall rule for port 5900 ensured");
    }

    // ── Password generation & ini/registry writing ───────────────────────────

    /**
     * Returns the persisted VNC password for this machine, generating and saving
     * a new one only on the very first run. Reusing the same password across
     * restarts prevents the race where the teacher holds the old DB password
     * while winvnc is already running with a freshly generated one.
     */
    private String loadOrGeneratePassword() {
        Path passwdFile = getPasswdFilePath();
        if (passwdFile != null && Files.exists(passwdFile)) {
            try {
                String saved = Files.readString(passwdFile, StandardCharsets.UTF_8).trim();
                if (!saved.isBlank()) {
                    log.info("Loaded existing VNC password from {}", passwdFile);
                    return saved;
                }
            } catch (Exception e) {
                log.warn("Could not read saved VNC password ({}), generating new one", e.getMessage());
            }
        }
        String newPassword = generatePassword();
        if (passwdFile != null) {
            try {
                Files.createDirectories(passwdFile.getParent());
                Files.writeString(passwdFile, newPassword, StandardCharsets.UTF_8);
                log.info("Saved new VNC password to {}", passwdFile);
            } catch (Exception e) {
                log.warn("Could not persist VNC password to file: {}", e.getMessage());
            }
        }
        return newPassword;
    }

    private Path getPasswdFilePath() {
        String base = System.getenv("LOCALAPPDATA");
        if (base == null || base.isBlank()) base = System.getProperty("java.io.tmpdir");
        if (base == null) return null;
        return Path.of(base, "TLULabSystem", "vnc.passwd");
    }

    private String generatePassword() {
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(CHARSET.charAt(rng.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }

    private byte[] encryptVncPassword(String password) {
        try {
            byte[] fixedKey = {23, 82, 107, 6, 35, 78, 88, 7};
            for (int i = 0; i < 8; i++) fixedKey[i] = reverseBits(fixedKey[i]);

            byte[] pwd = new byte[8];
            byte[] raw = password.getBytes(StandardCharsets.ISO_8859_1);
            System.arraycopy(raw, 0, pwd, 0, Math.min(8, raw.length));

            Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(fixedKey, "DES"));
            return cipher.doFinal(pwd);
        } catch (Exception e) {
            log.error("Failed to encrypt VNC password", e);
            return new byte[8];
        }
    }

    private byte reverseBits(byte b) {
        int v = b & 0xFF, r = 0;
        for (int i = 0; i < 8; i++) { r = (r << 1) | (v & 1); v >>= 1; }
        return (byte) r;
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b & 0xFF));
        return sb.toString();
    }

    private Path extractBundled(byte[] encryptedPassword) throws IOException {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.isBlank()) {
            localAppData = System.getProperty("java.io.tmpdir");
        }
        Path base = Path.of(localAppData, "TLULabSystem");

        // Clean up old extraction dirs — DLLs from previous runs may still be locked
        // (vnchooks.dll stays injected in other processes until reboot), so we can't
        // overwrite them. Each start gets a fresh dir; stale dirs are removed here
        // on a best-effort basis once the locks are gone.
        cleanupOldVncDirs(base);

        // Fresh timestamped dir — no existing files to conflict with.
        Path dir = base.resolve("uvnc-" + System.currentTimeMillis());
        Files.createDirectories(dir);

        for (String name : BUNDLED_FILES) {
            try (InputStream in = getClass().getResourceAsStream("/vnc/" + name)) {
                if (in == null) throw new IOException("Bundled resource not found: vnc/" + name);
                Files.copy(in, dir.resolve(name));
            }
        }

        writeIni(dir.resolve("ultravnc.ini"), encryptedPassword);
        return dir;
    }

    private void cleanupOldVncDirs(Path base) {
        try {
            if (!Files.exists(base)) return;
            Files.list(base)
                    .filter(p -> p.getFileName().toString().startsWith("uvnc-"))
                    .forEach(p -> {
                        try {
                            Files.walk(p)
                                    .sorted(java.util.Comparator.reverseOrder())
                                    .forEach(f -> { try { Files.deleteIfExists(f); } catch (IOException ignored) {} });
                        } catch (IOException ignored) {}
                    });
        } catch (Exception ignored) {}
    }

    private void writeIni(Path iniPath, byte[] encryptedPassword) throws IOException {
        String passwordHex = toHex(encryptedPassword) + "00";
        String content = String.join("\r\n",
                "[ultravnc]",
                "UseRegistry=0",
                "AuthRequired=1",
                "MSLogonRequired=0",
                "NewMSLogon=0",
                "passwd=" + passwordHex,
                "passwd2=",
                "FileTransferEnabled=0",
                "AllowLoopback=1",
                "LoopbackOnly=0",
                "RemoveWallpaper=0",
                "DisableAero=0",
                "ConnectPriority=2",
                ""
        );
        Files.writeString(iniPath, content, StandardCharsets.UTF_8);
    }

    // ── Process / port management ─────────────────────────────────────────────

    private void killAllWinvncProcesses() {
        runLogged("taskkill", "/F", "/IM", "winvnc.exe");
    }

    private void stopBundledServer() {
        if (vncDir != null) {
            runQuietly(vncDir.resolve("winvnc.exe").toString(), "-kill");
        }
    }

    private boolean isPortListening() {
        try (Socket probe = new Socket("127.0.0.1", VNC_PORT)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean waitUntilPortFree() {
        for (int i = 0; i < 40; i++) {
            if (!isPortListening()) return true;
            try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        }
        return false;
    }

    private void killProcessListeningOnVncPort() {
        try {
            Process process = new ProcessBuilder("netstat", "-ano", "-p", "tcp")
                    .redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), Charset.defaultCharset());
            process.waitFor();

            for (String line : output.split("\\R")) {
                String[] columns = line.trim().split("\\s+");
                if (columns.length < 5) continue;
                if (!"TCP".equalsIgnoreCase(columns[0]) || !"LISTENING".equalsIgnoreCase(columns[3])) continue;
                if (!isVncAddress(columns[1])) continue;
                String pid = columns[4];
                if ("0".equals(pid)) continue;
                log.warn("Killing process {} listening on VNC port", pid);
                runQuietly("taskkill", "/F", "/T", "/PID", pid);
            }
        } catch (Exception e) {
            log.warn("Could not inspect process on VNC port: {}", e.getMessage());
        }
    }

    private long getPidListeningOnVncPort() {
        try {
            Process process = new ProcessBuilder("netstat", "-ano", "-p", "tcp")
                    .redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), Charset.defaultCharset());
            process.waitFor();
            for (String line : output.split("\\R")) {
                String[] cols = line.trim().split("\\s+");
                if (cols.length < 5) continue;
                if (!"TCP".equalsIgnoreCase(cols[0])) continue;
                if (!"LISTENING".equalsIgnoreCase(cols[3])) continue;
                if (!isVncAddress(cols[1])) continue;
                try { return Long.parseLong(cols[4]); } catch (NumberFormatException ignored) {}
            }
        } catch (Exception e) {
            log.debug("Could not determine PID on port {}: {}", VNC_PORT, e.getMessage());
        }
        return -1;
    }

    private boolean isVncAddress(String address) {
        return address != null && address.endsWith(":" + VNC_PORT);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isRunningAsAdmin() {
        try {
            Process p = new ProcessBuilder("net", "session")
                    .redirectErrorStream(true).start();
            p.waitFor(3, TimeUnit.SECONDS);
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void runQuietly(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            if (!p.waitFor(10, TimeUnit.SECONDS)) p.destroyForcibly();
        } catch (Exception ignored) {}
    }

    private void runLogged(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            boolean finished = p.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                log.warn("CMD timed out: {}", String.join(" ", cmd));
                return;
            }
            int exit = p.exitValue();
            if (exit != 0) {
                String out = new String(p.getInputStream().readAllBytes(), Charset.defaultCharset()).trim();
                log.warn("CMD exit={}: {} — {}", exit, String.join(" ", cmd), out);
            }
        } catch (Exception e) {
            log.warn("CMD error: {} — {}", String.join(" ", cmd), e.getMessage());
        }
    }
}
