package com.bachld.ui;

import com.bachld.model.response.AuthResponse;
import com.bachld.service.AuthService;
import com.bachld.util.EmailValidator;
import com.bachld.util.PasswordValidator;
import com.bachld.util.ValidationResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.net.URL;
import java.util.Arrays;

public class LoginPanel extends JPanel {

    // ── Colours ──────────────────────────────────────────────────────────────
    private static final Color WHITE        = Color.WHITE;
    private static final Color TEXT_DARK    = new Color(30,  30,  30);
    private static final Color TEXT_HINT    = new Color(180, 180, 185);
    private static final Color BORDER_NORM  = new Color(220, 222, 230);
    private static final Color BORDER_FOCUS = new Color(99,  130, 236);
    private static final Color BTN_BLUE     = new Color(85,  120, 235);
    private static final Color BTN_HOVER    = new Color(70,  105, 220);
    private static final Color BTN_PRESS    = new Color(55,   85, 200);
    private static final Color LINK_DARK    = new Color(60,   60,  65);
    private static final Color ERROR_RED    = new Color(220,  50,  50);

    // ── Dimensions ───────────────────────────────────────────────────────────
    private static final int FORM_W      = 360;
    private static final int FIELD_H     = 46;
    private static final int CORNER      = 12;

    // ── Components ───────────────────────────────────────────────────────────
    private JTextField     txtEmail;
    private JPasswordField txtPassword;
    private JButton        btnEye;
    private JButton        btnLogin;
    private JLabel         lblEmailError;
    private JLabel         lblPasswordError;
    private JLabel         lblGeneralError;

    private boolean passwordVisible = false;
    private Color   passwordBorderColor = BORDER_NORM;
    private final AuthService authService;

    // ─────────────────────────────────────────────────────────────────────────
    public LoginPanel(AuthService authService) {
        this.authService = authService;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        buildForm();
    }

    // ── Build form ───────────────────────────────────────────────────────────
    private void buildForm() {
        // Logo — centered
        add(Box.createVerticalStrut(8));
        add(centeredRow(buildLogo()));
        add(Box.createVerticalStrut(20));

        // Title — centered
        JLabel title = new JLabel("Đăng nhập");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_DARK);
        add(centeredRow(title));
        add(Box.createVerticalStrut(32));

        // Email label — left-aligned within FORM_W
        add(leftRow(fieldLabel("Email")));
        add(Box.createVerticalStrut(8));
        txtEmail = buildTextField("Nhập email");
        add(centeredRow(txtEmail));
        lblEmailError = errorLabel();
        add(leftRow(lblEmailError));
        add(Box.createVerticalStrut(8));

        // Password label
        add(leftRow(fieldLabel("Mật khẩu")));
        add(Box.createVerticalStrut(8));
        add(centeredRow(buildPasswordRow()));
        lblPasswordError = errorLabel();
        add(leftRow(lblPasswordError));

        // Forgot password — right-aligned within FORM_W
        add(rightRow(buildForgotLink()));
        add(Box.createVerticalStrut(12));

        // General error
        lblGeneralError = errorLabel();
        add(leftRow(lblGeneralError));
        add(Box.createVerticalStrut(4));

        // Login button
        btnLogin = buildLoginButton();
        add(centeredRow(btnLogin));
    }

    // ── Row wrappers (keep everything aligned within FORM_W) ─────────────────

    /** Wraps a component in a fixed-width row, centered horizontally */
    private JPanel centeredRow(JComponent child) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(CENTER_ALIGNMENT);
        row.setMaximumSize(new Dimension(FORM_W, child.getPreferredSize().height));

        row.add(Box.createHorizontalGlue());
        row.add(child);
        row.add(Box.createHorizontalGlue());
        return row;
    }

    /** Left-aligned row within FORM_W */
    private JPanel leftRow(JComponent child) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(CENTER_ALIGNMENT);
        row.setMaximumSize(new Dimension(FORM_W, child.getPreferredSize().height));
        row.add(child);
        return row;
    }

    /** Right-aligned row within FORM_W */
    private JPanel rightRow(JComponent child) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(CENTER_ALIGNMENT);
        row.setMaximumSize(new Dimension(FORM_W, 22));
        row.add(child);
        return row;
    }

    // ── Logo ─────────────────────────────────────────────────────────────────
    private JLabel buildLogo() {
        JLabel lbl = new JLabel();
        try {
            URL url = getClass().getResource("/images/tlu-logo.png");
            if (url != null) {
                ImageIcon raw = new ImageIcon(url);
                Image scaled = raw.getImage().getScaledInstance(76, 76, Image.SCALE_SMOOTH);
                lbl.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception ignored) {}
        return lbl;
    }

    // ── Field helpers ─────────────────────────────────────────────────────────
    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(TEXT_DARK);
        return lbl;
    }

    private JTextField buildTextField(String hint) {
        JTextField f = new PlaceholderField(hint);
        styleField(f);
        return f;
    }

    private JPanel buildPasswordRow() {
        txtPassword = new PlaceholderPasswordField("Nhập mật khẩu");
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.setForeground(TEXT_DARK);
        txtPassword.setBackground(WHITE);
        txtPassword.setCaretColor(BORDER_FOCUS);
        txtPassword.setOpaque(false);
        txtPassword.setBorder(new EmptyBorder(0, 14, 0, 4));

        btnEye = new JButton();
        btnEye.setPreferredSize(new Dimension(40, FIELD_H));
        btnEye.setMinimumSize(new Dimension(40, FIELD_H));
        btnEye.setMaximumSize(new Dimension(40, FIELD_H));
        btnEye.setOpaque(false);
        btnEye.setContentAreaFilled(false);
        btnEye.setBorderPainted(false);
        btnEye.setFocusPainted(false);
        btnEye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setEyeIcon(false);
        btnEye.addActionListener(e -> togglePassword());

        JPanel row = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), CORNER, CORNER));
                g2.dispose();
            }
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(passwordBorderColor);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, CORNER, CORNER));
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(FORM_W, FIELD_H));
        row.setMaximumSize(new Dimension(FORM_W, FIELD_H));
        row.setMinimumSize(new Dimension(FORM_W, FIELD_H));

        txtPassword.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                passwordBorderColor = BORDER_FOCUS;
                row.repaint();
            }
            @Override public void focusLost(FocusEvent e) {
                passwordBorderColor = BORDER_NORM;
                row.repaint();
            }
        });

        row.add(txtPassword, BorderLayout.CENTER);
        row.add(btnEye, BorderLayout.EAST);
        return row;
    }

    private void styleField(JTextField f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setForeground(TEXT_DARK);
        f.setBackground(WHITE);
        f.setCaretColor(BORDER_FOCUS);
        f.setPreferredSize(new Dimension(FORM_W, FIELD_H));
        f.setMaximumSize(new Dimension(FORM_W, FIELD_H));
        f.setMinimumSize(new Dimension(FORM_W, FIELD_H));
        f.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(BORDER_NORM, CORNER, 1.2f),
                new EmptyBorder(0, 14, 0, 14)));
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                        new RoundBorder(BORDER_FOCUS, CORNER, 1.2f),
                        new EmptyBorder(0, 14, 0, 14)));
            }
            @Override public void focusLost(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                        new RoundBorder(BORDER_NORM, CORNER, 1.2f),
                        new EmptyBorder(0, 14, 0, 14)));
            }
        });
    }

    private JLabel buildForgotLink() {
        JLabel lbl = new JLabel("Quên mật khẩu?");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(LINK_DARK);
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                lbl.setForeground(BORDER_FOCUS);
            }
            @Override public void mouseExited(MouseEvent e) {
                lbl.setForeground(LINK_DARK);
            }
            @Override public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(LoginPanel.this,
                        "Chức năng quên mật khẩu chưa được triển khai.", "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
        return lbl;
    }

    private JLabel errorLabel() {
        JLabel lbl = new JLabel(" ");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(ERROR_RED);
        return lbl;
    }

    private JButton buildLoginButton() {
        JButton btn = new JButton("Đăng Nhập") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = isEnabled()
                        ? (getModel().isPressed() ? BTN_PRESS : getModel().isRollover() ? BTN_HOVER : BTN_BLUE)
                        : TEXT_HINT;
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), CORNER, CORNER));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setForeground(WHITE);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(FORM_W, 48));
        btn.setMaximumSize(new Dimension(FORM_W, 48));
        btn.setMinimumSize(new Dimension(FORM_W, 48));
        btn.addActionListener(e -> onLoginClicked());
        return btn;
    }

    // ── Eye icon ──────────────────────────────────────────────────────────────
    private void setEyeIcon(boolean visible) {
        btnEye.setIcon(new Icon() {
            @Override public int getIconWidth()  { return 22; }
            @Override public int getIconHeight() { return 22; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEXT_HINT);
                g2.setStroke(new BasicStroke(1.5f));
                // eye outline
                g2.drawOval(x + 2, y + 6, 18, 10);
                // pupil
                g2.fillOval(x + 8, y + 9, 6, 6);
                if (!visible) {
                    // slash through eye
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(x + 3, y + 18, x + 19, y + 4);
                }
                g2.dispose();
            }
        });
    }

    private void togglePassword() {
        passwordVisible = !passwordVisible;
        txtPassword.setEchoChar(passwordVisible ? (char) 0 : '•');
        setEyeIcon(passwordVisible);
    }

    // ── Login logic ───────────────────────────────────────────────────────────
    private void onLoginClicked() {
        clearErrors();

        String email = txtEmail.getText().trim();
        char[] pwdChars = txtPassword.getPassword();
        String password = new String(pwdChars);

        ValidationResult ev = EmailValidator.validate(email);
        if (!ev.isValid()) { showEmailError(ev.getErrorMessage()); Arrays.fill(pwdChars, '\0'); return; }

        ValidationResult pv = PasswordValidator.validate(password);
        if (!pv.isValid()) { showPasswordError(pv.getErrorMessage()); Arrays.fill(pwdChars, '\0'); return; }

        setLoginEnabled(false);

        authService.loginAsync(email, password, new AuthService.AuthCallback() {
            @Override public void onSuccess(AuthResponse response) {
                setLoginEnabled(true);
                JOptionPane.showMessageDialog(LoginPanel.this,
                        "Đăng nhập thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            }
            @Override public void onError(String msg) {
                showGeneralError(msg);
                txtPassword.setText("");
                setLoginEnabled(true);
            }
        });

        Arrays.fill(pwdChars, '\0');
    }

    // ── Public API ────────────────────────────────────────────────────────────
    public void showEmailError(String msg)    { lblEmailError.setText(msg); }
    public void showPasswordError(String msg) { lblPasswordError.setText(msg); }
    public void showGeneralError(String msg)  { lblGeneralError.setText(msg); }
    public void clearErrors() {
        lblEmailError.setText(" ");
        lblPasswordError.setText(" ");
        lblGeneralError.setText(" ");
    }
    public void setLoginEnabled(boolean e) { btnLogin.setEnabled(e); }
    public String getEmail()    { return txtEmail.getText().trim(); }
    public String getPassword() { return new String(txtPassword.getPassword()).trim(); }

    // ── Inner helpers ─────────────────────────────────────────────────────────

    /** TextField with placeholder text */
    private static class PlaceholderField extends JTextField {
        private final String hint;
        PlaceholderField(String hint) { this.hint = hint; }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !hasFocus()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(180, 180, 185));
                g2.setFont(getFont());
                Insets ins = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(hint, ins.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    }

    /** PasswordField with placeholder text */
    private static class PlaceholderPasswordField extends JPasswordField {
        private final String hint;
        PlaceholderPasswordField(String hint) { this.hint = hint; setEchoChar('•'); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getPassword().length == 0 && !hasFocus()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(180, 180, 185));
                g2.setFont(getFont());
                Insets ins = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(hint, ins.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    }

    /** Rounded border with configurable stroke width */
    private static class RoundBorder extends javax.swing.border.AbstractBorder {
        private final Color color;
        private final int radius;
        private final float strokeWidth;
        RoundBorder(Color color, int radius) { this(color, radius, 1f); }
        RoundBorder(Color color, int radius, float strokeWidth) {
            this.color = color;
            this.radius = radius;
            this.strokeWidth = strokeWidth;
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(strokeWidth));
            g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1, h - 1, radius, radius));
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(1,1,1,1); }
    }
}
