package com.bachld.service;

import com.bachld.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.Matcher;

public class VncService {

    private static final Logger log = LoggerFactory.getLogger(VncService.class);

    private static final String[] BUNDLED_FILES = {
        "winvnc.exe", "vnchooks.dll", "ddengine64.dll", "logmessages.dll", "ultravnc.ini"
    };

    private Path vncDir;
    private Process vncProcess;

    private String getVncPassword() {
        return AppConfig.getInstance().resolve("VNC_PASSWORD", "vnc.password", "");
    }

    public void start() {
        String password = getVncPassword();
        if (password.isEmpty()) {
            log.warn("VNC_PASSWORD not configured, VNC will not start");
            return;
        }

        stopSystemService();
        killAllVnc();
        try { Thread.sleep(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

        byte[] encBytes = encryptVncPassword(password);

        configureRegistry(encBytes);
        patchAllSystemIni(encBytes);

        try {
            vncDir = extractBundled();

            ProcessBuilder pb = new ProcessBuilder(
                vncDir.resolve("winvnc.exe").toString(), "-run"
            );
            pb.directory(vncDir.toFile());
            vncProcess = pb.start();

            // Poll port 5900 instead of fixed sleep — exits as soon as UltraVNC is accepting connections
            boolean ready = false;
            for (int i = 0; i < 20; i++) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); break; }
                if (!vncProcess.isAlive()) break;
                try (Socket probe = new Socket("127.0.0.1", 5900)) {
                    ready = true;
                    break;
                } catch (IOException ignored) {}
            }

            if (!vncProcess.isAlive()) {
                log.warn("UltraVNC exited immediately (exit={}), VNC unavailable", vncProcess.exitValue());
            } else {
                log.info("UltraVNC {} on port 5900 (dir={})", ready ? "ready" : "starting", vncDir);
            }
        } catch (Exception e) {
            log.warn("Could not start UltraVNC: {}", e.getMessage(), e);
        }
    }

    private void stopSystemService() {
        runQuietly("sc", "stop", "uvnc_service");
        runQuietly("net", "stop", "uvnc_service");
    }

    private void killAllVnc() {
        runQuietly("taskkill", "/F", "/IM", "winvnc.exe");
    }

    private void configureRegistry(byte[] encBytes) {
        String pwdHex = toRegHex(encBytes);
        String emptyHex = toRegHex(new byte[8]);

        StringBuilder reg = new StringBuilder("Windows Registry Editor Version 5.00\r\n");
        String[] keys = {
            "HKEY_LOCAL_MACHINE\\SOFTWARE\\ORL\\WinVNC3\\Default",
            "HKEY_LOCAL_MACHINE\\SOFTWARE\\ORL\\WinVNC3",
            "HKEY_CURRENT_USER\\SOFTWARE\\ORL\\WinVNC3\\Default",
            "HKEY_CURRENT_USER\\SOFTWARE\\ORL\\WinVNC3",
            "HKEY_LOCAL_MACHINE\\SOFTWARE\\UltraVNC",
            "HKEY_CURRENT_USER\\SOFTWARE\\UltraVNC",
        };
        for (String key : keys) {
            reg.append("\r\n[").append(key).append("]\r\n");
            reg.append("\"Password\"=hex:").append(pwdHex).append("\r\n");
            reg.append("\"Password2\"=hex:").append(emptyHex).append("\r\n");
            reg.append("\"PasswordViewOnly\"=hex:").append(emptyHex).append("\r\n");
            reg.append("\"AuthRequired\"=dword:00000001\r\n");
            reg.append("\"RemoveWallpaper\"=dword:00000000\r\n");
            reg.append("\"DisableAero\"=dword:00000000\r\n");
            reg.append("\"AllowLoopback\"=dword:00000001\r\n");
            reg.append("\"LoopbackOnly\"=dword:00000000\r\n");
        }

        try {
            Path regFile = Files.createTempFile("lab-vnc-", ".reg");
            // UTF-16 LE with BOM — required by regedit on all Windows versions
            byte[] bom = {(byte) 0xFF, (byte) 0xFE};
            byte[] content = reg.toString().getBytes(StandardCharsets.UTF_16LE);
            byte[] full = new byte[bom.length + content.length];
            System.arraycopy(bom, 0, full, 0, bom.length);
            System.arraycopy(content, 0, full, bom.length, content.length);
            Files.write(regFile, full);
            runQuietly("regedit", "/s", regFile.toAbsolutePath().toString());
            Files.deleteIfExists(regFile);
        } catch (Exception e) {
            log.warn("Failed to configure registry via .reg file: {}", e.getMessage());
        }
    }

    private String toRegHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format("%02x", bytes[i] & 0xFF));
        }
        return sb.toString();
    }

    private void patchAllSystemIni(byte[] encBytes) {
        String[] bases = {
            System.getenv("PROGRAMDATA"),
            System.getenv("ProgramFiles"),
            System.getenv("ProgramFiles(x86)"),
        };
        String[] subDirs = {"UltraVNC", "uvnc bvba\\UltraVNC"};
        for (String base : bases) {
            if (base == null) continue;
            for (String sub : subDirs) {
                patchIniFile(Path.of(base, sub, "ultravnc.ini"), encBytes);
            }
        }
    }

    private void patchIniFile(Path ini, byte[] encBytes) {
        if (!Files.exists(ini)) return;
        try {
            String content = Files.readString(ini, StandardCharsets.ISO_8859_1);
            String rawPwd = Matcher.quoteReplacement(new String(encBytes, StandardCharsets.ISO_8859_1));
            content = content.replaceAll("(?m)^passwd=.*$", "passwd=" + rawPwd);
            content = content.replaceAll("(?m)^passwd2=.*$", "passwd2=");
            content = content.replaceAll("(?m)^AuthRequired=.*$", "AuthRequired=1");
            Files.writeString(ini, content, StandardCharsets.ISO_8859_1);
            log.info("Patched ini: {}", ini);
        } catch (Exception e) {
            log.warn("Could not patch {}: {}", ini, e.getMessage());
        }
    }

    private byte[] encryptVncPassword(String password) {
        try {
            byte[] fixedKey = {0x17, 0x52, 0x6b, 0x17, (byte)0xD2, 0x1A, 0x1F, 0x62};
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

    private void runQuietly(String... cmd) {
        try {
            new ProcessBuilder(cmd).redirectErrorStream(true).start().waitFor();
        } catch (Exception ignored) {}
    }

    public void stop() {
        // RemoveWallpaper=0 in registry means UltraVNC never touches the wallpaper,
        // so graceful -kill is not needed — force-kill immediately
        if (vncProcess != null) {
            vncProcess.destroyForcibly();
            vncProcess = null;
        }
        killAllVnc();
        log.info("UltraVNC stopped");
    }

    private Path extractBundled() throws IOException {
        Path dir = Path.of(System.getProperty("java.io.tmpdir"), "lab-monitor-vnc");
        Files.createDirectories(dir);
        for (String name : BUNDLED_FILES) {
            Path target = dir.resolve(name);
            boolean alwaysRefresh = name.endsWith(".ini");
            if (!alwaysRefresh && Files.exists(target)) continue;
            try (InputStream in = getClass().getResourceAsStream("/vnc/" + name)) {
                if (in == null) throw new IOException("Bundled resource not found: vnc/" + name);
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        // No ini patching needed — UseRegistry=1 means UltraVNC reads all settings from the registry
        return dir;
    }
}
