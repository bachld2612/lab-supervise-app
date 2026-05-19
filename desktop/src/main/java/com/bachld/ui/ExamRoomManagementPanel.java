package com.bachld.ui;

import com.bachld.model.response.ExamRoomData;
import com.bachld.model.response.ExamRoomListResponse;
import com.bachld.service.ExamRoomService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class ExamRoomManagementPanel extends JPanel {
    private final ExamRoomService examRoomService;
    private JPanel gridContainer;
    private JScrollPane scrollPane;
    private JPanel loadingPanel;
    private JPanel errorPanel;
    private JLabel lblError;

    public ExamRoomManagementPanel(ExamRoomService examRoomService) {
        this.examRoomService = examRoomService;
        setLayout(new BorderLayout());
        setOpaque(false);

        initUI();

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                loadData();
            }
        });
    }

    private void initUI() {
        loadingPanel = new JPanel(new GridBagLayout());
        loadingPanel.setOpaque(false);
        loadingPanel.add(new JLabel("Đang tải lịch thi..."), new GridBagConstraints());

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

        gridContainer = new JPanel();
        gridContainer.setLayout(new GridLayout(0, 3, 25, 25));
        gridContainer.setOpaque(false);
        gridContainer.setBorder(new EmptyBorder(20, 0, 40, 0));

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
        examRoomService.fetchMyExamRoomsAsync(new ExamRoomService.FetchCallback() {
            @Override
            public void onSuccess(ExamRoomListResponse response) {
                renderExamRooms(response.getData());
                showComponent(scrollPane);
            }

            @Override
            public void onError(String errorMessage) {
                lblError.setText(errorMessage);
                showComponent(errorPanel);
            }
        });
    }

    private void renderExamRooms(List<ExamRoomData> examRooms) {
        gridContainer.removeAll();
        if (examRooms == null || examRooms.isEmpty()) {
            gridContainer.setLayout(new FlowLayout(FlowLayout.CENTER));
            gridContainer.add(new JLabel("Bạn chưa có lịch thi nào."));
        } else {
            gridContainer.setLayout(new GridLayout(0, 3, 25, 25));
            // sort: ongoing (1) → upcoming (0) → ended (2)
            examRooms.sort(Comparator.comparingInt(d -> {
                int s = ExamRoomCard.computeStatus(d);
                return s == 1 ? 0 : s == 0 ? 1 : 2;
            }));
            for (ExamRoomData data : examRooms) {
                gridContainer.add(new ExamRoomCard(data));
            }

            int mod = examRooms.size() % 3;
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
