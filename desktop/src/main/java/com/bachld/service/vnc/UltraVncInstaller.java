package com.bachld.service.vnc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

public final class UltraVncInstaller {

    private static final Logger log = LoggerFactory.getLogger(UltraVncInstaller.class);

    private static final String[] BUNDLED_FILES = {
            "winvnc.exe", "vnchooks.dll", "ddengine64.dll", "logmessages.dll"
    };

    private final VncProperties props;
    private final CommandRunner runner;
    private final UltraVncDetector detector;

    public UltraVncInstaller(VncProperties props, CommandRunner runner, UltraVncDetector detector) {
        this.props = props;
        this.runner = runner;
        this.detector = detector;
    }

    public Path ensureInstalled() {
        Path installDir = props.getInstallDir();
        Path winVncExe = installDir.resolve("winvnc.exe");

        if (!Files.exists(winVncExe)) {
            log.info("winvnc.exe not present — extracting bundled UltraVNC files to {}", installDir);
            extractBundled(installDir);
        } else {
            log.info("UltraVNC already installed at: {}", winVncExe);
        }

        return winVncExe;
    }

    public void ensureServiceInstalled(Path winVncExe) {
        Optional<String> svcName = detector.findUltraVncServiceName();
        if (svcName.isPresent()) {
            log.info("UltraVNC service '{}' already exists", svcName.get());
            return;
        }

        log.info("UltraVNC service not found — installing via winvnc.exe -install");
        CommandResult result = runner.run(List.of(winVncExe.toString(), "-install"));
        if (result.success()) {
            log.info("UltraVNC service installed successfully");
        } else {
            log.warn("winvnc.exe -install exit={}: {}", result.exitCode, result.stdout.trim());
        }
    }

    private void extractBundled(Path installDir) {
        try {
            Files.createDirectories(installDir);
            int count = 0;
            for (String name : BUNDLED_FILES) {
                try (InputStream in = getClass().getResourceAsStream("/vnc/" + name)) {
                    if (in == null) {
                        log.warn("Bundled resource not found: /vnc/{}", name);
                        continue;
                    }
                    Files.copy(in, installDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
                    count++;
                }
            }
            log.info("Extracted {} UltraVNC files to {}", count, installDir);
        } catch (IOException e) {
            log.error("Failed to extract bundled UltraVNC: {}", e.getMessage(), e);
        }
    }
}
