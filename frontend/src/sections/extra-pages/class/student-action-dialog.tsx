import {
  Alert,
  Avatar,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  Snackbar,
  Stack,
  TextField,
  Tooltip,
  Typography
} from '@mui/material';
import { Camera, CloseCircle, DocumentUpload, Global, Lock1, MessageText, Monitor, Unlock, Wifi } from 'iconsax-reactjs';
import { useEffect, useRef, useState } from 'react';
import { AppUsageEntry, StudentTrackingState } from 'hooks/useClassTracking';
import {
  lockScreen,
  openWebsiteForStudent,
  sendMessageToStudent,
  lockScreenForExamRoom,
  openWebsiteForExamRoomStudent,
  sendMessageToExamRoomStudent
} from 'api/remote-control';
import { requestClassScreenshot, requestExamRoomScreenshot } from 'api/screenshot';
import { sendFileToStudent } from 'api/class';
import { ChangeEvent } from 'react';
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
  isOnline: boolean;
  onLockChange: (userId: number, locked: boolean) => void;
  isExamRoom?: boolean;
  examRoomId?: number;
  isActive?: boolean;
  onScreenshotRequested?: (screenshotId: number) => void;
  readyScreenshot?: {
    screenshotId: number;
    studentId: number;
    studentUserId: number;
    imageUrl?: string;
  } | null;
}

export default function StudentActionDialog({
  open,
  onClose,
  student,
  classId,
  isLocked,
  isOnline,
  onLockChange,
  isExamRoom = false,
  examRoomId,
  isActive = true,
  onScreenshotRequested,
  readyScreenshot = null
}: StudentActionDialogProps) {
  const [lockLoading, setLockLoading] = useState(false);
  const [screenshotLoading, setScreenshotLoading] = useState(false);
  const [screenshotImageUrl, setScreenshotImageUrl] = useState<string | null>(null);
  const [pendingScreenshotId, setPendingScreenshotId] = useState<number | null>(null);
  const [screenshotOpen, setScreenshotOpen] = useState(false);
  const [openWebDialogOpen, setOpenWebDialogOpen] = useState(false);
  const [webUrlInput, setWebUrlInput] = useState('');
  const [webUrlLoading, setWebUrlLoading] = useState(false);
  const [msgDialogOpen, setMsgDialogOpen] = useState(false);
  const [msgInput, setMsgInput] = useState('');
  const [msgLoading, setMsgLoading] = useState(false);
  const [sendFileDialogOpen, setSendFileDialogOpen] = useState(false);
  const [fileToSend, setFileToSend] = useState<File | null>(null);
  const [sendFileLoading, setSendFileLoading] = useState(false);
  const sendFileInputRef = useRef<HTMLInputElement>(null);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error'
  });

  useEffect(() => {
    if (!open) {
      setScreenshotImageUrl(null);
      setPendingScreenshotId(null);
      setScreenshotLoading(false);
      setScreenshotOpen(false);
      setOpenWebDialogOpen(false);
      setWebUrlInput('');
      setMsgDialogOpen(false);
      setMsgInput('');
      setSendFileDialogOpen(false);
      setFileToSend(null);
    }
  }, [open, student.studentId]);

  const showSnackbar = (message: string, severity: 'success' | 'error') => setSnackbar({ open: true, message, severity });

  useEffect(() => {
    if (!readyScreenshot || !pendingScreenshotId) return;
    if (readyScreenshot.screenshotId !== pendingScreenshotId || readyScreenshot.studentUserId !== student.userId) return;

    if (!readyScreenshot.imageUrl) {
      setScreenshotLoading(false);
      showSnackbar('Không nhận được URL ảnh màn hình', 'error');
      return;
    }

    setScreenshotImageUrl(readyScreenshot.imageUrl);
    setScreenshotOpen(true);
    setPendingScreenshotId(null);
    setScreenshotLoading(false);
  }, [readyScreenshot, pendingScreenshotId, student.userId]);

  useEffect(() => {
    if (!pendingScreenshotId) return;
    const timer = window.setTimeout(() => {
      setPendingScreenshotId(null);
      setScreenshotLoading(false);
      showSnackbar('Máy sinh viên chưa gửi ảnh màn hình về server', 'error');
    }, 15000);

    return () => window.clearTimeout(timer);
  }, [pendingScreenshotId]);

  const handleSendFileSelected = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] ?? null;
    if (file) {
      setFileToSend(file);
      setSendFileDialogOpen(true);
    }
    event.target.value = '';
  };

  const handleSendFileToStudent = async () => {
    if (!fileToSend) return;
    setSendFileLoading(true);
    try {
      const formData = new FormData();
      formData.append('file', fileToSend);
      const response = await sendFileToStudent(student.studentId, formData);
      if (response.statusCode === 200) {
        showSnackbar(`Đã gửi file "${fileToSend.name}" tới sinh viên`, 'success');
      } else {
        showSnackbar(response.message ?? 'Lỗi hệ thống, vui lòng thử lại sau', 'error');
      }
    } catch (error: unknown) {
      showSnackbar(extractApiErrorMessage(error, 'Lỗi hệ thống, vui lòng thử lại sau'), 'error');
    } finally {
      setSendFileLoading(false);
      setSendFileDialogOpen(false);
      setFileToSend(null);
    }
  };

  const handleSendMessageToStudent = async () => {
    if (!msgInput.trim()) return;
    setMsgLoading(true);
    try {
      if (isExamRoom && examRoomId) {
        await sendMessageToExamRoomStudent(examRoomId, student.studentId, msgInput.trim());
      } else {
        await sendMessageToStudent(classId, student.studentId, msgInput.trim());
      }
      showSnackbar('Đã gửi thông báo tới sinh viên', 'success');
      setMsgDialogOpen(false);
      setMsgInput('');
    } catch (error: unknown) {
      showSnackbar(extractApiErrorMessage(error, 'Lỗi hệ thống, vui lòng thử lại sau'), 'error');
    } finally {
      setMsgLoading(false);
    }
  };

  const handleOpenWebForStudent = async () => {
    if (!webUrlInput.trim()) return;
    setWebUrlLoading(true);
    try {
      if (isExamRoom && examRoomId) {
        await openWebsiteForExamRoomStudent(examRoomId, student.studentId, webUrlInput.trim());
      } else {
        await openWebsiteForStudent(classId, student.studentId, webUrlInput.trim());
      }
      showSnackbar('Đã mở trang web cho sinh viên', 'success');
      setOpenWebDialogOpen(false);
      setWebUrlInput('');
    } catch (error: unknown) {
      showSnackbar(extractApiErrorMessage(error, 'Lỗi hệ thống, vui lòng thử lại sau'), 'error');
    } finally {
      setWebUrlLoading(false);
    }
  };

  const handleLockToggle = async () => {
    setLockLoading(true);
    try {
      let res: { statusCode: number; data: null };
      if (isExamRoom && examRoomId) {
        res = await lockScreenForExamRoom(examRoomId, student.userId, !isLocked);
      } else {
        res = await lockScreen(classId, student.userId, !isLocked);
      }
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
      let res: { statusCode: number; data: { id: number; imageUrl: string | null } };
      if (isExamRoom && examRoomId) {
        res = await requestExamRoomScreenshot(examRoomId, student.userId);
      } else {
        res = await requestClassScreenshot(classId, student.userId);
      }
      if (res.statusCode === HttpStatusCode.Ok && res.data?.id) {
        onScreenshotRequested?.(res.data.id);
        setPendingScreenshotId(res.data.id);
      } else {
        setScreenshotLoading(false);
        showSnackbar('Không nhận được mã ảnh màn hình', 'error');
      }
    } catch (error: unknown) {
      setScreenshotLoading(false);
      showSnackbar(extractApiErrorMessage(error, 'Không thể chụp màn hình, vui lòng thử lại'), 'error');
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
              <Stack direction="row" alignItems="center" spacing={1}>
                <Typography variant="h6" fontWeight="bold" noWrap sx={{ minWidth: 0 }}>
                  {student.fullName}
                </Typography>
                <Tooltip title={isOnline ? 'Đã kết nối' : 'Mất kết nối'} arrow>
                  <Box sx={{ color: isOnline ? 'success.main' : 'text.disabled', display: 'flex', alignItems: 'center' }}>
                    <Wifi size={16} />
                  </Box>
                </Tooltip>
              </Stack>
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

          {/* Action Buttons — chỉ hiển thị khi lớp/phòng thi đang diễn ra */}
          {isActive && (
          <input type="file" ref={sendFileInputRef} onChange={handleSendFileSelected} style={{ display: 'none' }} />
          )}
          {isActive && <Stack direction="row" spacing={3} justifyContent="center" sx={{ mb: 2.5 }}>
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

            <Tooltip title="Gửi file" arrow>
              <Stack alignItems="center" spacing={0.75}>
                <IconButton
                  onClick={() => sendFileInputRef.current?.click()}
                  disabled={sendFileLoading}
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
                  <DocumentUpload size={26} />
                </IconButton>
                <Typography variant="caption" fontWeight="medium" color="text.secondary">
                  Gửi file
                </Typography>
              </Stack>
            </Tooltip>

            <Tooltip title="Gửi thông báo" arrow>
              <Stack alignItems="center" spacing={0.75}>
                <IconButton
                  onClick={() => setMsgDialogOpen(true)}
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
                    }
                  }}
                >
                  <MessageText size={26} />
                </IconButton>
                <Typography variant="caption" fontWeight="medium" color="text.secondary">
                  Thông báo
                </Typography>
              </Stack>
            </Tooltip>

            <Tooltip title="Mở trang web" arrow>
              <Stack alignItems="center" spacing={0.75}>
                <IconButton
                  onClick={() => setOpenWebDialogOpen(true)}
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
                    }
                  }}
                >
                  <Global size={26} />
                </IconButton>
                <Typography variant="caption" fontWeight="medium" color="text.secondary">
                  Mở web
                </Typography>
              </Stack>
            </Tooltip>
          </Stack>}

          {isActive && <Divider sx={{ mb: 2 }} />}

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

      {/* Send File Dialog */}
      <Dialog
        open={sendFileDialogOpen}
        onClose={() => {
          if (!sendFileLoading) {
            setSendFileDialogOpen(false);
            setFileToSend(null);
          }
        }}
        maxWidth="xs"
        fullWidth
        slotProps={{ paper: { sx: { borderRadius: 3 } } }}
      >
        <DialogTitle sx={{ pb: 1, pt: 2.5 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Typography variant="h5">Gửi file cho sinh viên</Typography>
            <IconButton
              onClick={() => {
                setSendFileDialogOpen(false);
                setFileToSend(null);
              }}
              disabled={sendFileLoading}
              size="small"
              sx={{ color: 'text.secondary' }}
            >
              <CloseCircle size={20} />
            </IconButton>
          </Stack>
        </DialogTitle>
        <DialogContent sx={{ pt: 1, pb: 1 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
            File sẽ được gửi tới máy của <strong>{student.fullName}</strong> và tải về tự động.
          </Typography>
          {fileToSend && (
            <Box sx={{ p: 1.5, border: '1px solid', borderColor: 'divider', borderRadius: 2, bgcolor: 'background.default' }}>
              <Typography variant="body2" fontWeight="medium" noWrap>
                {fileToSend.name}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {(fileToSend.size / 1024 / 1024).toFixed(2)} MB
              </Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button
            onClick={() => {
              setSendFileDialogOpen(false);
              setFileToSend(null);
            }}
            disabled={sendFileLoading}
          >
            Hủy
          </Button>
          <Button
            variant="contained"
            onClick={handleSendFileToStudent}
            disabled={sendFileLoading}
            startIcon={sendFileLoading ? <CircularProgress size={14} color="inherit" /> : <DocumentUpload size={15} />}
          >
            Gửi
          </Button>
        </DialogActions>
      </Dialog>

      {/* Send Message Dialog */}
      <Dialog
        open={msgDialogOpen}
        onClose={() => {
          setMsgDialogOpen(false);
          setMsgInput('');
        }}
        maxWidth="xs"
        fullWidth
        slotProps={{ paper: { sx: { borderRadius: 3 } } }}
      >
        <DialogTitle sx={{ pb: 1, pt: 2.5 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Typography variant="h5">Gửi thông báo</Typography>
            <IconButton
              onClick={() => {
                setMsgDialogOpen(false);
                setMsgInput('');
              }}
              size="small"
              sx={{ color: 'text.secondary' }}
            >
              <CloseCircle size={20} />
            </IconButton>
          </Stack>
        </DialogTitle>
        <DialogContent sx={{ pt: 1, pb: 1 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
            Gửi thông báo tới máy của <strong>{student.fullName}</strong>:
          </Typography>
          <TextField
            autoFocus
            fullWidth
            multiline
            rows={3}
            label="Nội dung thông báo"
            placeholder="Nhập nội dung thông báo..."
            value={msgInput}
            onChange={(e) => setMsgInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSendMessageToStudent();
              }
            }}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button
            onClick={() => {
              setMsgDialogOpen(false);
              setMsgInput('');
            }}
            disabled={msgLoading}
          >
            Hủy
          </Button>
          <Button
            variant="contained"
            onClick={handleSendMessageToStudent}
            disabled={msgLoading || !msgInput.trim()}
            startIcon={msgLoading ? <CircularProgress size={14} color="inherit" /> : <MessageText size={15} />}
          >
            Gửi
          </Button>
        </DialogActions>
      </Dialog>

      {/* Open Website Dialog */}
      <Dialog
        open={openWebDialogOpen}
        onClose={() => {
          setOpenWebDialogOpen(false);
          setWebUrlInput('');
        }}
        maxWidth="xs"
        fullWidth
        slotProps={{ paper: { sx: { borderRadius: 3 } } }}
      >
        <DialogTitle sx={{ pb: 1, pt: 2.5 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Typography variant="h5">Mở trang web cho sinh viên</Typography>
            <IconButton
              onClick={() => {
                setOpenWebDialogOpen(false);
                setWebUrlInput('');
              }}
              size="small"
              sx={{ color: 'text.secondary' }}
            >
              <CloseCircle size={20} />
            </IconButton>
          </Stack>
        </DialogTitle>
        <DialogContent sx={{ pt: 1, pb: 1 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
            Điều hướng máy của <strong>{student.fullName}</strong> tới trang web sau:
          </Typography>
          <TextField
            autoFocus
            fullWidth
            label="Địa chỉ trang web"
            placeholder="https://example.com"
            value={webUrlInput}
            onChange={(e) => setWebUrlInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleOpenWebForStudent();
            }}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button
            onClick={() => {
              setOpenWebDialogOpen(false);
              setWebUrlInput('');
            }}
            disabled={webUrlLoading}
          >
            Hủy
          </Button>
          <Button
            variant="contained"
            onClick={handleOpenWebForStudent}
            disabled={webUrlLoading || !webUrlInput.trim()}
            startIcon={webUrlLoading ? <CircularProgress size={14} color="inherit" /> : <Global size={15} />}
          >
            Mở web
          </Button>
        </DialogActions>
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
            <Typography variant="h6">Màn hình — {student.fullName} — {student.code}</Typography>
            <IconButton onClick={() => setScreenshotOpen(false)} size="small" sx={{ color: 'text.secondary' }}>
              <CloseCircle size={20} />
            </IconButton>
          </Stack>
        </DialogTitle>
        <DialogContent sx={{ p: 1.5, pt: 0 }}>
          {screenshotImageUrl && (
            <Box
              component="img"
              src={screenshotImageUrl}
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
