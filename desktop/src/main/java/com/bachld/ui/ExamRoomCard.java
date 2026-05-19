package com.bachld.ui;

import com.bachld.model.response.ExamRoomData;
import com.bachld.util.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ExamRoomCard extends JPanel {
    private final ExamRoomData data;
    private final int examStatus; // 0=upcoming, 1=ongoing, 2=past

    private static final Color BG_ONGOING = new Color(240, 253, 244);
    private static final Color BORDER_ONGOING = new Color(74, 222, 128);

    private static final Color BG_UPCOMING = Color.WHITE;
    private static final Color BORDER_UPCOMING = new Color(226, 232, 240);

    private static final Color BG_PAST = new Color(248, 250, 252);
    private static final Color BORDER_PAST = new Color(203, 213, 225);

    private static final Color PRIMARY_BLUE = new Color(37, 99, 235);
    private static final Color SLATE_800 = new Color(30, 41, 59);

    public ExamRoomCard(ExamRoomData data) {
        this.data = data;
        this.examStatus = computeStatus(data);

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(25, 25, 25, 25));
        setPreferredSize(new Dimension(300, 260));

        initComponents();
    }

    public static int computeStatus(ExamRoomData data) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate examDate = LocalDate.parse(data.getExamDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            if (examDate.isBefore(today)) return 2;
            if (examDate.isAfter(today)) return 0;
            // Same day — check time
            LocalTime now = LocalTime.now();
            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm[:ss]");
            LocalTime start = LocalTime.parse(data.getStartTime(), timeFmt);
            LocalTime end = LocalTime.parse(data.getEndTime(), timeFmt);
            if (!now.isBefore(start) && !now.isAfter(end)) return 1;
            if (now.isAfter(end)) return 2;
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void initComponents() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        String badgeText = examStatus == 1 ? " ● ĐANG DIỄN RA " : examStatus == 2 ? " ĐÃ KẾT THÚC " : " SẮP TỚI ";
        JLabel badge = new JLabel(badgeText);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setOpaque(true);
        if (examStatus == 1) {
            badge.setBackground(new Color(34, 197, 94));
            badge.setForeground(Color.WHITE);
        } else if (examStatus == 2) {
            badge.setBackground(new Color(203, 213, 225));
            badge.setForeground(new Color(71, 85, 105));
        } else {
            badge.setBackground(new Color(241, 245, 249));
            badge.setForeground(new Color(100, 116, 139));
        }
        badge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        top.add(badge, BorderLayout.WEST);

        JLabel subject = new JLabel(data.getSubjectName() != null ? data.getSubjectName() : "");
        subject.setFont(new Font("Segoe UI", Font.BOLD, 12));
        subject.setForeground(new Color(100, 116, 139));
        top.add(subject, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(12, 0, 0, 0));

        JLabel code = new JLabel("<html><body style='width: 200px'>" + (data.getCode() != null ? data.getCode() : "") + "</body></html>");
        code.setFont(new Font("Segoe UI", Font.BOLD, 18));
        code.setForeground(SLATE_800);
        code.setAlignmentX(LEFT_ALIGNMENT);
        center.add(code);
        center.add(Box.createVerticalStrut(8));

        center.add(infoLabel("<html><b>Phòng:</b> " + (data.getRoomName() != null ? data.getRoomName() : "") + "</html>", 13, new Color(100, 116, 139)));
        center.add(Box.createVerticalStrut(6));
        center.add(infoLabel("<html><b>Ngày thi:</b> " + Util.formatDate(data.getExamDate()) + "</html>", 12, PRIMARY_BLUE));
        center.add(Box.createVerticalStrut(4));
        center.add(infoLabel("<html><b>Giờ thi:</b> " + (data.getStartTime() != null ? data.getStartTime() : "") + " – " + (data.getEndTime() != null ? data.getEndTime() : "") + "</html>", 12, PRIMARY_BLUE));
        center.add(Box.createVerticalStrut(12));

        JPanel dates = new JPanel(new GridLayout(1, 2, 10, 0));
        dates.setOpaque(false);
        dates.add(infoBox("GV coi thi 1", data.getTeacher1Name() != null ? data.getTeacher1Name() : ""));
        dates.add(infoBox("GV coi thi 2", data.getTeacher2Name() != null ? data.getTeacher2Name() : ""));
        dates.setAlignmentX(LEFT_ALIGNMENT);
        center.add(dates);

        add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(12, 0, 0, 0));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(241, 245, 249));
        bottom.add(sep, BorderLayout.NORTH);

        JLabel students = new JLabel("Sĩ số: " + data.getCurrentStudent() + "/" + data.getMaxStudent());
        students.setFont(new Font("Segoe UI", Font.BOLD, 13));
        students.setForeground(examStatus == 1 ? new Color(21, 128, 61) : PRIMARY_BLUE);
        bottom.add(students, BorderLayout.WEST);

        add(bottom, BorderLayout.SOUTH);
    }

    private JLabel infoLabel(String text, int size, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI Semibold", Font.PLAIN, size));
        lbl.setForeground(color);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel infoBox(String label, String value) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(new Color(148, 163, 184));
        p.add(lbl);

        JLabel val = new JLabel("<html><body style='width: 100px'>" + value + "</body></html>");
        val.setFont(new Font("Segoe UI", Font.BOLD, 11));
        val.setForeground(SLATE_800);
        p.add(val);

        return p;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bg = examStatus == 1 ? BG_ONGOING : examStatus == 2 ? BG_PAST : BG_UPCOMING;
        Color border = examStatus == 1 ? BORDER_ONGOING : examStatus == 2 ? BORDER_PAST : BORDER_UPCOMING;

        if (examStatus == 1) {
            g2.setColor(new Color(34, 197, 94, 20));
            g2.fill(new RoundRectangle2D.Float(2, 2, getWidth() - 4, getHeight() - 4, 25, 25));
        } else {
            g2.setColor(new Color(0, 0, 0, 15));
            g2.fill(new RoundRectangle2D.Float(3, 3, getWidth() - 6, getHeight() - 4, 25, 25));
        }

        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 4, getHeight() - 4, 20, 20));

        g2.setColor(border);
        g2.setStroke(new BasicStroke(examStatus == 1 ? 2f : 1f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 5, getHeight() - 5, 20, 20));

        if (examStatus == 1) {
            g2.setColor(new Color(34, 197, 94));
            g2.fill(new RoundRectangle2D.Float(0, 20, 4, getHeight() - 44, 2, 2));
        }

        g2.dispose();
    }
}
