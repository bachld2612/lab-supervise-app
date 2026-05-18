package com.bachld.ui;

import com.bachld.model.response.IncidentReportData;
import com.bachld.model.response.IncidentReportListResponse;
import com.bachld.service.IncidentReportService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.List;

public class IncidentReportPanel extends JPanel {

    private final IncidentReportService incidentService;

    private JTextField txtTitle;
    private JButton btnSubmit;
    private JLabel lblFeedback;

    private JTable table;
    private DefaultTableModel tableModel;
    private List<IncidentReportData> reports = new ArrayList<>();

    private static final Color BG_COLOR = new Color(244, 247, 254);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color PRIMARY = new Color(85, 120, 235);
    private static final Color TEXT_DARK = new Color(30, 41, 59);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color ERROR_COLOR = new Color(220, 38, 38);
    private static final Color SUCCESS_COLOR = new Color(22, 163, 74);
    private static final Color WARNING_COLOR = new Color(245, 158, 11);

    private static final String[] STATUS_LABELS = {"Chờ xử lý", "Đã xử lý", "Từ chối"};
    private static final String[] COLUMN_NAMES = {"STT", "Tiêu đề sự cố", "Phòng học", "Trạng thái", "Thời gian", "Thao tác"};

    public IncidentReportPanel(IncidentReportService incidentService) {
        this.incidentService = incidentService;
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        initUI();

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                loadReports();
            }
        });
    }

    private void initUI() {
        JPanel createCard = buildCreateCard();
        JPanel listCard = buildListCard();

        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setOpaque(false);
        content.add(createCard, BorderLayout.NORTH);
        content.add(listCard, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
    }

    // ── Create report card ─────────────────────────────────────────────────────

    private JPanel buildCreateCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(24, 24, 24, 24)
        ));

        JLabel cardTitle = new JLabel("Gửi báo cáo sự cố");
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        cardTitle.setForeground(TEXT_DARK);
        cardTitle.setAlignmentX(LEFT_ALIGNMENT);
        card.add(cardTitle);

        JLabel cardDesc = new JLabel("Mô tả ngắn gọn sự cố đang xảy ra trong phòng máy");
        cardDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cardDesc.setForeground(TEXT_MUTED);
        cardDesc.setAlignmentX(LEFT_ALIGNMENT);
        card.add(Box.createVerticalStrut(4));
        card.add(cardDesc);

        card.add(Box.createVerticalStrut(16));
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(BORDER_COLOR);
        sep.setAlignmentX(LEFT_ALIGNMENT);
        card.add(sep);
        card.add(Box.createVerticalStrut(16));

        // Title field
        JLabel lblTitle = new JLabel("Tiêu đề sự cố");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(TEXT_DARK);
        lblTitle.setAlignmentX(LEFT_ALIGNMENT);
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(6));

        txtTitle = new JTextField();
        txtTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtTitle.setPreferredSize(new Dimension(500, 38));
        txtTitle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(4, 10, 4, 10)
        ));

        btnSubmit = new JButton("Gửi báo cáo");
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSubmit.setForeground(TEXT_DARK);
        btnSubmit.setBackground(PRIMARY);
        btnSubmit.setPreferredSize(new Dimension(130, 38));
        btnSubmit.setFocusPainted(false);
        btnSubmit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSubmit.addActionListener(e -> handleSubmit());

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        inputRow.setAlignmentX(LEFT_ALIGNMENT);
        inputRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        inputRow.add(txtTitle, BorderLayout.CENTER);
        inputRow.add(btnSubmit, BorderLayout.EAST);
        card.add(inputRow);
        card.add(Box.createVerticalStrut(4));

        lblFeedback = new JLabel(" ");
        lblFeedback.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblFeedback.setForeground(ERROR_COLOR);
        lblFeedback.setAlignmentX(LEFT_ALIGNMENT);
        card.add(lblFeedback);

        return card;
    }

    // ── Report list card ───────────────────────────────────────────────────────

    private JPanel buildListCard() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(20, 20, 20, 20)
        ));

        // Header row
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblListTitle = new JLabel("Báo cáo đã gửi");
        lblListTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblListTitle.setForeground(TEXT_DARK);
        header.add(lblListTitle, BorderLayout.WEST);

        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefresh.setForeground(PRIMARY);
        btnRefresh.setBackground(new Color(239, 246, 255));
        btnRefresh.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(191, 219, 254), 1),
                new EmptyBorder(4, 12, 4, 12)
        ));
        btnRefresh.setFocusPainted(false);
        btnRefresh.setOpaque(true);
        btnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> loadReports());
        header.add(btnRefresh, BorderLayout.EAST);

        card.add(header, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(36);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.setGridColor(BORDER_COLOR);
        table.setSelectionBackground(new Color(219, 234, 254));
        table.setSelectionForeground(TEXT_DARK);

        // Status column renderer — badge style
        table.getColumnModel().getColumn(3).setCellRenderer((t, value, isSelected, hasFocus, row, col) -> {
            String text = value != null ? value.toString() : "";
            JPanel cell = new JPanel(new GridBagLayout());
            cell.setOpaque(true);
            cell.setBackground(isSelected ? t.getSelectionBackground() : t.getBackground());

            Color bgColor, fgColor;
            if ("Chờ xử lý".equals(text)) {
                bgColor = new Color(255, 243, 205);
                fgColor = new Color(130, 80, 0);
            } else if ("Đã xử lý".equals(text)) {
                bgColor = new Color(209, 250, 229);
                fgColor = new Color(6, 95, 70);
            } else if ("Từ chối".equals(text)) {
                bgColor = new Color(254, 226, 226);
                fgColor = new Color(153, 27, 27);
            } else {
                bgColor = new Color(241, 245, 249);
                fgColor = TEXT_DARK;
            }

            JLabel badge = new JLabel(text);
            badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
            if (isSelected) {
                badge.setForeground(fgColor);
                badge.setBackground(bgColor);
                badge.setOpaque(true);
                badge.setBorder(new EmptyBorder(3, 10, 3, 10));
            } else {
                badge.setForeground(fgColor);
                badge.setBackground(bgColor);
                badge.setOpaque(true);
                badge.setBorder(new EmptyBorder(3, 10, 3, 10));
            }
            cell.add(badge);
            return cell;
        });

        // Action column: edit button (status=0 only)
        table.getColumnModel().getColumn(5).setCellRenderer((t, value, isSelected, hasFocus, row, col) -> {
            int modelRow = t.convertRowIndexToModel(row);
            if (modelRow < reports.size() && reports.get(modelRow).getStatus() == 0) {
                JButton btn = new JButton("Sửa");
                btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btn.setBackground(PRIMARY);
                btn.setForeground(TEXT_DARK);
                btn.setFocusPainted(false);
                btn.setBorderPainted(false);
                btn.setOpaque(true);
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return btn;
            }
            JLabel lbl = new JLabel("—");
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setOpaque(true);
            lbl.setBackground(isSelected ? t.getSelectionBackground() : t.getBackground());
            lbl.setForeground(TEXT_MUTED);
            return lbl;
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (col == 5 && row >= 0 && row < reports.size()) {
                    IncidentReportData report = reports.get(row);
                    if (report.getStatus() == 0) {
                        showEditDialog(report);
                    }
                }
            }
        });

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(140);
        table.getColumnModel().getColumn(5).setPreferredWidth(70);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        card.add(scrollPane, BorderLayout.CENTER);

        return card;
    }

    // ── Actions ────────────────────────────────────────────────────────────────

    private void handleSubmit() {
        String title = txtTitle.getText().trim();
        if (title.isEmpty()) {
            setFeedback("Tiêu đề không được phép bỏ trống", false);
            return;
        }

        btnSubmit.setEnabled(false);
        setFeedback("Đang gửi...", null);

        incidentService.createReportAsync(title, new IncidentReportService.OperationCallback() {
            @Override
            public void onSuccess() {
                SwingUtilities.invokeLater(() -> {
                    setFeedback("Gửi báo cáo thành công!", true);
                    txtTitle.setText("");
                    btnSubmit.setEnabled(true);
                    loadReports();
                });
            }

            @Override
            public void onError(String errorMessage) {
                SwingUtilities.invokeLater(() -> {
                    setFeedback(errorMessage, false);
                    btnSubmit.setEnabled(true);
                });
            }
        });
    }

    private void showEditDialog(IncidentReportData report) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Cập nhật báo cáo", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(450, 200);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(20, 20, 10, 20));

        JLabel lbl = new JLabel("Tiêu đề sự cố");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        content.add(lbl);
        content.add(Box.createVerticalStrut(6));

        JTextField txtEdit = new JTextField(report.getTitle());
        txtEdit.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtEdit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txtEdit.setAlignmentX(LEFT_ALIGNMENT);
        content.add(txtEdit);
        content.add(Box.createVerticalStrut(6));

        JLabel lblError = new JLabel(" ");
        lblError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblError.setForeground(ERROR_COLOR);
        lblError.setAlignmentX(LEFT_ALIGNMENT);
        content.add(lblError);

        dialog.add(content, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCancel = new JButton("Huỷ");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancel.setForeground(TEXT_DARK);
        btnCancel.setBackground(new Color(241, 245, 249));
        btnCancel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(5, 16, 5, 16)
        ));
        btnCancel.setFocusPainted(false);
        btnCancel.setOpaque(true);
        btnCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JButton btnSave = new JButton("Cập nhật");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.setBackground(PRIMARY);
        btnSave.setForeground(TEXT_DARK);
        btnSave.setBorder(new EmptyBorder(5, 16, 5, 16));
        btnSave.setFocusPainted(false);
        btnSave.setOpaque(true);
        btnSave.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSave.addActionListener(e -> {
            String newTitle = txtEdit.getText().trim();
            if (newTitle.isEmpty()) {
                lblError.setText("Tiêu đề không được phép bỏ trống");
                return;
            }
            btnSave.setEnabled(false);
            incidentService.updateReportAsync(report.getId(), newTitle, new IncidentReportService.OperationCallback() {
                @Override
                public void onSuccess() {
                    SwingUtilities.invokeLater(() -> {
                        dialog.dispose();
                        loadReports();
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    SwingUtilities.invokeLater(() -> {
                        lblError.setText(errorMessage);
                        btnSave.setEnabled(true);
                    });
                }
            });
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void loadReports() {
        tableModel.setRowCount(0);
        incidentService.fetchMyReportsAsync(new IncidentReportService.FetchCallback() {
            @Override
            public void onSuccess(IncidentReportListResponse response) {
                SwingUtilities.invokeLater(() -> {
                    reports = response.getData() != null && response.getData().getContent() != null
                            ? response.getData().getContent()
                            : new ArrayList<>();
                    tableModel.setRowCount(0);
                    for (int i = 0; i < reports.size(); i++) {
                        IncidentReportData r = reports.get(i);
                        String statusLabel = r.getStatus() >= 0 && r.getStatus() < STATUS_LABELS.length
                                ? STATUS_LABELS[r.getStatus()] : "UNKNOWN";
                        tableModel.addRow(new Object[]{
                            i + 1,
                            r.getTitle(),
                            r.getRoomName() != null ? r.getRoomName() : "-",
                            statusLabel,
                            formatDate(r.getCreatedAt()),
                            ""
                        });
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                // Silent — table stays empty
            }
        });
    }

    private void setFeedback(String message, Boolean success) {
        lblFeedback.setText(message);
        if (success == null) {
            lblFeedback.setForeground(TEXT_MUTED);
        } else if (success) {
            lblFeedback.setForeground(SUCCESS_COLOR);
        } else {
            lblFeedback.setForeground(ERROR_COLOR);
        }
    }

    private String formatDate(String iso) {
        if (iso == null || iso.isEmpty()) return "-";
        try {
            // ISO format: 2024-01-15T10:30:00
            String[] parts = iso.split("T");
            if (parts.length == 2) {
                String[] dateParts = parts[0].split("-");
                String timePart = parts[1].substring(0, Math.min(5, parts[1].length()));
                if (dateParts.length == 3) {
                    return timePart + " " + dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0];
                }
            }
        } catch (Exception ignored) {}
        return iso;
    }
}