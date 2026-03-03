import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  IconButton,
  Typography,
  Box,
  List,
  ListItem,
  ListItemText,
  Card,
  CardContent
} from '@mui/material';

import { useStudentDetail } from '../../hook/useStudent';
import { Camera, CloseCircle, Lock, Monitor } from 'iconsax-reactjs';

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleTimeString('vi-VN', { hour12: false });
};

const StudentDetailModal = ({ open, onClose, studentId }: { open: boolean; onClose: () => void; studentId: number }) => {
  const { student, logs, loading } = useStudentDetail(studentId);

  if (!student) return null;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      {/* Header */}
      <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
        <Box>
          <Typography variant="h6" fontWeight="bold">
            {student.fullName}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {student.username}
          </Typography>
          <Typography variant="caption" display="block" color="text.secondary">
            IP: {student.hostname}
          </Typography>
        </Box>
        <IconButton onClick={onClose}>
          <CloseCircle size="24" color="#292D32" />
        </IconButton>
      </DialogTitle>

      {/* Actions Toolbar */}
      <Box sx={{ px: 3, pb: 1, display: 'flex', gap: 1 }}>
        <IconButton sx={{ bgcolor: '#ffebee', color: '#d32f2f' }}>
          {/* Sử dụng variant="Bold" để icon trông dày dặn hơn */}
          <Lock size="20" color="currentColor" variant="Bold" />
        </IconButton>
        <IconButton sx={{ bgcolor: '#e3f2fd', color: '#0288d1' }}>
          <Camera size="20" color="currentColor" variant="Bold" />
        </IconButton>
      </Box>

      <DialogContent dividers>
        <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
          Danh sách hoạt động ({logs.length})
        </Typography>

        {/* Log List Scrollable */}
        <List sx={{ maxHeight: 400, overflow: 'auto', bgcolor: '#f5f5f5', borderRadius: 1, p: 1 }}>
          {loading && logs.length === 0 ? (
            <Typography sx={{ p: 2 }}>Đang tải dữ liệu...</Typography>
          ) : (
            logs.map((log, index) => (
              <ListItem key={index} sx={{ py: 0.5 }}>
                <ListItemText
                  primary={
                    <Typography variant="body2" sx={{ color: '#0288d1' }}>
                      {formatDate(log.createdAt)}
                    </Typography>
                  }
                  secondary={
                    <Typography variant="body2" color="text.primary">
                      {log.appLog}
                    </Typography>
                  }
                  sx={{ display: 'flex', gap: 2 }}
                />
              </ListItem>
            ))
          )}
        </List>
      </DialogContent>
    </Dialog>
  );
};

export const StudentMonitorDemo = () => {
  const [openModal, setOpenModal] = useState(false);
  const DEMO_ID = 1;

  return (
    <Box p={4}>
      <Card sx={{ width: 300, cursor: 'pointer', border: '1px solid #e0e0e0' }} onClick={() => setOpenModal(true)}>
        <CardContent>
          <Box display="flex" alignItems="center" gap={2} mb={2}>
            <Monitor size="32" color="#1976d2" variant="Bold" />
            <Box>
              <Typography fontWeight="bold">Nguyễn Văn An</Typography>
              <Typography variant="body2" color="text.secondary">
                SV2021001
              </Typography>
            </Box>
          </Box>
          <Typography variant="caption" display="block" gutterBottom>
            IP: 192.168.1.10
          </Typography>

          <Box bgcolor="#f9f9f9" p={1} borderRadius={1} height={100}>
            <Typography variant="caption" color="text.secondary">
              Click để xem chi tiết...
            </Typography>
          </Box>
        </CardContent>
      </Card>

      <StudentDetailModal open={openModal} onClose={() => setOpenModal(false)} studentId={DEMO_ID} />
    </Box>
  );
};
