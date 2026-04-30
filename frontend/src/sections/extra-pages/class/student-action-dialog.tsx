import {
  Alert,
  Avatar,
  Box,
  CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  Snackbar,
  Stack,
  Tooltip,
  Typography
} from '@mui/material';
import { Camera, CloseCircle, Lock1, Monitor, Unlock } from 'iconsax-reactjs';
import { useEffect, useState } from 'react';
import { AppUsageEntry, StudentTrackingState } from 'hooks/useClassTracking';
import { getScreenshot, lockScreen } from 'api/veyon';
import { HttpStatusCode } from 'axios';

function formatTime(isoString: string): string {
  try {
    const date = new Date(isoString);
    return [date.getHours(), date.getMinutes(), date.getSeconds()].map((n) => String(n).padStart(2, '0')).join(':');
  } catch {
    return isoString;
  }
}

function getInitials(fullName: string): string {
  return fullName
    .split(' ')
    .filter(Boolean)
    .slice(-2)
    .map((w) => w[0].toUpperCase())
    .join('');
}

// 422 → show server message directly; others → use fallback
function extractApiErrorMessage(error: unknown, fallback: string): string {
  if (error && typeof error === 'object') {
    const e = error as Record<string, unknown>;
    if (e.statusCode === HttpStatusCode.UnprocessableEntity && typeof e.message === 'string') {
      return e.message;
    }
  }
  return fallback;
}

interface StudentActionDialogProps {
  open: boolean;
  onClose: () => void;
  student: StudentTrackingState;
  classId: number;
  isLocked: boolean;
  onLockChange: (userId: number, locked: boolean) => void;
}

export default function StudentActionDialog({ open, onClose, student, classId, isLocked, onLockChange }: StudentActionDialogProps) {
  const [lockLoading, setLockLoading] = useState(false);
  const [screenshotLoading, setScreenshotLoading] = useState(false);
  const [screenshotData, setScreenshotData] = useState<string | null>(null);
  const [screenshotOpen, setScreenshotOpen] = useState(false);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error'
  });

  useEffect(() => {
    if (!open) {
      setScreenshotData(null);
      setScreenshotOpen(false);
    }
  }, [open, student.studentId]);

  const showSnackbar = (message: string, severity: 'success' | 'error') => setSnackbar({ open: true, message, severity });

  const handleLockToggle = async () => {
    setLockLoading(true);
    try {
      const res = await lockScreen(classId, student.userId, !isLocked);
      if (res.statusCode === HttpStatusCode.Ok) {
        onLockChange(student.userId, !isLocked);
        showSnackbar(!isLocked ? 'Đã khoá màn hình thành công' : 'Đã mở khoá màn hình thành công', 'success');
      }
    } catch (error: unknown) {
      showSnackbar(extractApiErrorMessage(error, 'Thao tác thất bại, vui lòng thử lại'), 'error');
    } finally {
      setLockLoading(false);
    }
  };

  const handleScreenshot = async () => {
    setScreenshotLoading(true);
    try {
      const res = await getScreenshot(classId);
      if (res.statusCode === HttpStatusCode.Ok && res.data) {
        setScreenshotData(res.data);
        setScreenshotOpen(true);
      } else {
        showSnackbar('Không nhận được dữ liệu ảnh', 'error');
      }
    } catch (error: unknown) {
      showSnackbar(extractApiErrorMessage(error, 'Không thể chụp màn hình, vui lòng thử lại'), 'error');
    } finally {
      setScreenshotLoading(false);
    }
  };

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth slotProps={{ paper: { sx: { borderRadius: 3 } } }}>
        <DialogTitle sx={{ pb: 1, pt: 2.5 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Typography variant="h5">Chi tiết sinh viên</Typography>
            <IconButton onClick={onClose} size="small" sx={{ color: 'text.secondary' }}>
              <CloseCircle size={20} />
            </IconButton>
          </Stack>
        </DialogTitle>

        <DialogContent sx={{ pt: 1, pb: 2.5 }}>
          {/* Student Info */}
          <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2.5 }}>
            <Avatar
              sx={{
                width: 60,
                height: 60,
                bgcolor: 'primary.main',
                fontSize: 22,
                fontWeight: 'bold',
                flexShrink: 0
              }}
            >
              {getInitials(student.fullName)}
            </Avatar>
            <Stack spacing={0.3} sx={{ minWidth: 0 }}>
              <Typography variant="h6" fontWeight="bold" noWrap>
                {student.fullName}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {student.code} · {student.manageClassName}
              </Typography>
              {student.email && (
                <Typography variant="caption" color="text.secondary" noWrap>
                  {student.email}
                </Typography>
              )}
              {student.phone && (
                <Typography variant="caption" color="text.secondary">
                  {student.phone}
                </Typography>
              )}
            </Stack>
          </Stack>

          <Divider sx={{ mb: 2.5 }} />

          {/* Action Buttons */}
          <Stack direction="row" spacing={3} justifyContent="center" sx={{ mb: 2.5 }}>
            <Tooltip title={isLocked ? 'Mở khoá màn hình' : 'Khoá màn hình'} arrow>
              <Stack alignItems="center" spacing={0.75}>
                <IconButton
                  onClick={handleLockToggle}
                  disabled={lockLoading}
                  sx={{
                    width: 60,
                    height: 60,
                    border: '1.5px solid',
                    borderColor: isLocked ? 'error.main' : 'divider',
                    borderRadius: 2,
                    color: isLocked ? 'error.main' : 'text.secondary',
                    transition: 'all 0.2s',
                    '&:hover': {
                      bgcolor: isLocked ? 'error.lighter' : 'primary.lighter',
                      borderColor: isLocked ? 'error.dark' : 'primary.main',
                      color: isLocked ? 'error.dark' : 'primary.main',
                      transform: 'scale(1.05)'
                    },
                    '&.Mui-disabled': { borderColor: 'divider', opacity: 0.5 }
                  }}
                >
                  {lockLoading ? <CircularProgress size={22} color="inherit" /> : isLocked ? <Unlock size={26} /> : <Lock1 size={26} />}
                </IconButton>
                <Typography variant="caption" fontWeight="medium" color={isLocked ? 'error.main' : 'text.secondary'}>
                  {isLocked ? 'Mở khoá' : 'Khoá máy'}
                </Typography>
              </Stack>
            </Tooltip>

            <Tooltip title="Chụp màn hình" arrow>
              <Stack alignItems="center" spacing={0.75}>
                <IconButton
                  onClick={handleScreenshot}
                  disabled={screenshotLoading}
                  sx={{
                    width: 60,
                    height: 60,
                    border: '1.5px solid',
                    borderColor: 'divider',
                    borderRadius: 2,
                    color: 'text.secondary',
                    transition: 'all 0.2s',
                    '&:hover': {
                      bgcolor: 'primary.lighter',
                      borderColor: 'primary.main',
                      color: 'primary.main',
                      transform: 'scale(1.05)'
                    },
                    '&.Mui-disabled': { borderColor: 'divider', opacity: 0.5 }
                  }}
                >
                  {screenshotLoading ? <CircularProgress size={22} color="inherit" /> : <Camera size={26} />}
                </IconButton>
                <Typography variant="caption" fontWeight="medium" color="text.secondary">
                  Chụp màn hình
                </Typography>
              </Stack>
            </Tooltip>
          </Stack>

          <Divider sx={{ mb: 2 }} />

          {/* Live App History — stays in sync with WS via parent prop */}
          <Stack spacing={1}>
            <Stack direction="row" spacing={1} alignItems="center">
              <Monitor size={14} />
              <Typography variant="caption" fontWeight="medium" color="text.secondary">
                Ứng dụng đang dùng
              </Typography>
            </Stack>
            <Box
              sx={{
                maxHeight: 200,
                overflowY: 'auto',
                pr: 0.5,
                '&::-webkit-scrollbar': { width: 4 },
                '&::-webkit-scrollbar-track': { bgcolor: 'transparent' },
                '&::-webkit-scrollbar-thumb': { bgcolor: 'divider', borderRadius: 2 }
              }}
            >
              {student.appHistory.length === 0 ? (
                <Typography variant="caption" color="text.disabled" sx={{ fontStyle: 'italic' }}>
                  Chưa có dữ liệu
                </Typography>
              ) : (
                <Stack spacing={0.5}>
                  {student.appHistory.map((entry: AppUsageEntry, idx: number) => (
                    <Stack key={idx} direction="row" spacing={1} alignItems="baseline">
                      <Typography variant="caption" color="text.disabled" sx={{ minWidth: 56, fontFamily: 'monospace', flexShrink: 0 }}>
                        {formatTime(entry.createdAt)}
                      </Typography>
                      <Typography
                        variant="caption"
                        sx={{
                          flex: 1,
                          color: entry.banApplication ? 'error.main' : idx === 0 ? 'primary.main' : 'text.secondary',
                          fontWeight: entry.banApplication ? 'bold' : idx === 0 ? 'medium' : 'normal'
                        }}
                      >
                        {entry.applicationName}
                      </Typography>
                    </Stack>
                  ))}
                </Stack>
              )}
            </Box>
          </Stack>
        </DialogContent>
      </Dialog>

      {/* Screenshot Preview Dialog */}
      <Dialog
        open={screenshotOpen}
        onClose={() => setScreenshotOpen(false)}
        maxWidth="lg"
        slotProps={{ paper: { sx: { borderRadius: 2 } } }}
      >
        <DialogTitle sx={{ py: 1.5 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Typography variant="h6">Màn hình — {student.fullName}</Typography>
            <IconButton onClick={() => setScreenshotOpen(false)} size="small" sx={{ color: 'text.secondary' }}>
              <CloseCircle size={20} />
            </IconButton>
          </Stack>
        </DialogTitle>
        <DialogContent sx={{ p: 1.5, pt: 0 }}>
          {screenshotData && (
            <Box
              component="img"
              src={`data:image/png;base64,${screenshotData}`}
              alt={`Screenshot — ${student.fullName}`}
              sx={{ width: '100%', display: 'block', borderRadius: 1 }}
            />
          )}
        </DialogContent>
      </Dialog>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar((prev) => ({ ...prev, open: false }))}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Alert severity={snackbar.severity} variant="filled" sx={{ width: '100%', borderRadius: 2, fontSize: 14 }}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}
