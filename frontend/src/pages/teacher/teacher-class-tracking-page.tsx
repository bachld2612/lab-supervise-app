import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControlLabel,
  Grid,
  IconButton,
  Snackbar,
  Stack,
  Switch,
  TextField,
  Tooltip,
  Typography
} from '@mui/material';
import MainCard from 'components/MainCard';
import VncViewer from 'components/VncViewer';
import { ChangeEvent, useEffect, useMemo, useRef, useState } from 'react';
import { useClassTracking, StudentTrackingState, ScreenshotReadyMessage } from 'hooks/useClassTracking';
import {
  ArrowLeft,
  CloseCircle,
  Copy,
  DocumentUpload,
  ExportCurve,
  Global,
  ImportCurve,
  Lock1,
  MessageText,
  Refresh,
  Timer1,
  VideoTick,
  Wifi
} from 'iconsax-reactjs';
import { useNavigate, useParams } from 'react-router';
import StudentActionDialog from 'sections/extra-pages/class/student-action-dialog';
import {
  importStudentIntoClass,
  downloadClassStudentImportTemplate,
  sendFileToClass,
  getClassStudyStatus,
  getById,
  setTrackingEnabled as setTrackingEnabledApi,
  updateWifiSsid,
  generateWifiSsid
} from 'api/class';
import { openWebsiteForClass, sendMessageToClass } from 'api/remote-control';
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
  const [pinnedStudentIds, setPinnedStudentIds] = useState<Set<number>>(new Set());
  const [readyScreenshot, setReadyScreenshot] = useState<ScreenshotReadyMessage | null>(null);
  const [autoScreenshots, setAutoScreenshots] = useState<Array<{ screenshotId: number; imageUrl: string; fullName: string; code: string }>>([]);
  const pendingManualScreenshotIdsRef = useRef<Set<number>>(new Set());
  const handledScreenshotIdsRef = useRef<Set<number>>(new Set());
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
  const [trackingEnabled, setTrackingEnabled] = useState(true);
  const [trackingToggleLoading, setTrackingToggleLoading] = useState(false);
  const [accessCodeDialogOpen, setAccessCodeDialogOpen] = useState(false);
  const [accessCodeInput, setAccessCodeInput] = useState('');
  const [accessCodeLoading, setAccessCodeLoading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const sendFileInputRef = useRef<HTMLInputElement>(null);
  const { logout } = useAuth();

  useEffect(() => {
    if (!classId) {
      setPinnedStudentIds(new Set());
      return;
    }

    try {
      const raw = sessionStorage.getItem(`class-tracking-pinned:${classId}`);
      const ids = raw ? (JSON.parse(raw) as number[]) : [];
      setPinnedStudentIds(new Set(ids));
    } catch {
      setPinnedStudentIds(new Set());
    }
  }, [classId]);

  useEffect(() => {
    if (!classId) return;
    getClassStudyStatus(classId).then((res) => {
      if (res?.statusCode === HttpStatusCode.Ok) setStudyStatus(res.data as number);
    });
    getById(classId).then((res) => {
      if (res?.statusCode === HttpStatusCode.Ok) {
        setTrackingEnabled(res.data?.trackingEnabled ?? true);
      }
    });
  }, [classId]);

  useEffect(() => {
    if (!accessCodeDialogOpen || !classId) return;
    getById(classId).then((res) => {
      if (res?.statusCode === HttpStatusCode.Ok) {
        setAccessCodeInput(res.data?.wifiSsid ?? '');
      }
    });
  }, [accessCodeDialogOpen, classId]);

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
      }),
    (message) => setReadyScreenshot(message),
    (message) => setAlert({ open: true, message, severity: 'warning' })
  );

  useEffect(() => {
    if (!readyScreenshot?.screenshotId || !readyScreenshot.imageUrl) return;
    if (handledScreenshotIdsRef.current.has(readyScreenshot.screenshotId)) return;

    handledScreenshotIdsRef.current.add(readyScreenshot.screenshotId);
    if (pendingManualScreenshotIdsRef.current.delete(readyScreenshot.screenshotId)) {
      return;
    }

    const imageUrl = readyScreenshot.imageUrl;
    const student = students.find((item) => item.studentId === readyScreenshot.studentId);
    setAutoScreenshots((prev) => [
      ...prev,
      {
        screenshotId: readyScreenshot.screenshotId,
        imageUrl,
        fullName: student?.fullName ?? 'Sinh viên',
        code: student?.code ?? ''
      }
    ]);
  }, [readyScreenshot, students]);

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
      eventType: 'app' | 'connect' | 'disconnect' | 'copy' | 'paste' | 'cut';
      studentName: string;
      studentCode: string;
      applicationName?: string;
      banApplication?: boolean;
      clipboardText?: string;
      createdAt: string;
    };
    const entries: FeedEntry[] = [];
    for (const s of students) {
      for (const e of s.appHistory) {
        if (e.connectionType) {
          entries.push({
            eventType: e.connectionType === 'CONNECT' ? 'connect' : 'disconnect',
            studentName: s.fullName,
            studentCode: s.code,
            createdAt: e.createdAt
          });
        } else if ((e.action ?? 0) !== 0) {
          entries.push({
            eventType: e.action === 1 ? 'copy' : e.action === 3 ? 'cut' : 'paste',
            studentName: s.fullName,
            studentCode: s.code,
            applicationName: e.applicationName,
            clipboardText: e.clipboardText,
            createdAt: e.createdAt
          });
        } else {
          entries.push({
            eventType: 'app',
            studentName: s.fullName,
            studentCode: s.code,
            applicationName: e.applicationName,
            banApplication: e.banApplication,
            createdAt: e.createdAt
          });
        }
      }
    }
    return entries.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()).slice(0, 100);
  }, [students]);

  const handleTrackingToggle = async (enabled: boolean) => {
    if (!classId || trackingToggleLoading) return;
    setTrackingToggleLoading(true);
    try {
      const res = await setTrackingEnabledApi(classId, enabled);
      if (res?.statusCode === HttpStatusCode.Ok) {
        setTrackingEnabled(enabled);
        setAlert({
          open: true,
          message: enabled ? 'Đã bật giám sát - ứng dụng cấm sẽ bị đánh dấu đỏ' : 'Đã tắt giám sát - ứng dụng cấm vẫn được lưu nhưng không cảnh báo',
          severity: enabled ? 'warning' : 'info'
        });
      } else {
        setAlert({ open: true, message: 'Không thể thay đổi trạng thái giám sát', severity: 'error' });
      }
    } catch {
      setAlert({ open: true, message: 'Lỗi hệ thống, vui lòng thử lại', severity: 'error' });
    } finally {
      setTrackingToggleLoading(false);
    }
  };
  const handleCardClick = (student: StudentTrackingState) => {
    setSelectedStudent(student);
  };

  const handleTogglePin = (studentId: number) => {
    if (!classId) return;
    setPinnedStudentIds((prev) => {
      const next = new Set(prev);
      if (next.has(studentId)) next.delete(studentId);
      else next.add(studentId);
      sessionStorage.setItem(`class-tracking-pinned:${classId}`, JSON.stringify(Array.from(next)));
      return next;
    });
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
  const orderedStudents = useMemo(
    () => [...students].sort((a, b) => Number(pinnedStudentIds.has(b.studentId)) - Number(pinnedStudentIds.has(a.studentId))),
    [students, pinnedStudentIds]
  );

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

  const handleGenerateAccessCode = async () => {
    if (!classId) return;
    setAccessCodeLoading(true);
    try {
      const res = await generateWifiSsid(classId);
      if (res?.statusCode === HttpStatusCode.Ok) {
        setAccessCodeInput(res.data as string);
        setAlert({ open: true, message: 'Đã tạo mã truy cập mới', severity: 'success' });
      }
    } catch {
      setAlert({ open: true, message: 'Lỗi khi tạo mã truy cập', severity: 'error' });
    } finally {
      setAccessCodeLoading(false);
    }
  };

  const handleSaveAccessCode = async () => {
    if (!classId) return;
    setAccessCodeLoading(true);
    try {
      const res = await updateWifiSsid(classId, accessCodeInput.trim());
      if (res?.statusCode === HttpStatusCode.Ok) {
        setAlert({ open: true, message: 'Đã cập nhật mã truy cập', severity: 'success' });
        setAccessCodeDialogOpen(false);
      } else {
        setAlert({ open: true, message: res?.message ?? 'Lỗi hệ thống', severity: 'error' });
      }
    } catch {
      setAlert({ open: true, message: 'Lỗi hệ thống, vui lòng thử lại sau', severity: 'error' });
    } finally {
      setAccessCodeLoading(false);
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
              startIcon={<Wifi size={15} />}
              onClick={() => setAccessCodeDialogOpen(true)}
              sx={{ whiteSpace: 'nowrap' }}
            >
              Mã truy cập
            </Button>
          )}
          <Tooltip title={trackingEnabled ? 'Đang giám sát - click để tắt' : 'Chưa giám sát - click để bật'} arrow>
            <FormControlLabel
              control={
                <Switch
                  checked={trackingEnabled}
                  onChange={(e) => handleTrackingToggle(e.target.checked)}
                  disabled={trackingToggleLoading}
                  color="error"
                  size="small"
                />
              }
              label={
                <Typography variant="body2" fontWeight="bold" color={trackingEnabled ? 'error.main' : 'text.secondary'}>
                  {trackingEnabled ? 'Đang giám sát' : 'Chưa giám sát'}
                </Typography>
              }
              sx={{ m: 0 }}
            />
          </Tooltip>
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
                  {orderedStudents.map((student) => {
                    const isLocked = lockedStudents.has(student.userId);
                    const isPinned = pinnedStudentIds.has(student.studentId);
                    const isOnline = connectedStudentIds.has(student.studentId);
                    const latestEntry = student.appHistory.find((e) => !e.connectionType && (e.action ?? 0) === 0) ?? null;
                    const isBanned = isOnline && latestEntry?.banApplication === true;
                    const borderColor = isBanned ? 'error.main' : isOnline ? 'success.main' : 'divider';
                    const dotColor = isBanned ? 'error.main' : isOnline ? 'success.main' : 'text.disabled';
                    return (
                      <Grid key={student.studentId} size={{ xs: 12, sm: isPinned ? 12 : 6 }}>
                        <Card
                          sx={{
                            border: '2px solid',
                            borderColor,
                            borderRadius: 2,
                            bgcolor: isBanned ? 'rgba(255,86,48,0.04)' : 'background.paper',
                            overflow: 'hidden',
                            boxShadow: isPinned ? 3 : 0
                          }}
                        >
                          {/* Header */}
                          <Box
                            sx={{
                              px: 1.5,
                              py: 0.75,
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'space-between',
                              borderBottom: '1px solid',
                              borderColor: 'divider',
                              minHeight: 40
                            }}
                          >
                            <Stack direction="row" spacing={1} alignItems="center" sx={{ minWidth: 0, flex: 1 }}>
                              <Box sx={{ width: 8, height: 8, borderRadius: '50%', flexShrink: 0, bgcolor: dotColor }} />
                              {isLocked && (
                                <Box sx={{ color: 'warning.main', display: 'flex', alignItems: 'center', flexShrink: 0 }}>
                                  <Lock1 size={12} />
                                </Box>
                              )}
                              <Typography variant="body2" fontWeight="bold" noWrap sx={{ flex: 1 }}>
                                {student.fullName}
                              </Typography>
                              <Typography variant="caption" color="text.secondary" noWrap sx={{ flexShrink: 0 }}>
                                {student.code}
                              </Typography>
                            </Stack>
                            <Tooltip title={isPinned ? 'Bỏ ghim màn hình' : 'Ghim màn hình'} arrow>
                              <IconButton
                                size="small"
                                color={isPinned ? 'primary' : 'default'}
                                onClick={() => handleTogglePin(student.studentId)}
                                sx={{ ml: 0.75, flexShrink: 0, p: 0.5 }}
                              >
                                <VideoTick size={17} variant={isPinned ? 'Bold' : 'Outline'} />
                              </IconButton>
                            </Tooltip>
                            <Button
                              size="small"
                              variant="outlined"
                              onClick={() => handleCardClick(student)}
                              sx={{ ml: 1, flexShrink: 0, py: 0.25, px: 1, fontSize: '0.7rem', lineHeight: 1.5 }}
                            >
                              Chi tiết
                            </Button>
                          </Box>

                          {/* VNC screen */}
                          {classId && <VncViewer classId={classId} studentUserId={student.userId} isOnline={isOnline} />}
                        </Card>
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
                                  : entry.eventType === 'copy' || entry.eventType === 'paste' || entry.eventType === 'cut'
                                    ? 'warning.main'
                                    : entry.banApplication
                                      ? 'error.main'
                                      : 'primary.main',
                            flexShrink: 0,
                            mt: '5px'
                          }}
                        />
                        <Tooltip title={entry.clipboardText ?? ''} arrow placement="left">
                        <Typography
                          variant="caption"
                          color={
                            entry.eventType === 'connect'
                              ? 'success.main'
                              : entry.eventType === 'disconnect'
                                ? 'warning.main'
                                : entry.eventType === 'copy' || entry.eventType === 'paste' || entry.eventType === 'cut'
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
                          ) : entry.eventType === 'copy' ? (
                            <>
                              Sinh viên <strong>{entry.studentName}</strong> - {entry.studentCode} đã SAO CHÉP nội dung từ {entry.applicationName}
                            </>
                          ) : entry.eventType === 'paste' ? (
                            <>
                              Sinh viên <strong>{entry.studentName}</strong> - {entry.studentCode} đã DÁN nội dung từ {entry.applicationName}
                            </>
                          ) : entry.eventType === 'cut' ? (
                            <>
                              Sinh viên <strong>{entry.studentName}</strong> - {entry.studentCode} đã CẮT nội dung từ {entry.applicationName}
                            </>
                          ) : (
                            <>
                              <strong>{entry.studentName}</strong> đổi ứng dụng sang {entry.applicationName}
                            </>
                          )}
                        </Typography>
                        </Tooltip>
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
          onScreenshotRequested={(screenshotId) => pendingManualScreenshotIdsRef.current.add(screenshotId)}
          readyScreenshot={readyScreenshot}
        />
      )}

      {autoScreenshots.map((screenshot) => (
        <Dialog
          key={screenshot.screenshotId}
          open
          onClose={() => setAutoScreenshots((prev) => prev.filter((item) => item.screenshotId !== screenshot.screenshotId))}
          maxWidth="lg"
          slotProps={{ paper: { sx: { borderRadius: 2 } } }}
        >
          <DialogTitle sx={{ py: 1.5 }}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
              <Typography variant="h6">
                Màn hình — {screenshot.fullName} — {screenshot.code}
              </Typography>
              <IconButton
                onClick={() => setAutoScreenshots((prev) => prev.filter((item) => item.screenshotId !== screenshot.screenshotId))}
                size="small"
                sx={{ color: 'text.secondary' }}
              >
                <CloseCircle size={20} />
              </IconButton>
            </Stack>
          </DialogTitle>
          <DialogContent sx={{ p: 1.5, pt: 0 }}>
            <Box
              component="img"
              src={screenshot.imageUrl}
              alt={`Screenshot — ${screenshot.fullName}`}
              sx={{ width: '100%', display: 'block', borderRadius: 1 }}
            />
          </DialogContent>
        </Dialog>
      ))}

      <Dialog
        open={accessCodeDialogOpen}
        onClose={() => !accessCodeLoading && setAccessCodeDialogOpen(false)}
        maxWidth="xs"
        fullWidth
        slotProps={{ paper: { sx: { borderRadius: 3 } } }}
      >
        <DialogTitle>Quản lý mã truy cập</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Box
              sx={{
                p: 1.5,
                bgcolor: 'primary.lighter',
                borderRadius: 1.5,
                border: '1px solid',
                borderColor: 'primary.light'
              }}
            >
              <Typography variant="body2" fontWeight="medium" color="primary.dark" sx={{ mb: 0.5 }}>
                Cách thiết lập
              </Typography>
              <Typography variant="caption" color="text.secondary" display="block">
                Đặt tên hotspot điện thoại hoặc máy tính của bạn <strong>chính xác bằng mã bên dưới</strong> — sinh viên sẽ tự động xác minh
                được vị trí khi đăng nhập.
              </Typography>
              <Typography variant="caption" color="text.disabled" display="block" sx={{ mt: 0.75 }}>
                Nếu sinh viên không nhận diện được WiFi, chiếu mã lên màn hình để họ nhập thủ công qua tùy chọn "Đăng nhập bằng mã truy
                cập".
              </Typography>
            </Box>

            <Stack direction="row" spacing={1} alignItems="center">
              <TextField
                fullWidth
                placeholder="Nhập mã hoặc tạo tự động..."
                value={accessCodeInput}
                onChange={(e) => setAccessCodeInput(e.target.value)}
                size="small"
              />
              <Tooltip title="Sao chép mã" arrow>
                <span>
                  <IconButton
                    size="small"
                    disabled={!accessCodeInput}
                    onClick={() => {
                      navigator.clipboard.writeText(accessCodeInput);
                      setAlert({ open: true, message: 'Đã sao chép mã truy cập', severity: 'success' });
                    }}
                  >
                    <Copy size={17} />
                  </IconButton>
                </span>
              </Tooltip>
              <Tooltip title="Tạo mã ngẫu nhiên" arrow>
                <span>
                  <IconButton size="small" disabled={accessCodeLoading} onClick={handleGenerateAccessCode}>
                    {accessCodeLoading ? <CircularProgress size={15} color="inherit" /> : <Refresh size={17} />}
                  </IconButton>
                </span>
              </Tooltip>
            </Stack>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button onClick={() => setAccessCodeDialogOpen(false)} disabled={accessCodeLoading}>
            Đóng
          </Button>
          <Button
            variant="contained"
            onClick={handleSaveAccessCode}
            disabled={accessCodeLoading}
            startIcon={accessCodeLoading ? <CircularProgress size={14} color="inherit" /> : undefined}
          >
            Lưu
          </Button>
        </DialogActions>
      </Dialog>

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
