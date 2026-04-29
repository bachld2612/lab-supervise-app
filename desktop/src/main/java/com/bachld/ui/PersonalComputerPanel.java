package com.bachld.ui;

import com.bachld.model.response.PersonalComputerResponse;
import com.bachld.service.PersonalComputerService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Panel for managing personal computer IP address.
 * Contains a form with IP address field and update button.
 * Loads existing data on init via GET /me, and saves via POST /update.
 */
public class PersonalComputerPanel extends JPanel {

    private final PersonalComputerService pcService;
    private JTextField ipAddressField;
    private JButton saveButton;
    private JLabel errorLabel;
    private JLabel successLabel;
    private JLabel statusLabel;

    // Colors
    private static final Color BG_COLOR = new Color(244, 247, 254);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color PRIMARY_COLOR = new Color(85, 120, 235);
    private static final Color PRIMARY_HOVER = new Color(70, 100, 210);
    private static final Color TEXT_COLOR = new Color(30, 41, 59);
    private static final Color LABEL_COLOR = new Color(71, 85, 105);
    private static final Color BORDER_COLOR = new Color(203, 213, 225);
    private static final Color FOCUS_BORDER_COLOR = PRIMARY_COLOR;
    private static final Color ERROR_COLOR = new Color(220, 38, 38);
    private static final Color SUCCESS_COLOR = new Color(22, 163, 74);
    private static final Color BREADCRUMB_COLOR = new Color(148, 163, 184);

    public PersonalComputerPanel(PersonalComputerService pcService) {
        this.pcService = pcService;
        initUI();

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                loadData();
            }
        });
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // Title Section
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel breadcrumb = new JLabel("Home  >  Quản lý máy tính cá nhân");
        breadcrumb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        breadcrumb.setForeground(BREADCRUMB_COLOR);
        titlePanel.add(breadcrumb);
        titlePanel.add(Box.createVerticalStrut(10));

        JLabel lblTitle = new JLabel("Quản lý máy tính cá nhân");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(TEXT_COLOR);
        titlePanel.add(lblTitle);

        add(titlePanel, BorderLayout.NORTH);

        // Body with Card
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(30, 0, 0, 0));

        JPanel card = createFormCard();
        body.add(card, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    private JPanel createFormCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(30, 30, 30, 30)
        ));

        // Card Title
        JLabel cardTitle = new JLabel("Thông tin máy tính");
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        cardTitle.setForeground(TEXT_COLOR);
        cardTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(cardTitle);

        JLabel cardDesc = new JLabel("Cập nhật địa chỉ IP của máy tính cá nhân");
        cardDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cardDesc.setForeground(LABEL_COLOR);
        cardDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(Box.createVerticalStrut(4));
        card.add(cardDesc);

        // Divider
        card.add(Box.createVerticalStrut(20));
        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setForeground(new Color(226, 232, 240));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(separator);
        card.add(Box.createVerticalStrut(24));

        // Form wrapper with max width
        JPanel formWrapper = new JPanel();
        formWrapper.setLayout(new BoxLayout(formWrapper, BoxLayout.Y_AXIS));
        formWrapper.setOpaque(false);
        formWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        formWrapper.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));

        // IP Address Label
        JLabel ipLabel = new JLabel("Địa chỉ IP");
        ipLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        ipLabel.setForeground(LABEL_COLOR);
        ipLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formWrapper.add(ipLabel);
        formWrapper.add(Box.createVerticalStrut(8));

        // IP Address Field
        ipAddressField = new JTextField();
        ipAddressField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        ipAddressField.setForeground(TEXT_COLOR);
        ipAddressField.setBackground(Color.WHITE);
        ipAddressField.setMaximumSize(new Dimension(500, 44));
        ipAddressField.setPreferredSize(new Dimension(500, 44));
        ipAddressField.setAlignmentX(Component.LEFT_ALIGNMENT);
        ipAddressField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        ipAddressField.setToolTipText("Ví dụ: 192.168.100.197");

        // Focus effect
        ipAddressField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                ipAddressField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(FOCUS_BORDER_COLOR, 2),
                        new EmptyBorder(7, 11, 7, 11)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                ipAddressField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COLOR, 1),
                        new EmptyBorder(8, 12, 8, 12)
                ));
            }
        });

        formWrapper.add(ipAddressField);
        formWrapper.add(Box.createVerticalStrut(6));

        // Error Label
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        errorLabel.setForeground(ERROR_COLOR);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setVisible(false);
        formWrapper.add(errorLabel);

        // Success Label
        successLabel = new JLabel(" ");
        successLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        successLabel.setForeground(SUCCESS_COLOR);
        successLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        successLabel.setVisible(false);
        formWrapper.add(successLabel);

        formWrapper.add(Box.createVerticalStrut(20));

        // Save Button
        saveButton = new JButton("Lưu thay đổi") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(PRIMARY_HOVER.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(PRIMARY_HOVER);
                } else {
                    g2.setColor(PRIMARY_COLOR);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        saveButton.setForeground(Color.WHITE);
        saveButton.setPreferredSize(new Dimension(160, 42));
        saveButton.setMaximumSize(new Dimension(160, 42));
        saveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveButton.setContentAreaFilled(false);
        saveButton.setBorderPainted(false);
        saveButton.setFocusPainted(false);
        saveButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        saveButton.addActionListener(e -> handleSave());

        formWrapper.add(saveButton);

        // Loading status label (hidden by default)
        formWrapper.add(Box.createVerticalStrut(12));
        statusLabel = new JLabel("Đang tải...");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        statusLabel.setForeground(LABEL_COLOR);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setVisible(false);
        formWrapper.add(statusLabel);

        card.add(formWrapper);

        return card;
    }

    /**
     * Loads existing personal computer data from API on startup.
     */
    private void loadData() {
        clearMessages();
        setFormEnabled(false);
        statusLabel.setText("Đang tải dữ liệu...");
        statusLabel.setForeground(LABEL_COLOR);
        statusLabel.setVisible(true);

        pcService.fetchMyComputerAsync(new PersonalComputerService.FetchCallback() {
            @Override
            public void onSuccess(PersonalComputerResponse response) {
                statusLabel.setVisible(false);
                setFormEnabled(true);
                if (response.getData() != null && response.getData().getIpAddress() != null) {
                    ipAddressField.setText(response.getData().getIpAddress());
                } else {
                    ipAddressField.setText("");
                }
            }

            @Override
            public void onError(String errorMessage) {
                statusLabel.setText("⚠ " + errorMessage);
                statusLabel.setForeground(ERROR_COLOR);
                statusLabel.setVisible(true);
                setFormEnabled(true);
            }
        });
    }

    /**
     * Handles save button click - validates and sends update request.
     */
    private void handleSave() {
        clearMessages();
        String ipAddress = ipAddressField.getText().trim();

        // Client-side validation
        if (ipAddress.isEmpty()) {
            showError("Địa chỉ IP không được phép bỏ trống");
            return;
        }

        setFormEnabled(false);
        statusLabel.setText("Đang lưu...");
        statusLabel.setForeground(LABEL_COLOR);
        statusLabel.setVisible(true);

        pcService.updateComputerAsync(ipAddress, new PersonalComputerService.UpdateCallback() {
            @Override
            public void onSuccess() {
                statusLabel.setVisible(false);
                setFormEnabled(true);
                showSuccess("Cập nhật địa chỉ IP thành công!");
            }

            @Override
            public void onError(String errorMessage) {
                statusLabel.setVisible(false);
                setFormEnabled(true);
                showError(errorMessage);
            }
        });
    }

    private void showError(String message) {
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }

    private void showSuccess(String message) {
        successLabel.setText("✓ " + message);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }

    private void clearMessages() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
    }

    private void setFormEnabled(boolean enabled) {
        ipAddressField.setEnabled(enabled);
        saveButton.setEnabled(enabled);
    }
}
