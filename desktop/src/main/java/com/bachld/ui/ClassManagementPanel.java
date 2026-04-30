package com.bachld.ui;

import com.bachld.model.response.ClassData;
import com.bachld.model.response.ClassListResponse;
import com.bachld.service.ClassService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class ClassManagementPanel extends JPanel {
    private final ClassService classService;
    private JPanel gridContainer;
    private JScrollPane scrollPane;
    private JPanel loadingPanel;
    private JPanel errorPanel;
    private JLabel lblError;

    public ClassManagementPanel(ClassService classService) {
        this.classService = classService;
        setLayout(new BorderLayout());
        setOpaque(false);

        initUI();

        // HierarchyListener fires when any ancestor changes visibility (e.g. CardLayout wrapper).
        // componentShown would not work here because ClassManagementPanel is wrapped inside
        // wrapInPageWrapper() — CardLayout shows/hides the wrapper, not this panel directly.
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                loadData();
            }
        });
    }

    private void initUI() {
        // Header (handled by MainFrame typically, but we can add breadcrumbs here if needed)
        // For now, focus on the grid
        
        // Loading Panel
        loadingPanel = new JPanel(new GridBagLayout());
        loadingPanel.setOpaque(false);
        loadingPanel.add(new JLabel("Đang tải danh sách lớp học..."), new GridBagConstraints());
        
        // Error Panel
        errorPanel = new JPanel(new GridBagLayout());
        errorPanel.setOpaque(false);
        JPanel errorContent = new JPanel();
        errorContent.setLayout(new BoxLayout(errorContent, BoxLayout.Y_AXIS));
        errorContent.setOpaque(false);
        
        lblError = new JLabel("Đã xảy ra lỗi khi tải dữ liệu.");
        lblError.setForeground(Color.RED);
        lblError.setAlignmentX(CENTER_ALIGNMENT);
        errorContent.add(lblError);
        
        JButton btnRetry = new JButton("Thử lại");
        btnRetry.setAlignmentX(CENTER_ALIGNMENT);
        btnRetry.addActionListener(e -> loadData());
        errorContent.add(Box.createVerticalStrut(10));
        errorContent.add(btnRetry);
        
        errorPanel.add(errorContent, new GridBagConstraints());
        
        // Grid Container
        gridContainer = new JPanel();
        gridContainer.setLayout(new GridLayout(0, 3, 25, 25)); // 0 rows (dynamic), 3 columns, hgp=25, vgap=25
        gridContainer.setOpaque(false);
        gridContainer.setBorder(new EmptyBorder(20, 0, 40, 0));
        
        // Wrapper for grid to prevent stretching
        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.setOpaque(false);
        gridWrapper.add(gridContainer, BorderLayout.NORTH);
        
        scrollPane = new JScrollPane(gridWrapper);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        add(loadingPanel, BorderLayout.CENTER);
    }

    private void loadData() {
        showComponent(loadingPanel);
        classService.fetchMyClassesAsync(new ClassService.FetchCallback() {
            @Override
            public void onSuccess(ClassListResponse response) {
                renderClasses(response.getData());
                showComponent(scrollPane);
            }

            @Override
            public void onError(String errorMessage) {
                lblError.setText(errorMessage);
                showComponent(errorPanel);
            }
        });
    }

    private void renderClasses(List<ClassData> classes) {
        gridContainer.removeAll();
        if (classes == null || classes.isEmpty()) {
            gridContainer.setLayout(new FlowLayout(FlowLayout.CENTER));
            gridContainer.add(new JLabel("Bạn chưa có lớp học nào."));
        } else {
            // Force 3 columns again just in case
            gridContainer.setLayout(new GridLayout(0, 3, 25, 25));
            for (ClassData data : classes) {
                gridContainer.add(new ClassCard(data));
            }
            
            // Add placeholders for alignment if less than 3 cards
            int mod = classes.size() % 3;
            if (mod != 0) {
                int fillers = 3 - mod;
                for (int i = 0; i < fillers; i++) {
                    JPanel p = new JPanel();
                    p.setOpaque(false);
                    gridContainer.add(p);
                }
            }
        }
        gridContainer.revalidate();
        gridContainer.repaint();
    }

    private void showComponent(Component c) {
        removeAll();
        add(c, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}
