import { useEffect, useState } from 'react';
import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, Stack, Typography } from '@mui/material';
import { useNavigate } from 'react-router';
import { getMyPC } from 'api/personal-computer';

export const TEACHER_IP_NOTICE_KEY = 'teacher_ip_notice_shown';

// ==============================|| TEACHER IP NOTICE DIALOG ||============================== //

export default function TeacherIPNoticeDialog() {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [currentIP, setCurrentIP] = useState<string | null>(null);

  useEffect(() => {
    if (localStorage.getItem(TEACHER_IP_NOTICE_KEY)) return;

    const fetchIP = async () => {
      try {
        const res = await getMyPC();
        setCurrentIP(res.data?.ipAddress ?? null);
        setOpen(true);
      } catch {
        // silently ignore — do not block the dashboard on error
      } finally {
        setLoading(false);
      }
    };

    fetchIP();
  }, []);

  const handleConfirm = () => {
    localStorage.setItem(TEACHER_IP_NOTICE_KEY, '1');
    setOpen(false);
    navigate('/teacher/personal-computer');
  };

  const handleDismiss = () => {
    localStorage.setItem(TEACHER_IP_NOTICE_KEY, '1');
    setOpen(false);
  };

  if (loading || !open) return null;

  const hasIP = currentIP !== null;

  return (
    <Dialog open={open} maxWidth="xs" fullWidth slotProps={{ paper: { sx: { borderRadius: 3 } } }}>
      <DialogTitle sx={{ pb: 1 }}>
        <Typography variant="h5" fontWeight="bold">
          Thông tin máy tính
        </Typography>
      </DialogTitle>

      <DialogContent>
        {hasIP ? (
          <Stack spacing={1}>
            <DialogContentText>
              IP hiện tại của bạn là: <strong>{currentIP}</strong>
            </DialogContentText>
            <DialogContentText>Bạn có muốn cập nhật không?</DialogContentText>
          </Stack>
        ) : (
          <DialogContentText>Bạn chưa cài đặt thông tin IP, vui lòng cập nhật thông tin ngay.</DialogContentText>
        )}
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2.5, gap: 1 }}>
        <Button variant="contained" onClick={handleConfirm}>
          Đồng ý
        </Button>
        {hasIP && (
          <Button variant="outlined" color="inherit" onClick={handleDismiss}>
            Không
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
}
