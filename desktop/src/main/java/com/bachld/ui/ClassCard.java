package com.bachld.ui;

import com.bachld.model.response.ClassData;
import com.bachld.util.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class ClassCard extends JPanel {
    private final ClassData data;
    private final int studyStatus; // 0=upcoming, 1=ongoing, 2=ended

    // Theme Colors
    private static final Color BG_ONGOING = new Color(240, 253, 244);
    private static final Color BORDER_ONGOING = new Color(74, 222, 128);
    private static final Color TEXT_ONGOING = new Color(21, 128, 61);

    private static final Color BG_UPCOMING = Color.WHITE;
    private static final Color BORDER_UPCOMING = new Color(226, 232, 240);
    private static final Color TEXT_UPCOMING = new Color(71, 85, 105);

    private static final Color BG_ENDED = new Color(248, 250, 252);
    private static final Color BORDER_ENDED = new Color(203, 213, 225);

    private static final Color PRIMARY_BLUE = new Color(37, 99, 235);
    private static final Color SLATE_800 = new Color(30, 41, 59);

    public ClassCard(ClassData data) {
        this.data = data;
        this.studyStatus = data.getStudyStatus();
        
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(25, 25, 25, 25));
        setPreferredSize(new Dimension(300, 240));
        
        initComponents();
    }

    private void initComponents() {
        // Top Section: Status Badge & Subject
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        
        // Status Badge
        String badgeText = studyStatus == 1 ? " ● ĐANG DIỄN RA " : studyStatus == 2 ? " ĐÃ KẾT THÚC " : " SẮP TỚI ";
        JLabel badge = new JLabel(badgeText);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setOpaque(true);
        if (studyStatus == 1) {
            badge.setBackground(new Color(34, 197, 94));
            badge.setForeground(Color.WHITE);
        } else if (studyStatus == 2) {
            badge.setBackground(new Color(203, 213, 225));
            badge.setForeground(new Color(71, 85, 105));
        } else {
            badge.setBackground(new Color(241, 245, 249));
            badge.setForeground(new Color(100, 116, 139));
        }
        badge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        top.add(badge, BorderLayout.WEST);
        
        JLabel subject = new JLabel(data.getSubjectName());
        subject.setFont(new Font("Segoe UI", Font.BOLD, 12));
        subject.setForeground(new Color(100, 116, 139)); // Slightly darker for better readability
        top.add(subject, BorderLayout.EAST);
        
        add(top, BorderLayout.NORTH);
        
        // Center: Class Name & Details
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(15, 0, 0, 0));
        
        JLabel name = new JLabel("<html><body style='width: 200px'>" + data.getName() + "</body></html>");
        name.setFont(new Font("Segoe UI", Font.BOLD, 18));
        name.setForeground(SLATE_800);
        name.setAlignmentX(LEFT_ALIGNMENT);
        center.add(name);
        center.add(Box.createVerticalStrut(10));
        
        center.add(iconLabel("<html><b>Giảng viên:</b> " + data.getTeacherName() + "</html>", 13, new Color(100, 116, 139)));
        center.add(Box.createVerticalStrut(8));
        center.add(iconLabel("<html><b>Lịch học:</b> " + data.getScheduleName() + "</html>", 12, PRIMARY_BLUE));
        center.add(Box.createVerticalStrut(15));
        
        // Date Grid
        JPanel dates = new JPanel(new GridLayout(1, 2, 10, 0));
        dates.setOpaque(false);
        dates.add(dateBox("Ngày bắt đầu", Util.formatDate(data.getStartDate())));
        dates.add(dateBox("Ngày kết thúc", Util.formatDate(data.getEndDate())));
        dates.setAlignmentX(LEFT_ALIGNMENT);
        center.add(dates);
        
        add(center, BorderLayout.CENTER);
        
        // Bottom: Student Count
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(15, 0, 0, 0));
        
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(241, 245, 249));
        bottom.add(sep, BorderLayout.NORTH);
        
        JLabel students = new JLabel("Sĩ số: " + data.getCurrentStudent() + "/" + data.getMaxStudent());
        students.setFont(new Font("Segoe UI", Font.BOLD, 13));
        students.setForeground(studyStatus == 1 ? TEXT_ONGOING : studyStatus == 2 ? TEXT_UPCOMING : PRIMARY_BLUE);
        bottom.add(students, BorderLayout.WEST);
        
        int percent = (int) ((double) data.getCurrentStudent() / data.getMaxStudent() * 100);
        JLabel prg = new JLabel(percent + "%");
        prg.setFont(new Font("Segoe UI", Font.BOLD, 13));
        prg.setForeground(new Color(148, 163, 184));
        bottom.add(prg, BorderLayout.EAST);
        
        add(bottom, BorderLayout.SOUTH);
    }
    
    private JLabel iconLabel(String text, int size, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI Semibold", Font.PLAIN, size));
        lbl.setForeground(color);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }
    
    private JPanel dateBox(String label, String value) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(new Color(148, 163, 184));
        p.add(lbl);
        
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 12));
        val.setForeground(SLATE_800);
        p.add(val);
        
        return p;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (studyStatus == 1) {
            g2.setColor(new Color(34, 197, 94, 20));
            g2.fill(new RoundRectangle2D.Float(2, 2, getWidth() - 4, getHeight() - 4, 25, 25));
        } else {
            g2.setColor(new Color(0, 0, 0, 15));
            g2.fill(new RoundRectangle2D.Float(3, 3, getWidth() - 6, getHeight() - 4, 25, 25));
        }

        Color bg = studyStatus == 1 ? BG_ONGOING : studyStatus == 2 ? BG_ENDED : BG_UPCOMING;
        Color border = studyStatus == 1 ? BORDER_ONGOING : studyStatus == 2 ? BORDER_ENDED : BORDER_UPCOMING;

        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 4, getHeight() - 4, 20, 20));

        g2.setColor(border);
        g2.setStroke(new BasicStroke(studyStatus == 1 ? 2f : 1f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 5, getHeight() - 5, 20, 20));

        if (studyStatus == 1) {
            g2.setColor(new Color(34, 197, 94));
            g2.fill(new RoundRectangle2D.Float(0, 20, 4, getHeight() - 44, 2, 2));
        }
        
        g2.dispose();
    }
}
