package com.bachld.ui;

import com.bachld.service.SessionManager;
import com.bachld.service.UserService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;

public class ChangePasswordDialog extends JDialog {

    private static final Color TEXT_DARK      = new Color(30, 30, 30);
    private static final Color TEXT_HINT      = new Color(180, 180, 185);
    private static final Color BORDER_NORM    = new Color(220, 222, 230);
    private static final Color BTN_BLUE       = new Color(85, 120, 235);
    private static final Color BTN_HOVER      = new Color(70, 105, 220);
    private static final Color ERROR_RED      = new Color(220, 50, 50);
    private static final Color BTN_GRAY       = new Color(240, 242, 245);
    private static final Color BTN_GRAY_HOVER = new Color(226, 229, 235);

    private static final int FIELD_H = 44;
    private static final int CORNER  = 10;
    private static final int FORM_W  = 340;

    private JPasswordField txtOld;
    private JPasswordField txtNew;
    private JPasswordField txtConfirm;
    private JLabel         lblError;
    private JButton        btnSubmit;

    private final UserService userService;
    private final boolean mandatory;
    private final Runnable onSuccess;

    public ChangePasswordDialog(Window owner, UserService userService) {
        super(owner, "Đổi mật khẩu", ModalityType.APPLICATION_MODAL);
        this.userService = userService;
        this.mandatory = false;
        this.onSuccess = null;
        buildUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    public ChangePasswordDialog(Window owner, UserService userService, boolean mandatory, Runnable onSuccess) {
        super(owner, "\u0110\u1ed5i m\u1eadt kh\u1ea9u", ModalityType.APPLICATION_MODAL);
        this.userService = userService;
        this.mandatory = mandatory;
        this.onSuccess = onSuccess;
        buildUI();
        pack();
        setResizable(false);
        setDefaultCloseOperation(mandatory ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE);
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(32, 36, 32, 36));

        // Title
        JLabel title = new JLabel("Đổi mật khẩu");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT_DARK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(24));

        // Old password
        root.add(fieldLabel("Mật khẩu hiện tại"));
        root.add(Box.createVerticalStrut(8));
        txtOld = new JPasswordField();
        root.add(buildPasswordRow(txtOld));
        root.add(Box.createVerticalStrut(16));

        // New password
        root.add(fieldLabel("Mật khẩu mới"));
        root.add(Box.createVerticalStrut(8));
        txtNew = new JPasswordField();
        root.add(buildPasswordRow(txtNew));
        root.add(Box.createVerticalStrut(16));

        // Confirm password
        root.add(fieldLabel("Xác nhận mật khẩu mới"));
        root.add(Box.createVerticalStrut(8));
        txtConfirm = new JPasswordField();
        root.add(buildPasswordRow(txtConfirm));
        root.add(Box.createVerticalStrut(16));

        // Error label
        lblError = new JLabel(" ");
        lblError.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblError.setForeground(ERROR_RED);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(lblError);
        root.add(Box.createVerticalStrut(20));

        // Buttons row
        JPanel btnRow = new JPanel(new GridLayout(1, mandatory ? 1 : 2, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(FORM_W, FIELD_H));

        JButton btnCancel = createButton("Hủy", BTN_GRAY, BTN_GRAY_HOVER, TEXT_DARK);
        btnCancel.addActionListener(e -> dispose());

        btnSubmit = createButton("Xác nhận", BTN_BLUE, BTN_HOVER, Color.WHITE);
        btnSubmit.addActionListener(e -> handleSubmit());

        if (!mandatory) {
            btnRow.add(btnCancel);
        }
        btnRow.add(btnSubmit);
        root.add(btnRow);

        getRootPane().setDefaultButton(btnSubmit);
        setContentPane(root);
    }

    // ── Password row with eye button ──────────────────────────────────────────

    private JPanel buildPasswordRow(JPasswordField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(TEXT_DARK);
        field.setBackground(Color.WHITE);
        field.setOpaque(false);
        field.setBorder(new EmptyBorder(0, 14, 0, 4));
        field.setEchoChar('•');

        boolean[] visible = {false};

        JButton eye = new JButton();
        eye.setPreferredSize(new Dimension(40, FIELD_H));
        eye.setMinimumSize(new Dimension(40, FIELD_H));
        eye.setMaximumSize(new Dimension(40, FIELD_H));
        eye.setOpaque(false);
        eye.setContentAreaFilled(false);
        eye.setBorderPainted(false);
        eye.setFocusPainted(false);
        eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setEyeIcon(eye, false);

        eye.addActionListener(e -> {
            visible[0] = !visible[0];
            field.setEchoChar(visible[0] ? (char) 0 : '•');
            setEyeIcon(eye, visible[0]);
        });

        JPanel row = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER, CORNER);
                g2.dispose();
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BORDER_NORM);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER, CORNER);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(FORM_W, FIELD_H));
        row.setMaximumSize(new Dimension(FORM_W, FIELD_H));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(field, BorderLayout.CENTER);
        row.add(eye, BorderLayout.EAST);

        return row;
    }

    private void setEyeIcon(JButton btn, boolean visible) {
        btn.setIcon(new Icon() {
            @Override public int getIconWidth()  { return 22; }
            @Override public int getIconHeight() { return 22; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEXT_HINT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x + 2, y + 6, 18, 10);
                g2.fillOval(x + 8, y + 9, 6, 6);
                if (!visible) {
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(x + 3, y + 18, x + 19, y + 4);
                }
                g2.dispose();
            }
        });
    }

    // ── Submit logic ──────────────────────────────────────────────────────────

    private void handleSubmit() {
        lblError.setText(" ");

        char[] oldChars     = txtOld.getPassword();
        char[] newChars     = txtNew.getPassword();
        char[] confirmChars = txtConfirm.getPassword();

        String oldPass     = new String(oldChars);
        String newPass     = new String(newChars);
        String confirmPass = new String(confirmChars);

        Arrays.fill(oldChars, '\0');
        Arrays.fill(newChars, '\0');
        Arrays.fill(confirmChars, '\0');

        if (oldPass.isBlank()) {
            lblError.setText("Vui lòng nhập mật khẩu hiện tại.");
            return;
        }
        if (newPass.length() < 6) {
            lblError.setText("Mật khẩu mới phải chứa ít nhất 6 ký tự.");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            lblError.setText("Xác nhận mật khẩu không khớp.");
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang xử lý...");

        userService.changePasswordAsync(oldPass, newPass, new UserService.ChangePasswordCallback() {
            @Override
            public void onSuccess() {
                JOptionPane.showMessageDialog(
                    ChangePasswordDialog.this,
                    "Đổi mật khẩu thành công.",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE
                );
                if (SessionManager.getInstance().getCurrentUser() != null) {
                    SessionManager.getInstance().getCurrentUser().setRawPassword(null);
                }
                if (onSuccess != null) {
                    onSuccess.run();
                }
                dispose();
            }

            @Override
            public void onError(String errorMessage) {
                lblError.setText(errorMessage);
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Xác nhận");
            }
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(new Color(71, 85, 105));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JButton createButton(String text, Color bg, Color hover, Color fg) {
        JButton btn = new JButton(text) {
            private Color currentBg = bg;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER, CORNER);
                g2.dispose();
                super.paintComponent(g);
            }

            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                        if (isEnabled()) { currentBg = hover; repaint(); }
                    }
                    @Override public void mouseExited(java.awt.event.MouseEvent e) {
                        currentBg = bg; repaint();
                    }
                });
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, FIELD_H));
        return btn;
    }
}
