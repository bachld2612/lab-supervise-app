package com.bachld.ui;

import com.bachld.config.AppConfig;
import com.bachld.service.AuthService;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private final AuthService authService;

    public LoginFrame(AuthService authService) {
        this.authService = authService;
        initFrame();
        setVisible(true);
    }

    private void initFrame() {
        String appName = AppConfig.getInstance().getAppName();
        String version  = AppConfig.getInstance().getAppVersion();
        setTitle(appName + " v" + version);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Pure white background — the form IS the window
        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(Color.WHITE);

        LoginPanel form = new LoginPanel(authService);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill   = GridBagConstraints.NONE;
        bg.add(form, gbc);

        setContentPane(bg);
        setSize(480, 560);
        setMinimumSize(new Dimension(480, 560));
        setLocationRelativeTo(null);
    }
}
