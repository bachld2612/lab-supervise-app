package com.bachld.ui;

import com.bachld.model.response.User;
import com.bachld.service.SessionManager;

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
    private static final Color TEXT_DARK = new Color(30, 41, 59);
    private static final Color ACTIVE_COLOR = new Color(85, 120, 235);
    private static final Color HOVER_BG = new Color(248, 250, 252);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);

    public SidebarPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setPreferredSize(new Dimension(280, 0));
        setBackground(BG_COLOR);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR));

        initComponents();
    }

    private void initComponents() {
        // ── Top: logo icon only ────────────────────────────────────────────────
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        topPanel.setOpaque(false);

        JLabel logoLabel = new JLabel();
        try {
            URL url = getClass().getResource("/images/tlu-logo.png");
            if (url != null) {
                ImageIcon raw = new ImageIcon(url);
                Image scaled = raw.getImage().getScaledInstance(44, 44, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception ignored) {}

        topPanel.add(logoLabel);
        add(topPanel, BorderLayout.NORTH);

        // ── Center: menu items ─────────────────────────────────────────────────
        JPanel menuPanel = new JPanel();
        menuPanel.setOpaque(false);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        addItem(menuPanel, "Quản lý lớp học", "CLASS_MGMT", "🏫");
        addItem(menuPanel, "Quản lý máy tính cá nhân", "PC_MGMT", "💻");
        addItem(menuPanel, "Báo cáo sự cố", "INCIDENT_REPORT", "🚨");

        add(menuPanel, BorderLayout.CENTER);

        // ── Bottom: hotline + user info ────────────────────────────────────────
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildBottomPanel() {
        JPanel bottom = new JPanel();
        bottom.setOpaque(true);
        bottom.setBackground(BG_COLOR);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

        // ── Hotline card (giống FE: border + border-radius + shadow) ──────────
        JPanel hotlineWrapper = new JPanel(new BorderLayout());
        hotlineWrapper.setOpaque(false);
        hotlineWrapper.setAlignmentX(LEFT_ALIGNMENT);
        hotlineWrapper.setBorder(new EmptyBorder(6, 16, 10, 16));

        JPanel hotlineCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // shadow
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fillRoundRect(2, 3, getWidth() - 2, getHeight() - 2, 12, 12);
                // background
                g2.setColor(BG_COLOR);
                g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 12, 12);
                // border
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        hotlineCard.setOpaque(false);
        hotlineCard.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 10));

        JLabel hotlineIcon = new JLabel("📞");
        hotlineIcon.setFont(new Font("Segoe UI Emoji", Font.BOLD, 15));

        JLabel hotlineText = new JLabel("Hotline TTTH: 024.3563.8072");
        hotlineText.setFont(new Font("Segoe UI", Font.BOLD, 12));
        hotlineText.setForeground(TEXT_DARK);

        hotlineCard.add(hotlineIcon);
        hotlineCard.add(hotlineText);
        hotlineWrapper.add(hotlineCard, BorderLayout.CENTER);
        bottom.add(hotlineWrapper);

        // ── User row (border-top, giống FE NavUser) ───────────────────────────
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        userPanel.setOpaque(true);
        userPanel.setBackground(BG_COLOR);
        userPanel.setAlignmentX(LEFT_ALIGNMENT);
        userPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));

        User currentUser = SessionManager.getInstance().getCurrentUser();
        String fullName = (currentUser != null && currentUser.getFullName() != null)
                ? currentUser.getFullName() : "?";
        String initial = fullName.isEmpty() ? "?" : String.valueOf(fullName.charAt(0)).toUpperCase();

        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACTIVE_COLOR);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(initial)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(initial, x, y);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(38, 38));
        avatar.setOpaque(false);

        JLabel lblName = new JLabel(fullName);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblName.setForeground(TEXT_DARK);

        userPanel.add(avatar);
        userPanel.add(lblName);
        bottom.add(userPanel);

        return bottom;
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
                if (!item.isActive()) item.setBackground(HOVER_BG);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!item.isActive()) item.setBackground(BG_COLOR);
            }
        });
    }

    public void setActiveItem(String id) {
        for (SidebarItem item : items) {
            item.setActive(item.id.equals(id));
        }
    }

    private static class SidebarItem extends JPanel {
        final String id;
        private boolean active = false;
        private final JLabel lblIcon;
        private final JLabel lblText;

        public SidebarItem(String text, String id, String iconText) {
            this.id = id;

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