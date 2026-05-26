package com.bachld.service.vnc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class AdminPrivilegeService {

    private static final Logger log = LoggerFactory.getLogger(AdminPrivilegeService.class);

    public boolean isRunningAsAdmin() {
        log.info("Checking administrator permission...");
        try {
            Process p = new ProcessBuilder("net", "session")
                    .redirectErrorStream(true).start();
            p.waitFor(5, TimeUnit.SECONDS);
            boolean admin = p.exitValue() == 0;
            if (admin) {
                log.info("Application is running as Administrator");
            } else {
                log.warn("Application is NOT running as Administrator");
            }
            return admin;
        } catch (Exception e) {
            log.warn("Could not determine admin status: {}", e.getMessage());
            return false;
        }
    }

    public void relaunchAsAdminAndExit() {
        log.info("Relaunching application with Administrator privileges via UAC...");

        String jarPath = findJarPath();
        if (jarPath == null) {
            log.error("Cannot determine JAR path for UAC relaunch");
            showAdminRequiredError();
            System.exit(1);
            return;
        }

        String javaExe = ProcessHandle.current().info().command().orElse("javaw.exe");

        // Use Start-Process -Verb RunAs to trigger UAC prompt
        String psCommand = String.format(
                "Start-Process -FilePath '%s' -ArgumentList '-jar \"%s\"' -Verb RunAs",
                javaExe, jarPath.replace("\\", "\\\\"));

        CommandRunner runner = new CommandRunner();
        CommandResult result = runner.run(List.of("powershell.exe", "-Command", psCommand));

        if (!result.success()) {
            log.warn("UAC relaunch failed (exit={}) — user may have denied the prompt", result.exitCode);
            showAdminRequiredError();
        }

        System.exit(0);
    }

    private String findJarPath() {
        try {
            Path p = Path.of(AdminPrivilegeService.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (p.toString().endsWith(".jar")) return p.toString();
            return null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private void showAdminRequiredError() {
        JOptionPane.showMessageDialog(null,
                "Ứng dụng cần quyền Administrator để cấu hình UltraVNC và tường lửa.\n" +
                "Vui lòng chạy lại ứng dụng với quyền Administrator.",
                "Cần quyền Administrator",
                JOptionPane.ERROR_MESSAGE);
    }
}
