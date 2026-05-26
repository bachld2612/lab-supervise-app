package com.bachld.service.vnc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class UltraVncConfigService {

    private static final Logger log = LoggerFactory.getLogger(UltraVncConfigService.class);

    private final VncProperties props;

    public UltraVncConfigService(VncProperties props) {
        this.props = props;
    }

    public void overwriteConfig(Path winVncExe) {
        Path iniPath = props.getIniPath();

        try {
            Files.createDirectories(iniPath.getParent());
        } catch (IOException e) {
            log.warn("Could not create ini parent dir {}: {}", iniPath.getParent(), e.getMessage());
        }

        if (Files.exists(iniPath)) {
            try {
                Files.copy(iniPath, iniPath.resolveSibling("ultravnc.ini.bak"),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.warn("Could not backup ultravnc.ini: {}", e.getMessage());
            }
        }

        try {
            Files.writeString(iniPath, buildIniContent(winVncExe.getParent().toString()),
                    StandardCharsets.UTF_8);
            log.info("Overwrote UltraVNC configuration at {} (the path winvnc 1.8.x actually reads)", iniPath);
        } catch (IOException e) {
            log.error("Failed to write ultravnc.ini: {}", e.getMessage(), e);
        }
    }

    private String buildIniContent(String installPath) {
        return String.join("\r\n",
                "[ultravnc]",
                "UseRegistry=0",
                "AuthRequired=0",
                "MSLogonRequired=0",
                "NewMSLogon=0",
                "passwd=",
                "passwd2=",
                "FileTransferEnabled=0",
                "AllowLoopback=1",
                "LoopbackOnly=0",
                "RemoveWallpaper=0",
                "DisableAero=0",
                "ConnectPriority=2",
                "",
                "[admin]",
                "AllowUserSettingsWithPassword=0",
                "FileTransferEnabled=0",
                "FTUserImpersonation=0",
                "BlankMonitorEnabled=0",
                "BlankInputsOnly=0",
                "DefaultScale=1",
                "UseDSMPlugin=0",
                "DSMPlugin=",
                "AuthHosts=",
                "primary=1",
                "secondary=0",
                "SocketConnect=1",
                "HTTPConnect=0",
                "AutoPortSelect=0",
                "PortNumber=5900",
                "HTTPPortNumber=5800",
                "InputsEnabled=1",
                "LocalInputsDisabled=0",
                "IdleTimeout=0",
                "QuerySetting=0",
                "QueryTimeout=10",
                "QueryAccept=0",
                "MaxViewerSetting=0",
                "MaxViewers=128",
                "QueryIfNoLogon=0",
                "LockSetting=0",
                "RemoveWallpaper=0",
                "RemoveEffects=0",
                "RemoveFontSmoothing=0",
                "DebugMode=2",
                "Avilog=0",
                "path=" + installPath,
                "DebugLevel=9",
                "AllowLoopback=1",
                "UseIpv6=0",
                "LoopbackOnly=0",
                "AllowShutdown=1",
                "AllowProperties=1",
                "UseBridge=1",
                "AllowInjection=0",
                "AllowEditClients=1",
                "FileTransferTimeout=30",
                "KeepAliveInterval=5",
                "IdleInputTimeout=0",
                "DisableTrayIcon=1",
                "rdpmode=0",
                "noscreensaver=0",
                "Secure=0",
                "MSLogonRequired=0",
                "NewMSLogon=0",
                "ReverseAuthRequired=0",
                "ConnectPriority=0",
                "AuthRequired=0",
                "service_commandline=",
                "accept_reject_mesg=",
                "cloudServer=",
                "cloudEnabled=0",
                "Language=en",
                "",
                "[poll]",
                "TurboMode=1",
                "PollUnderCursor=0",
                "PollForeground=0",
                "PollFullScreen=1",
                "OnlyPollConsole=0",
                "OnlyPollOnEvent=0",
                "MaxCpu2=100",
                "MaxFPS=25",
                "EnableDriver=0",
                "EnableHook=1",
                "EnableVirtual=0",
                "autocapt=1",
                "",
                "[admin_auth]",
                "group1=",
                "group2=",
                "group3=",
                "locdom1=0",
                "locdom2=0",
                "locdom3=0",
                ""
        );
    }
}
