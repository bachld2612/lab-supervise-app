package com.bachld.ui;

import com.bachld.model.response.PersonalComputerResponse;
import com.bachld.service.PersonalComputerService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PersonalComputerPanel extends JPanel {

    private final PersonalComputerService pcService;
    private JTextField ipAddressField;
    private JLabel statusLabel;

    private static final Color CARD_BG = Color.WHITE;
    private static final Color TEXT_COLOR = new Color(30, 41, 59);
    private static final Color LABEL_COLOR = new Color(71, 85, 105);
    private static final Color BORDER_COLOR = new Color(203, 213, 225);
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

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(30, 0, 0, 0));
        body.add(createFormCard(), BorderLayout.NORTH);
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

        JLabel cardTitle = new JLabel("Thông tin máy tính");
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        cardTitle.setForeground(TEXT_COLOR);
        cardTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(cardTitle);

        JLabel cardDesc = new JLabel("Thông tin địa chỉ IP máy tính của bạn");
        cardDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cardDesc.setForeground(LABEL_COLOR);
        cardDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(Box.createVerticalStrut(4));
        card.add(cardDesc);

        card.add(Box.createVerticalStrut(20));
        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setForeground(new Color(226, 232, 240));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(separator);
        card.add(Box.createVerticalStrut(24));

        JPanel formWrapper = new JPanel();
        formWrapper.setLayout(new BoxLayout(formWrapper, BoxLayout.Y_AXIS));
        formWrapper.setOpaque(false);
        formWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        formWrapper.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));

        JLabel ipLabel = new JLabel("Địa chỉ IP");
        ipLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        ipLabel.setForeground(LABEL_COLOR);
        ipLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formWrapper.add(ipLabel);
        formWrapper.add(Box.createVerticalStrut(8));

        ipAddressField = new JTextField();
        ipAddressField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        ipAddressField.setForeground(TEXT_COLOR);
        ipAddressField.setBackground(new Color(241, 245, 249));
        ipAddressField.setEditable(false);
        ipAddressField.setMaximumSize(new Dimension(500, 44));
        ipAddressField.setPreferredSize(new Dimension(500, 44));
        ipAddressField.setAlignmentX(Component.LEFT_ALIGNMENT);
        ipAddressField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        formWrapper.add(ipAddressField);

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

    private void loadData() {
        statusLabel.setText("Đang tải dữ liệu...");
        statusLabel.setForeground(LABEL_COLOR);
        statusLabel.setVisible(true);

        pcService.fetchMyComputerAsync(new PersonalComputerService.FetchCallback() {
            @Override
            public void onSuccess(PersonalComputerResponse response) {
                statusLabel.setVisible(false);
                if (response.getData() != null && response.getData().getIpAddress() != null) {
                    ipAddressField.setText(response.getData().getIpAddress());
                } else {
                    ipAddressField.setText("");
                }
            }

            @Override
            public void onError(String errorMessage) {
                statusLabel.setText("⚠ " + errorMessage);
                statusLabel.setForeground(new Color(220, 38, 38));
                statusLabel.setVisible(true);
            }
        });
    }
}
