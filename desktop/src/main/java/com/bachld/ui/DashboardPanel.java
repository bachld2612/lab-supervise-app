package com.bachld.ui;

import com.bachld.model.response.ClassData;
import com.bachld.model.response.ClassListResponse;
import com.bachld.model.response.ExamRoomData;
import com.bachld.model.response.ExamRoomListResponse;
import com.bachld.model.response.SemesterData;
import com.bachld.service.ClassService;
import com.bachld.service.ExamRoomService;
import com.bachld.service.SemesterService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DashboardPanel extends JPanel {

    // ── Colors ─────────────────────────────────────────────────────────────
    private static final Color PRIMARY    = new Color(85, 120, 235);
    private static final Color TEXT_DARK  = new Color(30, 41, 59);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color BORDER_CLR = new Color(226, 232, 240);

    // ── Services ────────────────────────────────────────────────────────────
    private final ClassService    classService;
    private final ExamRoomService examRoomService;
    private final SemesterService semesterService;

    // ── Raw data ─────────────────────────────────────────────────────────────
    private List<ClassData>    allClasses   = new ArrayList<>();
    private List<ExamRoomData> allExamRooms = new ArrayList<>();
    private List<SemesterItem> semesters    = new ArrayList<>();

    // ── Exam filter state ────────────────────────────────────────────────────
    private String       examKeyword      = "";
    private SemesterItem examSemester     = null;
    private int          examVisibleCount = 3;

    // ── Class filter state ───────────────────────────────────────────────────
    private String       classKeyword      = "";
    private SemesterItem classSemester     = null;
    private int          classVisibleCount = 3;

    // ── Exam UI ──────────────────────────────────────────────────────────────
    private JTextField              examSearchField;
    private JComboBox<SemesterItem> examCombo;
    private JLabel                  examCountLbl;
    private JPanel                  examGrid;
    private JButton                 examToggleBtn;

    // ── Class UI ─────────────────────────────────────────────────────────────
    private JTextField              classSearchField;
    private JComboBox<SemesterItem> classCombo;
    private JLabel                  classCountLbl;
    private JPanel                  classGrid;
    private JButton                 classToggleBtn;

    // ── State panels ─────────────────────────────────────────────────────────
    private JPanel      loadingPanel;
    private JPanel      errorPanel;
    private JLabel      lblError;
    private JScrollPane scrollPane;
    private JPanel      contentPanel;

    public DashboardPanel(ClassService classService, ExamRoomService examRoomService, SemesterService semesterService) {
        this.classService    = classService;
        this.examRoomService = examRoomService;
        this.semesterService = semesterService;
        setLayout(new BorderLayout());
        setOpaque(false);
        buildUI();
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing())
                loadData();
        });
    }

    // ── UI builder ────────────────────────────────────────────────────────────

    private void buildUI() {
        // Loading
        loadingPanel = centeredPanel(styledLabel("Đang tải dữ liệu...", 14, TEXT_MUTED, Font.PLAIN));

        // Error
        JPanel errBox = vbox();
        lblError = styledLabel("Đã xảy ra lỗi.", 13, new Color(220, 38, 38), Font.PLAIN);
        lblError.setAlignmentX(CENTER_ALIGNMENT);
        JButton retryBtn = new JButton("Thử lại");
        retryBtn.setAlignmentX(CENTER_ALIGNMENT);
        retryBtn.addActionListener(e -> loadData());
        errBox.add(lblError);
        errBox.add(Box.createVerticalStrut(10));
        errBox.add(retryBtn);
        errorPanel = centeredPanel(errBox);

        // Scrollable content
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(8, 0, 40, 0));
        contentPanel.add(buildExamSection());
        contentPanel.add(Box.createVerticalStrut(36));
        contentPanel.add(buildClassSection());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(contentPanel, BorderLayout.NORTH);

        scrollPane = new JScrollPane(wrapper);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        showComp(loadingPanel);
    }

    private JPanel buildExamSection() {
        JPanel section = vbox();
        section.setAlignmentX(LEFT_ALIGNMENT);

        // Header row
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JPanel left = hbox();
        JLabel title = styledLabel("📅  Lịch thi", 19, TEXT_DARK, Font.BOLD);
        examCountLbl = styledLabel("  0 phòng", 13, TEXT_MUTED, Font.PLAIN);
        left.add(title);
        left.add(examCountLbl);

        examSearchField = buildSearchField("Tìm mã phòng, môn thi...");
        examSearchField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    examKeyword = examSearchField.getText().trim().toLowerCase();
                    examVisibleCount = 3;
                    renderExam();
                }
            }
        });

        examCombo = buildCombo();
        examCombo.addActionListener(e -> {
            Object sel = examCombo.getSelectedItem();
            if (sel instanceof SemesterItem) {
                SemesterItem s = (SemesterItem) sel;
                examSemester = s.isAll() ? null : s;
                examVisibleCount = 3;
                renderExam();
            }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);
        right.add(examSearchField);
        right.add(examCombo);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        section.add(header);
        section.add(Box.createVerticalStrut(14));

        examGrid = new JPanel(new GridLayout(0, 3, 25, 25));
        examGrid.setOpaque(false);
        examGrid.setAlignmentX(LEFT_ALIGNMENT);
        section.add(examGrid);
        section.add(Box.createVerticalStrut(10));

        examToggleBtn = buildToggleBtn(
            () -> { examVisibleCount += 3; renderExam(); },
            () -> { examVisibleCount = 3;  renderExam(); }
        );
        JPanel examBtnRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        examBtnRow.setOpaque(false);
        examBtnRow.setAlignmentX(LEFT_ALIGNMENT);
        examBtnRow.add(examToggleBtn);
        examToggleBtn.setVisible(false);
        section.add(examBtnRow);

        return section;
    }

    private JPanel buildClassSection() {
        JPanel section = vbox();
        section.setAlignmentX(LEFT_ALIGNMENT);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JPanel left = hbox();
        JLabel title = styledLabel("📚  Lớp học", 19, TEXT_DARK, Font.BOLD);
        classCountLbl = styledLabel("  0 lớp", 13, TEXT_MUTED, Font.PLAIN);
        left.add(title);
        left.add(classCountLbl);

        classSearchField = buildSearchField("Tìm tên lớp, môn học...");
        classSearchField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    classKeyword = classSearchField.getText().trim().toLowerCase();
                    classVisibleCount = 3;
                    renderClass();
                }
            }
        });

        classCombo = buildCombo();
        classCombo.addActionListener(e -> {
            Object sel = classCombo.getSelectedItem();
            if (sel instanceof SemesterItem) {
                SemesterItem s = (SemesterItem) sel;
                classSemester = s.isAll() ? null : s;
                classVisibleCount = 3;
                renderClass();
            }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);
        right.add(classSearchField);
        right.add(classCombo);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        section.add(header);
        section.add(Box.createVerticalStrut(14));

        classGrid = new JPanel(new GridLayout(0, 3, 25, 25));
        classGrid.setOpaque(false);
        classGrid.setAlignmentX(LEFT_ALIGNMENT);
        section.add(classGrid);
        section.add(Box.createVerticalStrut(10));

        classToggleBtn = buildToggleBtn(
            () -> { classVisibleCount += 3; renderClass(); },
            () -> { classVisibleCount = 3;  renderClass(); }
        );
        JPanel classBtnRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        classBtnRow.setOpaque(false);
        classBtnRow.setAlignmentX(LEFT_ALIGNMENT);
        classBtnRow.add(classToggleBtn);
        classToggleBtn.setVisible(false);
        section.add(classBtnRow);

        return section;
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadData() {
        showComp(loadingPanel);
        allClasses   = new ArrayList<>();
        allExamRooms = new ArrayList<>();
        semesters    = new ArrayList<>();
        AtomicInteger pending = new AtomicInteger(3);

        semesterService.fetchSemestersAsync(new SemesterService.FetchCallback() {
            @Override public void onSuccess(List<SemesterData> result) {
                semesters = result.stream()
                        .map(s -> new SemesterItem(s.getId().longValue(), s.getName()))
                        .collect(Collectors.toList());
                if (pending.decrementAndGet() == 0) onAllLoaded();
            }
            @Override public void onError(String msg) {
                if (pending.decrementAndGet() == 0) onAllLoaded();
            }
        });

        classService.fetchMyClassesAsync(new ClassService.FetchCallback() {
            @Override public void onSuccess(ClassListResponse r) {
                allClasses = r.getData() != null ? r.getData() : new ArrayList<>();
                if (pending.decrementAndGet() == 0) onAllLoaded();
            }
            @Override public void onError(String msg) {
                lblError.setText(msg);
                showComp(errorPanel);
            }
        });

        examRoomService.fetchMyExamRoomsAsync(new ExamRoomService.FetchCallback() {
            @Override public void onSuccess(ExamRoomListResponse r) {
                allExamRooms = r.getData() != null ? r.getData() : new ArrayList<>();
                if (pending.decrementAndGet() == 0) onAllLoaded();
            }
            @Override public void onError(String msg) {
                lblError.setText(msg);
                showComp(errorPanel);
            }
        });
    }

    private void onAllLoaded() {
        buildSemesters();
        examKeyword  = "";
        classKeyword = "";
        examVisibleCount  = 3;
        classVisibleCount = 3;
        examSearchField.setText("");
        classSearchField.setText("");
        renderExam();
        renderClass();
        showComp(scrollPane);
    }

    // ── Semester builder ──────────────────────────────────────────────────────

    private void buildSemesters() {
        // If the API returned nothing, fall back to deriving semesters from loaded data
        if (semesters.isEmpty()) {
            LinkedHashMap<Long, String> map = new LinkedHashMap<>();
            allClasses.forEach(c -> { if (c.getSemesterId() != null) map.put(c.getSemesterId(), c.getSemesterName()); });
            allExamRooms.forEach(e -> { if (e.getSemesterId() != null) map.put(e.getSemesterId(), e.getSemesterName()); });
            semesters = map.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getKey(), a.getKey()))
                    .map(en -> new SemesterItem(en.getKey(), en.getValue()))
                    .collect(Collectors.toList());
        }

        SemesterItem nearest = semesters.isEmpty() ? null : semesters.get(0);
        examSemester  = nearest;
        classSemester = nearest;

        boolean hasSemesters = !semesters.isEmpty();
        examCombo.setVisible(hasSemesters);
        classCombo.setVisible(hasSemesters);

        fillCombo(examCombo,  nearest);
        fillCombo(classCombo, nearest);
    }

    private void fillCombo(JComboBox<SemesterItem> combo, SemesterItem defaultSel) {
        ActionListener[] listeners = combo.getActionListeners();
        for (ActionListener l : listeners) combo.removeActionListener(l);

        combo.removeAllItems();
        combo.addItem(new SemesterItem(-1L, "Tất cả"));
        semesters.forEach(combo::addItem);

        if (defaultSel != null) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (Objects.equals(combo.getItemAt(i).getId(), defaultSel.getId())) {
                    combo.setSelectedIndex(i);
                    break;
                }
            }
        }

        for (ActionListener l : listeners) combo.addActionListener(l);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void renderExam() {
        List<ExamRoomData> filtered = filterExams();
        int total   = filtered.size();
        int visible = Math.min(examVisibleCount, total);
        examCountLbl.setText("  " + total + " phòng");
        fillGrid(examGrid, visible, total, i -> new ExamRoomCard(filtered.get(i)));
        updateToggle(examToggleBtn, visible, total, "phòng thi");
    }

    private void renderClass() {
        List<ClassData> filtered = filterClasses();
        int total   = filtered.size();
        int visible = Math.min(classVisibleCount, total);
        classCountLbl.setText("  " + total + " lớp");
        fillGrid(classGrid, visible, total, i -> new ClassCard(filtered.get(i)));
        updateToggle(classToggleBtn, visible, total, "lớp học");
    }

    private List<ExamRoomData> filterExams() {
        List<ExamRoomData> list = new ArrayList<>(allExamRooms);
        if (examSemester != null)
            list = list.stream().filter(e -> examSemester.getId().equals(e.getSemesterId())).collect(Collectors.toList());
        if (!examKeyword.isEmpty())
            list = list.stream().filter(e ->
                contains(e.getCode(), examKeyword) || contains(e.getSubjectName(), examKeyword)
            ).collect(Collectors.toList());
        list.sort(Comparator.comparingInt(e -> statusSortKey(ExamRoomCard.computeStatus(e))));
        return list;
    }

    private List<ClassData> filterClasses() {
        List<ClassData> list = new ArrayList<>(allClasses);
        if (classSemester != null)
            list = list.stream().filter(c -> classSemester.getId().equals(c.getSemesterId())).collect(Collectors.toList());
        if (!classKeyword.isEmpty())
            list = list.stream().filter(c ->
                contains(c.getName(), classKeyword) || contains(c.getSubjectName(), classKeyword)
            ).collect(Collectors.toList());
        list.sort(Comparator.comparingInt(c -> statusSortKey(c.getStudyStatus())));
        return list;
    }

    private interface CardFactory { JPanel make(int index); }

    private void fillGrid(JPanel grid, int visible, int total, CardFactory factory) {
        grid.removeAll();
        if (total == 0) {
            grid.setLayout(new FlowLayout(FlowLayout.CENTER));
            grid.add(styledLabel("Không có dữ liệu", 13, TEXT_MUTED, Font.PLAIN));
        } else {
            grid.setLayout(new GridLayout(0, 3, 25, 25));
            for (int i = 0; i < visible; i++) grid.add(factory.make(i));
            int mod = visible % 3;
            if (mod != 0) {
                for (int i = 0; i < 3 - mod; i++) {
                    JPanel f = new JPanel(); f.setOpaque(false); grid.add(f);
                }
            }
        }
        grid.revalidate();
        grid.repaint();
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void updateToggle(JButton btn, int visible, int total, String unit) {
        if (visible < total) {
            btn.setText("Xem thêm " + Math.min(3, total - visible) + " " + unit);
            btn.putClientProperty("mode", "expand");
            btn.setVisible(true);
        } else if (total > 3) {
            btn.setText("Ẩn bớt");
            btn.putClientProperty("mode", "collapse");
            btn.setVisible(true);
        } else {
            btn.setVisible(false);
        }
    }

    // ── Widget factories ──────────────────────────────────────────────────────

    private JTextField buildSearchField(String hint) {
        JTextField f = new JTextField(16) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(TEXT_MUTED);
                    g2.setFont(getFont());
                    Insets ins = getInsets();
                    FontMetrics fm = g2.getFontMetrics();
                    int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(hint, ins.left, y);
                    g2.dispose();
                }
            }
        };
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setPreferredSize(new Dimension(200, 28));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR, 1),
                new EmptyBorder(3, 8, 3, 8)));
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { f.repaint(); }
            @Override public void focusLost(FocusEvent e) { f.repaint(); }
        });
        return f;
    }

    private JComboBox<SemesterItem> buildCombo() {
        JComboBox<SemesterItem> combo = new JComboBox<>();
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setPreferredSize(new Dimension(200, 28));
        return combo;
    }

    private JButton buildToggleBtn(Runnable onExpand, Runnable onCollapse) {
        JButton btn = new JButton();
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(PRIMARY);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY, 1, true),
                new EmptyBorder(5, 18, 5, 18)));
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if ("expand".equals(btn.getClientProperty("mode"))) onExpand.run();
            else onCollapse.run();
        });
        return btn;
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private JPanel vbox() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        return p;
    }

    private JPanel hbox() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        return p;
    }

    private JPanel centeredPanel(Component content) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.add(content, new GridBagConstraints());
        return p;
    }

    private JLabel styledLabel(String text, int size, Color color, int style) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", style, size));
        lbl.setForeground(color);
        return lbl;
    }

    private boolean contains(String str, String kw) {
        return str != null && str.toLowerCase().contains(kw);
    }

    private int statusSortKey(int status) {
        return status == 1 ? 0 : status == 0 ? 1 : 2;
    }

    private void showComp(Component c) {
        removeAll();
        add(c, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // ── SemesterItem ──────────────────────────────────────────────────────────

    static class SemesterItem {
        private final Long   id;
        private final String name;

        SemesterItem(Long id, String name) {
            this.id   = id;
            this.name = name;
        }

        public Long getId()    { return id; }
        public boolean isAll() { return id < 0; }

        @Override
        public String toString() { return name; }
    }
}
