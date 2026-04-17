package com.bachld.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SidebarPanel extends JPanel {
    private final MainFrame mainFrame;
    private final List<SidebarItem> items = new ArrayList<>();
    
    private static final Color BG_COLOR = Color.WHITE;
    private static final Color TEXT_COLOR = new Color(100, 116, 139);
    private static final Color ACTIVE_COLOR = new Color(85, 120, 235);
    private static final Color HOVER_BG = new Color(248, 250, 252);

    public SidebarPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setPreferredSize(new Dimension(280, 0));
        setBackground(BG_COLOR);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(226, 232, 240)));

        initComponents();
    }

    private void initComponents() {
        // Top Logo Section
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 30));
        topPanel.setOpaque(false);
        
        JLabel logoLabel = new JLabel();
        try {
            URL url = getClass().getResource("/images/tlu-logo.png");
            if (url != null) {
                ImageIcon raw = new ImageIcon(url);
                Image scaled = raw.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception ignored) {}
        
        JLabel nameLabel = new JLabel("VĂN PHÒNG KHOA");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(new Color(51, 65, 85));
        
        topPanel.add(logoLabel);
        topPanel.add(nameLabel);
        add(topPanel, BorderLayout.NORTH);

        // Menu Section
        JPanel menuPanel = new JPanel();
        menuPanel.setOpaque(false);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        addItem(menuPanel, "Quản lý lớp học", "CLASS_MGMT", "🏫");
        addItem(menuPanel, "Quản lý máy tính cá nhân", "PC_MGMT", "💻");

        add(menuPanel, BorderLayout.CENTER);
    }

    private void addItem(JPanel parent, String text, String id, String icon) {
        SidebarItem item = new SidebarItem(text, id, icon);
        items.add(item);
        parent.add(item);
        
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mainFrame.showPage(id);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!item.isActive()) {
                    item.setBackground(HOVER_BG);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!item.isActive()) {
                    item.setBackground(BG_COLOR);
                }
            }
        });
    }

    public void setActiveItem(String id) {
        for (SidebarItem item : items) {
            item.setActive(item.id.equals(id));
        }
    }

    private static class SidebarItem extends JPanel {
        private final String id;
        private final String text;
        private final String iconText;
        private boolean active = false;
        private final JLabel lblIcon;
        private final JLabel lblText;

        public SidebarItem(String text, String id, String iconText) {
            this.id = id;
            this.text = text;
            this.iconText = iconText;
            
            setLayout(new FlowLayout(FlowLayout.LEFT, 20, 15));
            setBackground(BG_COLOR);
            setMaximumSize(new Dimension(280, 50));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            lblIcon = new JLabel(iconText);
            lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            lblIcon.setForeground(TEXT_COLOR);

            lblText = new JLabel(text);
            lblText.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lblText.setForeground(TEXT_COLOR);

            add(lblIcon);
            add(lblText);
        }

        public void setActive(boolean active) {
            this.active = active;
            if (active) {
                setBackground(HOVER_BG);
                lblIcon.setForeground(ACTIVE_COLOR);
                lblText.setForeground(ACTIVE_COLOR);
            } else {
                setBackground(BG_COLOR);
                lblIcon.setForeground(TEXT_COLOR);
                lblText.setForeground(TEXT_COLOR);
            }
            repaint();
        }

        public boolean isActive() {
            return active;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (active) {
                g.setColor(ACTIVE_COLOR);
                g.fillRect(0, 0, 4, getHeight());
            }
        }
    }
}
