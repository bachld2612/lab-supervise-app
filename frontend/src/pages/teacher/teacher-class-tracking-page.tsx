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
import { ChangeEvent, useEffect, useMemo, useRef, useState } from 'react';
import { useClassTracking, StudentTrackingState } from 'hooks/useClassTracking';
import { ArrowLeft, DocumentUpload, ExportCurve, Global, ImportCurve, Key, Lock1, MessageText, Timer1, Wifi } from 'iconsax-reactjs';
import { useNavigate, useParams } from 'react-router';
import StudentActionDialog from 'sections/extra-pages/class/student-action-dialog';
import ImportVeyonKeyDialog from 'sections/extra-pages/class/import-veyon-key-dialog';
import { importStudentIntoClass, downloadClassStudentImportTemplate, sendFileToClass, getClassStudyStatus } from 'api/class';
import { openWebsiteForClass, sendMessageToClass } from 'api/veyon';
import { HttpStatusCode } from 'axios';
import useAuth from 'hooks/useAuth';

function formatTime(isoString: string): string {
  try {
    const date = new Date(isoString);
    const hh = String(date.getHours()).padStart(2, '0');
    const mm = String(date.getMinutes()).padStart(2, '0');
    return `${hh}:${mm}`;
  } catch {
    return isoString;
  }
}

export default function TeacherClassTrackingPage() {
  const { id } = useParams<{ id: string }>();
  const classId = id ? Number(id) : null;
  const navigate = useNavigate();

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
  const [sendFileDialogOpen, setSendFileDialogOpen] = useState(false);
  const [fileToSend, setFileToSend] = useState<File | null>(null);
  const [sendFileLoading, setSendFileLoading] = useState(false);
  const [reload, setReload] = useState(false);
  const [studyStatus, setStudyStatus] = useState<number | undefined>(undefined);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const sendFileInputRef = useRef<HTMLInputElement>(null);
  const { logout } = useAuth();

  useEffect(() => {
    if (!classId) return;
    getClassStudyStatus(classId).then((res) => {
      if (res?.statusCode === HttpStatusCode.Ok) setStudyStatus(res.data as number);
    });
  }, [classId]);

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

  const chip =
    studyStatus === undefined
      ? { label: 'Đang kết nối', color: 'warning' as const, icon: <Wifi size={14} /> }
      : studyStatus === 1
        ? { label: 'Đã kết nối', color: 'success' as const, icon: <Wifi size={14} /> }
        : studyStatus === 2
          ? { label: 'Đã kết thúc', color: 'default' as const, icon: <Timer1 size={14} /> }
          : { label: 'Chờ đến giờ học', color: 'warning' as const, icon: <Timer1 size={14} /> };

  const activityFeed = useMemo(() => {
    type FeedEntry = {
      eventType: 'app' | 'connect' | 'disconnect';
      studentName: string;
      applicationName?: string;
      banApplication?: boolean;
      createdAt: string;
    };
    const entries: FeedEntry[] = [];
    for (const s of students) {
      for (const e of s.appHistory) {
        if (e.connectionType) {
          entries.push({
            eventType: e.connectionType === 'CONNECT' ? 'connect' : 'disconnect',
            studentName: s.fullName,
            createdAt: e.createdAt
          });
        } else {
          entries.push({
            eventType: 'app',
            studentName: s.fullName,
            applicationName: e.applicationName,
            banApplication: e.banApplication,
            createdAt: e.createdAt
          });
        }
      }
    }
    return entries.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()).slice(0, 100);
  }, [students]);

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

  const handleSendFileSelected = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] ?? null;
    if (file) {
      setFileToSend(file);
      setSendFileDialogOpen(true);
    }
    event.target.value = '';
  };

  const handleSendFileToClass = async () => {
    if (!classId || !fileToSend) return;
    setSendFileLoading(true);
    try {
      const formData = new FormData();
      formData.append('file', fileToSend);
      const response = await sendFileToClass(classId, formData);
      if (response.statusCode === HttpStatusCode.Ok) {
        setAlert({ open: true, message: `Đã gửi file "${fileToSend.name}" tới tất cả sinh viên`, severity: 'success' });
      } else {
        setAlert({ open: true, message: response.message ?? 'Lỗi hệ thống, vui lòng thử lại sau', severity: 'error' });
      }
    } catch {
      setAlert({ open: true, message: 'Lỗi hệ thống, vui lòng thử lại sau', severity: 'error' });
    } finally {
      setSendFileLoading(false);
      setSendFileDialogOpen(false);
      setFileToSend(null);
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

      {classId && studyStatus === 1 && (
        <Box sx={{ display: 'flex', justifyContent: 'center', gap: 2, pb: 2 }}>
          <input type="file" ref={sendFileInputRef} onChange={handleSendFileSelected} style={{ display: 'none' }} />
          <Tooltip title="Gửi file tới cả lớp" arrow placement="top">
            <IconButton
              onClick={() => sendFileInputRef.current?.click()}
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
              <DocumentUpload size={26} />
            </IconButton>
          </Tooltip>
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

      <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2} alignItems="flex-start">
        {/* Left panel: student cards grid */}
        <Box sx={{ flex: '1 1 0', minWidth: 0 }}>
          <MainCard content={false}>
            <Box sx={{ p: 2 }}>
              {students.length === 0 ? (
                <Box textAlign="center" py={6}>
                  <Typography color="text.secondary" variant="h6">
                    Không có sinh viên nào trong lớp này
                  </Typography>
                </Box>
              ) : (
                <Grid container spacing={1.5}>
                  {students.map((student, idx) => {
                    const isLocked = lockedStudents.has(student.userId);
                    const isOnline = connectedStudentIds.has(student.studentId);
                    const latestEntry = student.appHistory.find((e) => !e.connectionType) ?? null;
                    const isBanned = isOnline && latestEntry?.banApplication === true;
                    return (
                      <Grid key={student.studentId} size={{ xs: 12, sm: 6, md: 3 }}>
                        <Tooltip title="Bấm để xem chi tiết và điều khiển máy" placement="top" arrow>
                          <Card
                            onClick={() => handleCardClick(student)}
                            sx={{
                              border: '2px solid',
                              borderColor: isBanned ? 'error.main' : isOnline ? 'success.main' : 'divider',
                              borderRadius: 2,
                              cursor: 'pointer',
                              bgcolor: isBanned ? 'rgba(255,86,48,0.08)' : 'background.paper',
                              transition: 'transform 0.15s, box-shadow 0.15s',
                              '&:hover': { transform: 'translateY(-2px)', boxShadow: 3 }
                            }}
                          >
                            <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
                              <Stack spacing={0.75}>
                                <Stack direction="row" alignItems="center" justifyContent="space-between">
                                  <Typography variant="caption" color="text.disabled" fontFamily="monospace" fontWeight="bold">
                                    {`PC-${String(idx + 1).padStart(2, '0')}`}
                                  </Typography>
                                  <Stack direction="row" spacing={0.5} alignItems="center">
                                    {isLocked && (
                                      <Box sx={{ color: 'warning.main', display: 'flex', alignItems: 'center' }}>
                                        <Lock1 size={12} />
                                      </Box>
                                    )}
                                    <Box
                                      sx={{
                                        width: 8,
                                        height: 8,
                                        borderRadius: '50%',
                                        bgcolor: isBanned ? 'error.main' : isOnline ? 'success.main' : 'text.disabled'
                                      }}
                                    />
                                  </Stack>
                                </Stack>
                                <Typography variant="body2" fontWeight="bold" noWrap title={student.fullName}>
                                  {student.fullName}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                  {student.code}
                                </Typography>
                                <Divider />
                                {isBanned ? (
                                  <Typography variant="caption" color="error.main" noWrap fontWeight="medium">
                                    {latestEntry.applicationName}
                                  </Typography>
                                ) : !isOnline ? (
                                  <Typography variant="caption" color="text.disabled" sx={{ fontStyle: 'italic' }}>
                                    {student.appHistory.length === 0 ? 'Chưa kết nối' : 'Offline'}
                                  </Typography>
                                ) : (
                                  <Typography variant="caption" color={latestEntry ? 'primary.main' : 'text.disabled'} noWrap>
                                    {latestEntry?.applicationName ?? 'Chưa có dữ liệu'}
                                  </Typography>
                                )}
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
        </Box>

        {/* Right panel: live activity feed */}
        <Box sx={{ flex: '0 0 300px', minWidth: 280, position: 'sticky', top: 80, alignSelf: 'flex-start' }}>
          <MainCard content={false}>
            <Box sx={{ p: 2 }}>
              <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1.5 }}>
                <Typography variant="h5">Hoạt Động Trực Tiếp</Typography>
                <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: 'success.main' }} />
              </Stack>
              <Divider sx={{ mb: 1.5 }} />
              <Box
                sx={{
                  maxHeight: 'calc(100vh - 260px)',
                  overflowY: 'auto',
                  '&::-webkit-scrollbar': { width: 4 },
                  '&::-webkit-scrollbar-track': { bgcolor: 'transparent' },
                  '&::-webkit-scrollbar-thumb': { bgcolor: 'divider', borderRadius: 2 }
                }}
              >
                {activityFeed.length === 0 ? (
                  <Typography variant="caption" color="text.disabled" sx={{ fontStyle: 'italic' }}>
                    Chưa có hoạt động
                  </Typography>
                ) : (
                  <Stack spacing={0}>
                    {activityFeed.map((entry, feedIdx) => (
                      <Stack
                        key={feedIdx}
                        direction="row"
                        spacing={1}
                        alignItems="flex-start"
                        sx={{ py: 1, borderBottom: '1px solid', borderColor: 'divider' }}
                      >
                        <Typography
                          variant="caption"
                          color="text.disabled"
                          sx={{ fontFamily: 'monospace', flexShrink: 0, lineHeight: 1.8 }}
                        >
                          {formatTime(entry.createdAt)}
                        </Typography>
                        <Box
                          sx={{
                            width: 6,
                            height: 6,
                            borderRadius: '50%',
                            bgcolor:
                              entry.eventType === 'connect'
                                ? 'success.main'
                                : entry.eventType === 'disconnect'
                                  ? 'warning.main'
                                  : entry.banApplication
                                    ? 'error.main'
                                    : 'primary.main',
                            flexShrink: 0,
                            mt: '5px'
                          }}
                        />
                        <Typography
                          variant="caption"
                          color={
                            entry.eventType === 'connect'
                              ? 'success.main'
                              : entry.eventType === 'disconnect'
                                ? 'warning.main'
                                : entry.banApplication
                                  ? 'error.main'
                                  : 'text.primary'
                          }
                          sx={{ lineHeight: 1.6 }}
                        >
                          {entry.eventType === 'connect' ? (
                            <>
                              <strong>{entry.studentName}</strong> đã kết nối vào hệ thống
                            </>
                          ) : entry.eventType === 'disconnect' ? (
                            <>
                              <strong>{entry.studentName}</strong> đã ngắt kết nối
                            </>
                          ) : (
                            <>
                              <strong>{entry.studentName}</strong> đổi ứng dụng sang {entry.applicationName}
                            </>
                          )}
                        </Typography>
                      </Stack>
                    ))}
                  </Stack>
                )}
              </Box>
            </Box>
          </MainCard>
        </Box>
      </Stack>

      {liveSelectedStudent && classId && (
        <StudentActionDialog
          open={!!liveSelectedStudent}
          onClose={handleDialogClose}
          student={liveSelectedStudent}
          classId={classId}
          isLocked={lockedStudents.has(liveSelectedStudent.userId)}
          isOnline={connectedStudentIds.has(liveSelectedStudent.studentId)}
          onLockChange={handleLockChange}
          isActive={studyStatus === 1}
        />
      )}

      {classId && <ImportVeyonKeyDialog open={importKeyOpen} onClose={() => setImportKeyOpen(false)} classId={classId} />}

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
        <DialogTitle>Gửi file tới cả lớp</DialogTitle>
        <DialogContent>
          <Stack spacing={1} sx={{ mt: 1 }}>
            <Typography variant="body2" color="text.secondary">
              File sẽ được gửi tới <strong>tất cả sinh viên</strong> trong lớp và tải về máy của họ.
            </Typography>
            {fileToSend && (
              <Box
                sx={{
                  p: 1.5,
                  border: '1px solid',
                  borderColor: 'divider',
                  borderRadius: 2,
                  bgcolor: 'background.default'
                }}
              >
                <Typography variant="body2" fontWeight="medium" noWrap>
                  {fileToSend.name}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {(fileToSend.size / 1024 / 1024).toFixed(2)} MB
                </Typography>
              </Box>
            )}
          </Stack>
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
            onClick={handleSendFileToClass}
            disabled={sendFileLoading}
            startIcon={sendFileLoading ? <CircularProgress size={14} color="inherit" /> : <DocumentUpload size={15} />}
          >
            Gửi
          </Button>
        </DialogActions>
      </Dialog>

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
