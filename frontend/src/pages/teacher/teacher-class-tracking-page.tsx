import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Grid,
  IconButton,
  Snackbar,
  Stack,
  TextField,
  Tooltip,
  Typography
} from '@mui/material';
import MainCard from 'components/MainCard';
import { ChangeEvent, useRef, useState } from 'react';
import { useClassTracking, StudentTrackingState } from 'hooks/useClassTracking';
import { ArrowLeft, ExportCurve, Global, ImportCurve, Key, Lock1, MessageText, Monitor, Timer1, Wifi } from 'iconsax-reactjs';
import { useLocation, useNavigate, useParams } from 'react-router';
import StudentActionDialog from 'sections/extra-pages/class/student-action-dialog';
import ImportVeyonKeyDialog from 'sections/extra-pages/class/import-veyon-key-dialog';
import { importStudentIntoClass, downloadClassStudentImportTemplate } from 'api/class';
import { openWebsiteForClass, sendMessageToClass } from 'api/veyon';
import { HttpStatusCode } from 'axios';
import useAuth from 'hooks/useAuth';

function formatTime(isoString: string): string {
  try {
    const date = new Date(isoString);
    const hh = String(date.getHours()).padStart(2, '0');
    const mm = String(date.getMinutes()).padStart(2, '0');
    const ss = String(date.getSeconds()).padStart(2, '0');
    return `${hh}:${mm}:${ss}`;
  } catch {
    return isoString;
  }
}

function useConnectionChip(studyStatus: number | undefined) {
  if (studyStatus === undefined) {
    return { label: 'Đang kết nối', color: 'warning' as const, icon: <Wifi size={14} /> };
  }
  if (studyStatus === 1) {
    return { label: 'Đã kết nối', color: 'success' as const, icon: <Wifi size={14} /> };
  }
  return { label: 'Chờ đến giờ học', color: 'warning' as const, icon: <Timer1 size={14} /> };
}

export default function TeacherClassTrackingPage() {
  const { id } = useParams<{ id: string }>();
  const classId = id ? Number(id) : null;
  const navigate = useNavigate();
  const location = useLocation();

  const studyStatus: number | undefined = location.state?.studyStatus;
  const chip = useConnectionChip(studyStatus);

  const [alert, setAlert] = useState({ open: false, message: '', severity: 'error' as 'success' | 'error' | 'info' | 'warning' });
  const [selectedStudent, setSelectedStudent] = useState<StudentTrackingState | null>(null);
  const [lockedStudents, setLockedStudents] = useState<Set<number>>(new Set());
  const [importKeyOpen, setImportKeyOpen] = useState(false);
  const [openWebDialogOpen, setOpenWebDialogOpen] = useState(false);
  const [webUrlInput, setWebUrlInput] = useState('');
  const [webUrlLoading, setWebUrlLoading] = useState(false);
  const [msgDialogOpen, setMsgDialogOpen] = useState(false);
  const [msgInput, setMsgInput] = useState('');
  const [msgLoading, setMsgLoading] = useState(false);
  const [reload, setReload] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { logout } = useAuth();

  const { students, loading, connectedStudentIds } = useClassTracking(
    classId,
    (message) => setAlert({ open: true, message, severity: 'error' }),
    reload,
    undefined,
    (studentName, studentCode) =>
      setAlert({
        open: true,
        message: `Sinh viên ${studentName} có mã sinh viên ${studentCode} đã mất kết nối với server`,
        severity: 'warning'
      })
  );

  const handleCardClick = (student: StudentTrackingState) => {
    setSelectedStudent(student);
  };

  const handleDialogClose = () => {
    setSelectedStudent(null);
  };

  const handleLockChange = (userId: number, locked: boolean) => {
    setLockedStudents((prev) => {
      const next = new Set(prev);
      if (locked) next.add(userId);
      else next.delete(userId);
      return next;
    });
  };

  // Keep selectedStudent in sync with live WS data
  const liveSelectedStudent = selectedStudent ? (students.find((s) => s.studentId === selectedStudent.studentId) ?? selectedStudent) : null;

  const handleFileImportChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files ? event.target.files[0] : null;
    if (file) {
      const fileTypes = ['application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'application/vnd.ms-excel'];
      if (!fileTypes.includes(file.type)) {
        setAlert({ open: true, message: 'File lỗi định dạng. Vui lòng thử lại', severity: 'error' });
        event.target.value = '';
        return;
      }
      const formData = new FormData();
      formData.append('file', file);
      try {
        const response = await importStudentIntoClass(classId!, formData);
        if (response.statusCode === HttpStatusCode.Ok) {
          setAlert({ open: true, message: 'Import sinh viên vào lớp thành công', severity: 'success' });
          setReload(!reload);
        } else if (response.statusCode === HttpStatusCode.Unauthorized) {
          logout();
        } else if (response.statusCode === HttpStatusCode.UnprocessableEntity) {
          setAlert({ open: true, message: response.message, severity: 'error' });
        } else {
          setAlert({ open: true, message: 'Lỗi hệ thống, vui lòng thử lại sau', severity: 'error' });
        }
      } catch {
        setAlert({ open: true, message: 'Lỗi hệ thống, vui lòng thử lại sau', severity: 'error' });
      }
      event.target.value = '';
    }
  };

  const handleSendMessageToClass = async () => {
    if (!classId || !msgInput.trim()) return;
    setMsgLoading(true);
    try {
      await sendMessageToClass(classId, msgInput.trim());
      setAlert({ open: true, message: 'Đã gửi thông báo tới tất cả sinh viên', severity: 'success' });
      setMsgDialogOpen(false);
      setMsgInput('');
    } catch {
      setAlert({ open: true, message: 'Lỗi hệ thống, vui lòng thử lại sau', severity: 'error' });
    } finally {
      setMsgLoading(false);
    }
  };

  const handleOpenWebForClass = async () => {
    if (!classId || !webUrlInput.trim()) return;
    setWebUrlLoading(true);
    try {
      await openWebsiteForClass(classId, webUrlInput.trim());
      setAlert({ open: true, message: 'Đã mở trang web cho tất cả sinh viên', severity: 'success' });
      setOpenWebDialogOpen(false);
      setWebUrlInput('');
    } catch {
      setAlert({ open: true, message: 'Lỗi hệ thống, vui lòng thử lại sau', severity: 'error' });
    } finally {
      setWebUrlLoading(false);
    }
  };

  const handleDownloadExcelForm = async () => {
    const response = await downloadClassStudentImportTemplate();
    const blob = new Blob([response], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
    const fileURL = URL.createObjectURL(blob);
    const link = document.createElement('a');

    link.href = fileURL;
    link.download = 'Mẫu import sinh viên vào lớp.xlsx';

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    URL.revokeObjectURL(fileURL);
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight={400}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Stack sx={{ p: 0 }}>
      {/* Ban detection alert */}
      <Snackbar
        open={alert.open}
        autoHideDuration={3000}
        onClose={() => setAlert((prev) => ({ ...prev, open: false }))}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Alert severity={alert.severity} variant="filled" sx={{ width: '100%', borderRadius: 2, fontSize: 15 }}>
          {alert.message}
        </Alert>
      </Snackbar>

      <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ gap: 2, justifyContent: 'space-between', pb: 3, alignItems: 'center' }}>
        <Stack direction="row" spacing={2} alignItems="center">
          <Box
            onClick={() => navigate('/dashboard/teacher')}
            sx={{ cursor: 'pointer', display: 'flex', alignItems: 'center', color: 'text.secondary', '&:hover': { color: 'primary.main' } }}
          >
            <ArrowLeft size={20} />
          </Box>
          <Typography variant="h3">Theo dõi sinh viên</Typography>
          <Chip label={`${students.length} sinh viên`} color="primary" size="small" variant="outlined" />
        </Stack>

        <Stack direction="row" spacing={1.5} alignItems="center">
          <input type="file" ref={fileInputRef} onChange={handleFileImportChange} style={{ display: 'none' }} accept=".xlsx, .xls" />
          <Button
            variant="outlined"
            size="small"
            startIcon={<ExportCurve size={15} />}
            onClick={handleDownloadExcelForm}
            sx={{ whiteSpace: 'nowrap' }}
          >
            Xuất file mẫu
          </Button>

          <Button
            variant="outlined"
            size="small"
            startIcon={<ImportCurve size={15} />}
            onClick={() => fileInputRef.current?.click()}
            sx={{ whiteSpace: 'nowrap' }}
          >
            Import sinh viên
          </Button>

          {classId && (
            <Button
              variant="outlined"
              size="small"
              startIcon={<Key size={15} />}
              onClick={() => setImportKeyOpen(true)}
              sx={{ whiteSpace: 'nowrap' }}
            >
              Import khóa Veyon
            </Button>
          )}
          <Chip icon={chip.icon} label={chip.label} color={chip.color} size="small" variant="outlined" />
        </Stack>
      </Stack>

      {classId && (
        <Box sx={{ display: 'flex', justifyContent: 'center', gap: 2, pb: 2 }}>
          <Tooltip title="Gửi thông báo tới cả lớp" arrow placement="top">
            <IconButton
              onClick={() => setMsgDialogOpen(true)}
              sx={{
                width: 56,
                height: 56,
                bgcolor: 'background.paper',
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
          </Tooltip>
          <Tooltip title="Mở trang web cho cả lớp" arrow placement="top">
            <IconButton
              onClick={() => setOpenWebDialogOpen(true)}
              sx={{
                width: 56,
                height: 56,
                bgcolor: 'background.paper',
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
          </Tooltip>
        </Box>
      )}

      <MainCard content={false}>
        <Box sx={{ p: 3 }}>
          {students.length === 0 ? (
            <Box textAlign="center" py={6}>
              <Typography color="text.secondary" variant="h6">
                Không có sinh viên nào trong lớp này
              </Typography>
            </Box>
          ) : (
            <Grid container spacing={3}>
              {students.map((student) => {
                const isLocked = lockedStudents.has(student.userId);
                const isOnline = connectedStudentIds.has(student.studentId);
                return (
                  <Grid key={student.studentId} size={{ xs: 12, sm: 6, md: 4 }}>
                    <Tooltip title="Bấm để xem chi tiết và điều khiển máy" placement="top" arrow>
                      <Card
                        onClick={() => handleCardClick(student)}
                        sx={{
                          height: '100%',
                          border: '1px solid',
                          borderColor: isLocked ? 'error.main' : 'divider',
                          borderRadius: 2,
                          display: 'flex',
                          flexDirection: 'column',
                          cursor: 'pointer',
                          transition: 'transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease',
                          '&:hover': {
                            transform: 'translateY(-3px)',
                            boxShadow: 4,
                            borderColor: isLocked ? 'error.dark' : 'primary.main'
                          },
                          '&:active': {
                            transform: 'translateY(-1px)',
                            boxShadow: 2
                          }
                        }}
                      >
                        <CardContent sx={{ p: 2.5, '&:last-child': { pb: 2.5 }, display: 'flex', flexDirection: 'column', height: '100%' }}>
                          <Stack spacing={1.5} sx={{ flex: 1, minHeight: 0 }}>
                            <Stack spacing={0.5}>
                              <Stack direction="row" alignItems="center" justifyContent="space-between">
                                <Typography
                                  variant="h6"
                                  fontWeight="bold"
                                  sx={{ flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', pr: 1 }}
                                >
                                  {student.fullName}
                                </Typography>
                                <Stack direction="row" spacing={0.75} alignItems="center" sx={{ flexShrink: 0 }}>
                                  <Tooltip title={isOnline ? 'Kết nối' : 'Mất kết nối'} arrow>
                                    <Box sx={{ color: isOnline ? 'success.main' : 'text.disabled', display: 'flex', alignItems: 'center' }}>
                                      <Wifi size={16} />
                                    </Box>
                                  </Tooltip>
                                  {isLocked && (
                                    <Tooltip title="Màn hình đang bị khoá" arrow>
                                      <Box sx={{ color: 'error.main', display: 'flex', alignItems: 'center' }}>
                                        <Lock1 size={16} />
                                      </Box>
                                    </Tooltip>
                                  )}
                                </Stack>
                              </Stack>
                              <Stack direction="row" spacing={1}>
                                <Typography variant="caption" color="text.secondary">
                                  {student.code}
                                </Typography>
                                <Typography variant="caption" color="text.disabled">
                                  ·
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                  {student.manageClassName}
                                </Typography>
                              </Stack>
                            </Stack>

                            <Divider />

                            <Stack spacing={0.5}>
                              <Stack direction="row" spacing={1} alignItems="center">
                                <Monitor size={14} />
                                <Typography variant="caption" fontWeight="medium" color="text.secondary">
                                  Ứng dụng đang dùng
                                </Typography>
                              </Stack>

                              <Box
                                sx={{
                                  maxHeight: 180,
                                  overflowX: 'auto',
                                  overflowY: 'auto',
                                  pr: 0.5,
                                  pb: 0.5,
                                  '&::-webkit-scrollbar': { width: 4, height: 4 },
                                  '&::-webkit-scrollbar-track': { bgcolor: 'transparent' },
                                  '&::-webkit-scrollbar-thumb': { bgcolor: 'divider', borderRadius: 2 }
                                }}
                              >
                                {student.appHistory.length === 0 ? (
                                  <Typography variant="caption" color="text.disabled" sx={{ fontStyle: 'italic' }}>
                                    Chưa có dữ liệu
                                  </Typography>
                                ) : (
                                  <Stack spacing={0.5} sx={{ minWidth: 'max-content' }}>
                                    {student.appHistory.map((entry, idx) => (
                                      <Stack key={idx} direction="row" spacing={1} alignItems="baseline">
                                        <Typography variant="caption" color="text.disabled" sx={{ minWidth: 56, fontFamily: 'monospace' }}>
                                          {formatTime(entry.createdAt)}
                                        </Typography>
                                        <Typography
                                          variant="caption"
                                          sx={{
                                            flex: 1,
                                            whiteSpace: 'nowrap',
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
                          </Stack>
                        </CardContent>
                      </Card>
                    </Tooltip>
                  </Grid>
                );
              })}
            </Grid>
          )}
        </Box>
      </MainCard>

      {liveSelectedStudent && classId && (
        <StudentActionDialog
          open={!!liveSelectedStudent}
          onClose={handleDialogClose}
          student={liveSelectedStudent}
          classId={classId}
          isLocked={lockedStudents.has(liveSelectedStudent.userId)}
          isOnline={connectedStudentIds.has(liveSelectedStudent.studentId)}
          onLockChange={handleLockChange}
        />
      )}

      {classId && <ImportVeyonKeyDialog open={importKeyOpen} onClose={() => setImportKeyOpen(false)} classId={classId} />}

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
        <DialogTitle>Gửi thông báo tới cả lớp</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            fullWidth
            multiline
            rows={3}
            label="Nội dung thông báo"
            placeholder="Nhập nội dung thông báo..."
            value={msgInput}
            onChange={(e) => setMsgInput(e.target.value)}
            sx={{ mt: 1 }}
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
            onClick={handleSendMessageToClass}
            disabled={msgLoading || !msgInput.trim()}
            startIcon={msgLoading ? <CircularProgress size={14} color="inherit" /> : <MessageText size={15} />}
          >
            Gửi
          </Button>
        </DialogActions>
      </Dialog>

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
        <DialogTitle>Mở trang web cho cả lớp</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            fullWidth
            label="Địa chỉ trang web"
            placeholder="https://example.com"
            value={webUrlInput}
            onChange={(e) => setWebUrlInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleOpenWebForClass();
            }}
            sx={{ mt: 1 }}
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
            onClick={handleOpenWebForClass}
            disabled={webUrlLoading || !webUrlInput.trim()}
            startIcon={webUrlLoading ? <CircularProgress size={14} color="inherit" /> : <Global size={15} />}
          >
            Mở web
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
