package com.bachld.service.vnc;

public final class CommandResult {
    public final int exitCode;
    public final String stdout;
    public final String stderr;

    public CommandResult(int exitCode, String stdout, String stderr) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public boolean success() {
        return exitCode == 0;
    }
}
