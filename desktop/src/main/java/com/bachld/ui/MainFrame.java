package com.bachld.ui;

import com.bachld.config.AppConfig;
import com.bachld.model.response.PersonalComputerResponse;
import com.bachld.service.AuthService;
import com.bachld.service.PersonalComputerService;
import com.bachld.service.UserService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.net.URL;

public class MainFrame extends JFrame {
    private final AuthService authService;
    private final PersonalComputerService pcService;
    private final com.bachld.service.ClassService classService;
    private final com.bachld.service.WebSocketService webSocketService;
    private final com.bachld.service.IncidentReportService incidentReportService;
    private final com.bachld.service.ExamRoomService examRoomService;
    private final com.bachld.service.SemesterService semesterService;
    private final UserService userService;
    private com.bachld.service.TrackingService trackingService;
    private final com.bachld.service.VncService vncService = new com.bachld.service.VncService();
    private JPanel contentArea;
    private CardLayout cardLayout;
    private SidebarPanel sidebarPanel;
    private TrayIcon trayIcon;
    private PersonalComputerPanel pcPanel;

    public MainFrame(AuthService authService, PersonalComputerService pcService,
                     com.bachld.service.ClassService classService,
                     com.bachld.service.WebSocketService webSocketService,
                     com.bachld.service.IncidentReportService incidentReportService,
                     com.bachld.service.ExamRoomService examRoomService,
                     com.bachld.service.SemesterService semesterService,
                     UserService userService) {
        this.authService = authService;
        this.pcService = pcService;
        this.classService = classService;
        this.webSocketService = webSocketService;
        this.incidentReportService = incidentReportService;
        this.examRoomService = examRoomService;
        this.semesterService = semesterService;
        this.userService = userService;

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            this.trackingService = new com.bachld.service.WindowsTrackingService(webSocketService);
        } else if (os.contains("linux")) {
            this.trackingService = new com.bachld.service.LinuxX11TrackingService(webSocketService);
        }
        if (this.trackingService != null) {
            this.trackingService.start();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(vncService::stop));
        Thread vncThread = new Thread(vncService::start, "vnc-start");
        vncThread.setDaemon(true);
        vncThread.start();

        initFrame();
        setupSystemTray();
    }

    private void initFrame() {
        String appName = AppConfig.getInstance().getAppName();
        String version = AppConfig.getInstance().getAppVersion();
        setTitle(appName + " v" + version);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1024, 768));

        setLayout(new BorderLayout());

        sidebarPanel = new SidebarPanel(this);
        add(sidebarPanel, BorderLayout.WEST);

        JPanel mainWrapper = new JPanel(new BorderLayout());
        mainWrapper.setBackground(new Color(244, 247, 254));

        JPanel topBar = createTopBar();
        mainWrapper.add(topBar, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setOpaque(false);
        contentArea.setBorder(new EmptyBorder(0, 30, 30, 30));

        contentArea.add(wrapInPageWrapper(new DashboardPanel(classService, examRoomService, semesterService), "Trang chủ"), "HOME");
        pcPanel = new PersonalComputerPanel(pcService);
        contentArea.add(pcPanel, "PC_MGMT");
        contentArea.add(wrapInPageWrapper(new IncidentReportPanel(incidentReportService), "Báo cáo sự cố"), "INCIDENT_REPORT");

        mainWrapper.add(contentArea, BorderLayout.CENTER);
        add(mainWrapper, BorderLayout.CENTER);

        showPage("HOME");
    }

    // ── System Tray ───────────────────────────────────────────────────────────

    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            return;
        }

        Image icon = loadAppIcon(16);

        PopupMenu popup = new PopupMenu();

        MenuItem itemOpen = new MenuItem("Mở ứng dụng");
        itemOpen.addActionListener(e -> SwingUtilities.invokeLater(this::restoreWindow));

        MenuItem itemExit = new MenuItem("Thoát");
        itemExit.addActionListener(e -> exitApplication());

        popup.add(itemOpen);
        popup.addSeparator();
        popup.add(itemExit);

        trayIcon = new TrayIcon(icon, AppConfig.getInstance().getAppName(), popup);
        trayIcon.setImageAutoSize(true);
        // Double-click on tray icon restores the window
        trayIcon.addActionListener(e -> SwingUtilities.invokeLater(this::restoreWindow));

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            trayIcon = null;
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            return;
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
                trayIcon.displayMessage(
                    AppConfig.getInstance().getAppName(),
                    "Ứng dụng vẫn đang chạy nền. Nhấp đúp vào biểu tượng để mở lại.",
                    TrayIcon.MessageType.INFO
                );
            }
        });
    }

    private void restoreWindow() {
        setVisible(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        toFront();
        requestFocus();
    }

    private void exitApplication() {
        if (trackingService != null) {
            trackingService.stop();
        }
        if (webSocketService != null) {
            webSocketService.disconnect();
        }
        vncService.stop();
        com.bachld.service.TokenManager.getInstance().clearToken();
        removeTrayIcon();
        System.exit(0);
    }

    private void removeTrayIcon() {
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
    }

    private Image loadAppIcon(int size) {
        try {
            URL url = getClass().getResource("/images/tlu-logo.png");
            if (url != null) {
                return new ImageIcon(url).getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            }
        } catch (Exception ignored) {}
        // Fallback: blue square
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(new Color(85, 120, 235));
        g2.fillRect(0, 0, size, size);
        g2.dispose();
        return img;
    }

    // ── Top Bar ───────────────────────────────────────────────────────────────

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 70));
        bar.setBorder(new EmptyBorder(0, 30, 0, 30));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 15));
        right.setOpaque(false);

        JLabel profile = new JLabel("👤");
        profile.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        profile.setForeground(new Color(100, 116, 139));
        profile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPopupMenu profileMenu = new JPopupMenu();
        profileMenu.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));

        JMenuItem itemPassword = new JMenuItem("Đổi mật khẩu");
        itemPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        itemPassword.addActionListener(e -> new ChangePasswordDialog(this, userService).setVisible(true));

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

        right.add(profile);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Bạn có chắc chắn muốn đăng xuất?",
            "Xác nhận đăng xuất",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (trackingService != null) {
                trackingService.stop();
            }
            if (webSocketService != null) {
                webSocketService.disconnect();
            }
            vncService.stop();
            com.bachld.service.TokenManager.getInstance().clearToken();
            removeTrayIcon();
            this.dispose();
            new LoginFrame(authService).setVisible(true);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JPanel wrapInPageWrapper(JPanel content, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

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

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(30, 0, 0, 0));
        body.add(content, BorderLayout.CENTER);

        panel.add(body, BorderLayout.CENTER);

        return panel;
    }

    // ── IP Check on Startup ───────────────────────────────────────────────────

    public void checkIpOnStartup() {
        pcService.fetchMyComputerAsync(new PersonalComputerService.FetchCallback() {
            @Override
            public void onSuccess(PersonalComputerResponse response) {
                String storedIp = (response.getData() != null) ? response.getData().getIpAddress() : null;
                if (storedIp != null && !storedIp.isBlank()) {
                    showIpExistsDialog(storedIp);
                } else {
                    showIpMissingDialog();
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Silent fail — do not disrupt user if server is unreachable
            }
        });
    }

    private void showIpExistsDialog(String storedIp) {
        JOptionPane.showMessageDialog(
                this,
                "Địa chỉ IP hiện tại của bạn là: " + storedIp
                        + "\nNếu địa chỉ IP chưa chính xác, vui lòng báo cáo lại giáo viên để được thay đổi.",
                "Thông tin địa chỉ IP",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showIpMissingDialog() {
        JOptionPane.showMessageDialog(
                this,
                "Chưa có thông tin địa chỉ IP cho máy tính của bạn."
                        + "\nVui lòng báo cáo lại giáo viên để được cập nhật.",
                "Chưa có địa chỉ IP",
                JOptionPane.WARNING_MESSAGE
        );
    }

    public void showPage(String pageId) {
        cardLayout.show(contentArea, pageId);
        sidebarPanel.setActiveItem(pageId);
    }
}