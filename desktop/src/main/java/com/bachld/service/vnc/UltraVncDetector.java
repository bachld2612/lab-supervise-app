package com.bachld.service.vnc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class UltraVncDetector {

    private static final Logger log = LoggerFactory.getLogger(UltraVncDetector.class);

    private static final List<Path> KNOWN_PATHS = List.of(
            Path.of("C:\\Program Files\\TLU Lab System\\uvnc\\winvnc.exe"),
            Path.of("C:\\Program Files\\uvnc bvba\\UltraVNC\\winvnc.exe"),
            Path.of("C:\\Program Files\\UltraVNC\\winvnc.exe"),
            Path.of("C:\\Program Files (x86)\\uvnc bvba\\UltraVNC\\winvnc.exe"),
            Path.of("C:\\Program Files (x86)\\UltraVNC\\winvnc.exe")
    );

    public Optional<Path> findWinVncExe() {
        for (Path p : KNOWN_PATHS) {
            if (Files.exists(p)) {
                log.info("UltraVNC found at: {}", p);
                return Optional.of(p);
            }
        }
        log.info("UltraVNC executable not found in known paths");
        return Optional.empty();
    }

    public Optional<String> findUltraVncServiceName() {
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
                    String dn = t.substring("DISPLAY_NAME:".length()).trim().toLowerCase();
                    String sn = pendingName.toLowerCase();
                    if (sn.contains("uvnc") || sn.contains("ultravnc") || sn.contains("winvnc")
                            || dn.contains("ultravnc") || dn.contains("winvnc") || dn.contains("uvnc")) {
                        log.info("UltraVNC service found: {}", pendingName);
                        return Optional.of(pendingName);
                    }
                    pendingName = null;
                }
            }
        } catch (Exception e) {
            log.warn("Could not scan Windows services: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
