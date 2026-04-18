package com.bachld.ui;

import com.bachld.config.AppConfig;
import com.bachld.service.AuthService;
import com.bachld.service.PersonalComputerService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainFrame extends JFrame {
    private final AuthService authService;
    private final PersonalComputerService pcService;
    private final com.bachld.service.ClassService classService;
    private final com.bachld.service.WebSocketService webSocketService;
    private com.bachld.service.WindowsTrackingService windowsTrackingService;
    private JPanel contentArea;
    private CardLayout cardLayout;
    private SidebarPanel sidebarPanel;

    public MainFrame(AuthService authService, PersonalComputerService pcService, com.bachld.service.ClassService classService, com.bachld.service.WebSocketService webSocketService) {
        this.authService = authService;
        this.pcService = pcService;
        this.classService = classService;
        this.webSocketService = webSocketService;
        
        // Initialize tracking for Windows
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            this.windowsTrackingService = new com.bachld.service.WindowsTrackingService(webSocketService);
            this.windowsTrackingService.start();
        }
        
        initFrame();
    }

    private void initFrame() {
        String appName = AppConfig.getInstance().getAppName();
        String version = AppConfig.getInstance().getAppVersion();
        setTitle(appName + " v" + version);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Full screen
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1024, 768));

        setLayout(new BorderLayout());

        // Sidebar
        sidebarPanel = new SidebarPanel(this);
        add(sidebarPanel, BorderLayout.WEST);

        // Main Wrapper
        JPanel mainWrapper = new JPanel(new BorderLayout());
        mainWrapper.setBackground(new Color(244, 247, 254));

        // Top Bar
        JPanel topBar = createTopBar();
        mainWrapper.add(topBar, BorderLayout.NORTH);

        // Main Content Area
        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setOpaque(false);
        contentArea.setBorder(new EmptyBorder(0, 30, 30, 30));

        // Pages
        contentArea.add(wrapInPageWrapper(new ClassManagementPanel(classService), "Quản lý lớp học"), "CLASS_MGMT");
        contentArea.add(new PersonalComputerPanel(pcService), "PC_MGMT");

        mainWrapper.add(contentArea, BorderLayout.CENTER);
        add(mainWrapper, BorderLayout.CENTER);

        // Default page after login
        showPage("PC_MGMT");
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 70));
        bar.setBorder(new EmptyBorder(0, 30, 0, 30));

        // Left side: Empty (Icon removed)
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        bar.add(left, BorderLayout.WEST);

        // Right side: Profile/Notifications
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 15));
        right.setOpaque(false);
        
        JLabel bell = new JLabel("🔔");
        bell.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        bell.setForeground(new Color(100, 116, 139));
        
        JLabel profile = new JLabel("👤");
        profile.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        profile.setForeground(new Color(100, 116, 139));
        profile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Profile Menu
        JPopupMenu profileMenu = new JPopupMenu();
        profileMenu.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        
        JMenuItem itemPassword = new JMenuItem("Đổi mật khẩu");
        itemPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        JMenuItem itemLogout = new JMenuItem("Đăng xuất");
        itemLogout.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        itemLogout.setForeground(new Color(220, 38, 38));
        
        itemLogout.addActionListener(e -> handleLogout());
        
        profileMenu.add(itemPassword);
        profileMenu.addSeparator();
        profileMenu.add(itemLogout);

        profile.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                profileMenu.show(profile, -profileMenu.getPreferredSize().width + profile.getWidth(), profile.getHeight());
            }
        });
        
        right.add(bell);
        right.add(profile);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
            this, 
            "Bạn có chắc chắn muốn đăng xuất?", 
            "Xác nhận đăng xuất", 
            JOptionPane.YES_NO_OPTION
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            // Stop tracking
            if (windowsTrackingService != null) {
                windowsTrackingService.stop();
            }
            // Disconnect WebSocket
            if (webSocketService != null) {
                webSocketService.disconnect();
            }
            // Clear token
            com.bachld.service.TokenManager.getInstance().clearToken();
            
            // Redirect to Login
            this.dispose();
            new LoginFrame(authService).setVisible(true);
        }
    }

    private JPanel wrapInPageWrapper(JPanel content, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        // Title Section
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        JLabel breadcrumb = new JLabel("Home  >  " + title);
        breadcrumb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        breadcrumb.setForeground(new Color(148, 163, 184));
        titlePanel.add(breadcrumb);
        titlePanel.add(Box.createVerticalStrut(10));
        
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(30, 41, 59));
        titlePanel.add(lblTitle);
        
        panel.add(titlePanel, BorderLayout.NORTH);
        
        // Body (Inject provided content)
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(30, 0, 0, 0));
        body.add(content, BorderLayout.CENTER);
        
        panel.add(body, BorderLayout.CENTER);
        
        return panel;
    }

    public void showPage(String pageId) {
        cardLayout.show(contentArea, pageId);
        sidebarPanel.setActiveItem(pageId);
    }
}
