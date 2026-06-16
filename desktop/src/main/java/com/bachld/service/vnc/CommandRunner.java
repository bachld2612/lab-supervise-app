package com.bachld.service.vnc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class CommandRunner {

    private static final Logger log = LoggerFactory.getLogger(CommandRunner.class);
    private static final int TIMEOUT_SECONDS = 30;

    public CommandResult run(List<String> command, boolean sensitive) {
        String logCmd = sensitive ? command.get(0) + " [args hidden]" : String.join(" ", command);
        log.debug("CMD: {}", logCmd);

        try {
            Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(p.getInputStream().readAllBytes(), Charset.defaultCharset());
            boolean finished = p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                p.destroyForcibly();
                log.warn("CMD timed out: {}", logCmd);
                return new CommandResult(-1, "", "timeout");
            }

            int exit = p.exitValue();
            if (exit != 0) {
                log.warn("CMD exit={}: {} — {}", exit, logCmd, output.trim());
            }
            return new CommandResult(exit, output, "");

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("CMD error: {} — {}", logCmd, e.getMessage());
            return new CommandResult(-1, "", e.getMessage());
        }
    }

    public CommandResult run(List<String> command) {
        return run(command, false);
    }
}
