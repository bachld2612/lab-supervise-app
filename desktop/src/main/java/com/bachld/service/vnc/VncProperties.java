package com.bachld.service.vnc;

import com.bachld.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class VncProperties {

    private static final Logger log = LoggerFactory.getLogger(VncProperties.class);

    private final int port;
    private final String vpnSubnet;
    private final Path embeddedDir;
    private final Path installDir;

    private VncProperties(int port, String vpnSubnet, Path embeddedDir, Path installDir) {
        this.port = port;
        this.vpnSubnet = vpnSubnet;
        this.embeddedDir = embeddedDir;
        this.installDir = installDir;
    }

    public static VncProperties load() {
        AppConfig cfg = AppConfig.getInstance();
        int port = Integer.parseInt(cfg.get("vnc.port", "5900"));
        String vpnSubnet = cfg.get("vnc.vpn-subnet", "10.0.1.0/16");
        Path embeddedDir = Path.of(cfg.get("vnc.embedded-dir",
                "C:\\Program Files\\TLU Lab System\\uvnc"));
        Path installDir = Path.of(cfg.get("vnc.install-dir",
                "C:\\Program Files\\TLU Lab System\\uvnc"));
        log.info("VNC properties loaded - port={} vpnSubnet={}", port, vpnSubnet);
        return new VncProperties(port, vpnSubnet, embeddedDir, installDir);
    }

    public int getPort() { return port; }
    public String getVpnSubnet() { return vpnSubnet; }
    public Path getEmbeddedDir() { return embeddedDir; }
    public Path getInstallDir() { return installDir; }

    // UltraVNC 1.8.x reads ini from %PROGRAMDATA%/UltraVNC/ultravnc.ini.
    public Path getIniPath() {
        String pd = System.getenv("PROGRAMDATA");
        if (pd == null || pd.isBlank()) pd = "C:\\ProgramData";
        return Path.of(pd, "UltraVNC", "ultravnc.ini");
    }
}
