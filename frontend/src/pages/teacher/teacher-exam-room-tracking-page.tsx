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
  List,
  ListItem,
  ListItemText,
  Snackbar,
  Stack,
  Switch,
  Tab,
  Tabs,
  TextField,
  Tooltip,
  Typography
} from '@mui/material';
import MainCard from 'components/MainCard';
import VncViewer from 'components/VncViewer';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useExamRoomTracking, StudentTrackingState, ScreenshotReadyMessage } from 'hooks/useExamRoomTracking';
import { Add, ArrowLeft, CloseCircle, Copy, Global, Lock1, MessageText, Refresh, Timer1, Trash, VideoTick, Wifi } from 'iconsax-reactjs';
import { useNavigate, useParams } from 'react-router';
import { HttpStatusCode } from 'axios';
import { AllowedApplication } from 'types/allowed-application';
import * as allowedApplicationApi from 'api/allowed-application';
import { openWebsiteForExamRoom, sendMessageToExamRoom } from 'api/remote-control';
import {
  getById as getExamRoomById,
  getStudyStatus as getExamStudyStatus,
  setTrackingEnabled as setTrackingEnabledApi,
  updateWifiSsid,
  generateWifiSsid
} from 'api/exam-room';
import StudentActionDialog from 'sections/extra-pages/class/student-action-dialog';
import { StudentTrackingState as ClassStudentTrackingState } from 'hooks/useClassTracking';

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

export default function TeacherExamRoomTrackingPage() {
  const { id } = useParams<{ id: string }>();
  const examRoomId = id ? Number(id) : null;
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
  const [rightTab, setRightTab] = useState(0);
  const [allowedApps, setAllowedApps] = useState<AllowedApplication[]>([]);
  const [addAppDialogOpen, setAddAppDialogOpen] = useState(false);
  const [newAppName, setNewAppName] = useState('');
  const [newAppImageUrl, setNewAppImageUrl] = useState('');
  const [addAppLoading, setAddAppLoading] = useState(false);
  const [examRoomName, setExamRoomName] = useState('');
  const [examStatus, setExamStatus] = useState<number | undefined>(undefined);
  const [trackingEnabled, setTrackingEnabled] = useState(false);
  const [trackingToggleLoading, setTrackingToggleLoading] = useState(false);
  const [accessCodeDialogOpen, setAccessCodeDialogOpen] = useState(false);
  const [accessCodeInput, setAccessCodeInput] = useState('');
  const [accessCodeLoading, setAccessCodeLoading] = useState(false);

  useEffect(() => {
    if (!examRoomId) {
      setPinnedStudentIds(new Set());
      return;
    }

    try {
      const raw = sessionStorage.getItem(`exam-room-tracking-pinned:${examRoomId}`);
      const ids = raw ? (JSON.parse(raw) as number[]) : [];
      setPinnedStudentIds(new Set(ids));
    } catch {
      setPinnedStudentIds(new Set());
    }
  }, [examRoomId]);

  useEffect(() => {
    if (!accessCodeDialogOpen || !examRoomId) return;
    getExamRoomById(examRoomId).then((res) => {
      if (res?.statusCode === HttpStatusCode.Ok) {
        setAccessCodeInput(res.data?.wifiSsid ?? '');
      }
    });
  }, [accessCodeDialogOpen, examRoomId]);

  useEffect(() => {
    if (!examRoomId) return;
    allowedApplicationApi.getList(examRoomId, { page: 0, size: 100 }).then((res) => {
      if (res?.statusCode === HttpStatusCode.Ok) {
        setAllowedApps(res.data?.content ?? []);
      }
    });
    getExamRoomById(examRoomId).then((res) => {
      if (res?.statusCode === HttpStatusCode.Ok) {
        setExamRoomName(res.data?.code ?? '');
        setTrackingEnabled(res.data?.trackingEnabled ?? false);
      }
    });
    getExamStudyStatus(examRoomId).then((res) => {
      if (res?.statusCode === HttpStatusCode.Ok) setExamStatus(res.data as number);
    });
  }, [examRoomId]);

  const handleTrackingToggle = async (enabled: boolean) => {
    if (!examRoomId || trackingToggleLoading) return;
    setTrackingToggleLoading(true);
    try {
      const res = await setTrackingEnabledApi(examRoomId, enabled);
      if (res?.statusCode === HttpStatusCode.Ok) {
        setTrackingEnabled(enabled);
        setAlert({
          open: true,
          message: enabled ? 'Đã bật giám sát — ứng dụng vi phạm sẽ bị đánh dấu đỏ' : 'Đã tắt giám sát — mọi ứng dụng đều hợp lệ',
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

  const handleLockChange = (userId: number, locked: boolean) => {
    setLockedStudents((prev) => {
      const next = new Set(prev);
      if (locked) next.add(userId);
      else next.delete(userId);
      return next;
    });
  };

  const handleCardClick = (student: StudentTrackingState) => {
    setSelectedStudent(student);
  };

  const handleTogglePin = (studentId: number) => {
    if (!examRoomId) return;
    setPinnedStudentIds((prev) => {
      const next = new Set(prev);
      if (next.has(studentId)) next.delete(studentId);
      else next.add(studentId);
      sessionStorage.setItem(`exam-room-tracking-pinned:${examRoomId}`, JSON.stringify(Array.from(next)));
      return next;
    });
  };

  const { students, loading, connectedStudentIds } = useExamRoomTracking(
    examRoomId,
    (message) => setAlert({ open: true, message, severity: 'error' }),
    undefined,
    undefined,
    (studentName, studentCode) =>
      setAlert({
        open: true,
        message: `Sinh viên ${studentName} mã ${studentCode} đã mất kết nối`,
        severity: 'warning'
      }),
    (apps) => setAllowedApps(apps),
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

  const liveSelectedStudent = selectedStudent ? (students.find((student) => student.studentId === selectedStudent.studentId) ?? selectedStudent) : null;
  const orderedStudents = useMemo(
    () => [...students].sort((a, b) => Number(pinnedStudentIds.has(b.studentId)) - Number(pinnedStudentIds.has(a.studentId))),
    [students, pinnedStudentIds]
  );

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

  const handleAddApp = async () => {
    if (!examRoomId || !newAppName.trim()) return;
    setAddAppLoading(true);
    try {
      const res = await allowedApplicationApi.create({
        examRoomId,
        applicationName: newAppName.trim(),
        imageUrl: newAppImageUrl.trim() || null
      });
      if (res?.statusCode === HttpStatusCode.Ok) {
        setAlert({ open: true, message: 'Đã thêm ứng dụng vào danh sách cho phép', severity: 'success' });
        setAddAppDialogOpen(false);
        setNewAppName('');
        setNewAppImageUrl('');
        // Refresh list
        allowedApplicationApi.getList(examRoomId, { page: 0, size: 100 }).then((r) => {
          if (r?.statusCode === HttpStatusCode.Ok) setAllowedApps(r.data?.content ?? []);
        });
      } else {
        setAlert({ open: true, message: res?.message ?? 'Lỗi hệ thống', severity: 'error' });
      }
    } finally {
      setAddAppLoading(false);
    }
  };

  const handleDeleteApp = async (appId: number) => {
    const res = await allowedApplicationApi.deleteById(appId);
    if (res?.statusCode === HttpStatusCode.Ok) {
      setAlert({ open: true, message: 'Đã xoá ứng dụng khỏi danh sách', severity: 'success' });
      setAllowedApps((prev) => prev.filter((a) => a.id !== appId));
    } else {
      setAlert({ open: true, message: res?.message ?? 'Lỗi hệ thống', severity: 'error' });
    }
  };

  const handleOpenWebForExamRoom = async () => {
    if (!examRoomId || !webUrlInput.trim()) return;
    setWebUrlLoading(true);
    try {
      await openWebsiteForExamRoom(examRoomId, webUrlInput.trim());
      setAlert({ open: true, message: 'Đã mở trang web cho tất cả sinh viên', severity: 'success' });
      setOpenWebDialogOpen(false);
      setWebUrlInput('');
    } catch {
      setAlert({ open: true, message: 'Lỗi hệ thống, vui lòng thử lại sau', severity: 'error' });
    } finally {
      setWebUrlLoading(false);
    }
  };

  const handleSendMessageToExamRoom = async () => {
    if (!examRoomId || !msgInput.trim()) return;
    setMsgLoading(true);
    try {
      await sendMessageToExamRoom(examRoomId, msgInput.trim());
      setAlert({ open: true, message: 'Đã gửi thông báo tới tất cả sinh viên', severity: 'success' });
      setMsgDialogOpen(false);
      setMsgInput('');
    } catch {
      setAlert({ open: true, message: 'Lỗi hệ thống, vui lòng thử lại sau', severity: 'error' });
    } finally {
      setMsgLoading(false);
    }
  };

  const handleGenerateAccessCode = async () => {
    if (!examRoomId) return;
    setAccessCodeLoading(true);
    try {
      const res = await generateWifiSsid(examRoomId);
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
    if (!examRoomId) return;
    setAccessCodeLoading(true);
    try {
      const res = await updateWifiSsid(examRoomId, accessCodeInput.trim());
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

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight={400}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Stack sx={{ p: 0 }}>
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
            onClick={() => navigate(-1)}
            sx={{ cursor: 'pointer', display: 'flex', alignItems: 'center', color: 'text.secondary', '&:hover': { color: 'primary.main' } }}
          >
            <ArrowLeft size={20} />
          </Box>
          <Typography variant="h3">Giám sát phòng thi {examRoomName && `— ${examRoomName}`}</Typography>
          <Chip label={`${students.length} sinh viên`} color="primary" size="small" variant="outlined" />
        </Stack>

        <Stack direction="row" spacing={1.5} alignItems="center">
          {examRoomId && (
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
          <Tooltip title={trackingEnabled ? 'Đang giám sát — click để tắt' : 'Chưa giám sát — click để bật'} arrow>
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
          {(() => {
            if (examStatus === undefined)
              return <Chip icon={<Wifi size={14} />} label="Đang kết nối" color="warning" size="small" variant="outlined" />;
            if (examStatus === 1)
              return <Chip icon={<Wifi size={14} />} label="Đang diễn ra" color="success" size="small" variant="outlined" />;
            if (examStatus === 2)
              return <Chip icon={<Timer1 size={14} />} label="Đã kết thúc" color="default" size="small" variant="outlined" />;
            return <Chip icon={<Timer1 size={14} />} label="Chờ đến giờ thi" color="warning" size="small" variant="outlined" />;
          })()}
        </Stack>
      </Stack>

      {examRoomId && examStatus === 1 && (
        <Box sx={{ display: 'flex', justifyContent: 'center', gap: 2, pb: 2 }}>
          <Tooltip title="Gửi thông báo tới cả phòng" arrow placement="top">
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
                '&:hover': { bgcolor: 'primary.lighter', borderColor: 'primary.main', color: 'primary.main', transform: 'scale(1.05)' }
              }}
            >
              <MessageText size={26} />
            </IconButton>
          </Tooltip>
          <Tooltip title="Mở trang web cho cả phòng" arrow placement="top">
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
                '&:hover': { bgcolor: 'primary.lighter', borderColor: 'primary.main', color: 'primary.main', transform: 'scale(1.05)' }
              }}
            >
              <Global size={26} />
            </IconButton>
          </Tooltip>
        </Box>
      )}

      <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2} alignItems="flex-start">
        {/* Left panel: student cards */}
        <Box sx={{ flex: '1 1 0', minWidth: 0 }}>
          <MainCard content={false}>
            <Box sx={{ p: 2 }}>
              {students.length === 0 ? (
                <Box textAlign="center" py={6}>
                  <Typography color="text.secondary" variant="h6">
                    Không có sinh viên nào trong phòng thi này
                  </Typography>
                </Box>
              ) : (
                <Grid container spacing={1.5}>
                  {orderedStudents.map((student) => {
                    const isLocked = lockedStudents.has(student.userId);
                    const isPinned = pinnedStudentIds.has(student.studentId);
                    const isOnline = connectedStudentIds.has(student.studentId);
                    const latestEntry = student.appHistory.find((e) => !e.connectionType && (e.action ?? 0) === 0) ?? null;
                    const isViolation = isOnline && latestEntry?.banApplication === true;
                    const borderColor = isViolation ? 'error.main' : isOnline ? 'success.main' : 'divider';
                    const dotColor = isViolation ? 'error.main' : isOnline ? 'success.main' : 'text.disabled';
                    return (
                      <Grid key={student.studentId} size={{ xs: 12, sm: isPinned ? 12 : 6 }}>
                        <Card
                          sx={{
                            border: '2px solid',
                            borderColor,
                            borderRadius: 2,
                            bgcolor: isViolation ? 'rgba(255,86,48,0.04)' : 'background.paper',
                            overflow: 'hidden',
                            boxShadow: isPinned ? 3 : 0
                          }}
                        >
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

                          {examRoomId && <VncViewer classId={examRoomId} studentUserId={student.userId} isOnline={isOnline} mode="exam-room" />}
                        </Card>
                      </Grid>
                    );
                  })}
                </Grid>
              )}
            </Box>
          </MainCard>
        </Box>

        {/* Right panel: tabs (activity + whitelist) */}
        <Box sx={{ flex: '0 0 320px', minWidth: 300, position: 'sticky', top: 80, alignSelf: 'flex-start' }}>
          <MainCard content={false}>
            <Tabs value={rightTab} onChange={(_, v) => setRightTab(v)} sx={{ borderBottom: 1, borderColor: 'divider', px: 2, pt: 1 }}>
              <Tab label="Hoạt động" sx={{ fontSize: 13 }} />
              <Tab label={`Whitelist (${allowedApps.length})`} sx={{ fontSize: 13 }} />
            </Tabs>

            {/* Tab 0: Activity feed */}
            {rightTab === 0 && (
              <Box sx={{ p: 2 }}>
                <Box
                  sx={{
                    maxHeight: 'calc(100vh - 300px)',
                    overflowY: 'auto',
                    '&::-webkit-scrollbar': { width: 4 },
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
                              flexShrink: 0,
                              mt: '5px',
                              bgcolor:
                                entry.eventType === 'connect'
                                  ? 'success.main'
                                  : entry.eventType === 'disconnect'
                                    ? 'warning.main'
                                    : entry.eventType === 'copy' || entry.eventType === 'paste' || entry.eventType === 'cut'
                                      ? 'warning.main'
                                      : entry.banApplication
                                        ? 'error.main'
                                        : 'primary.main'
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
                                <strong>{entry.studentName}</strong> đã kết nối
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
                                <strong>{entry.studentName}</strong> mở {entry.applicationName}
                                {entry.banApplication ? ' ⚠️ vi phạm' : ''}
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
            )}

            {/* Tab 1: Whitelist management */}
            {rightTab === 1 && (
              <Box sx={{ p: 2 }}>
                <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1.5 }}>
                  <Typography variant="body2" color="text.secondary">
                    Ứng dụng được phép sử dụng
                  </Typography>
                  <Button size="small" variant="contained" startIcon={<Add size={14} />} onClick={() => setAddAppDialogOpen(true)}>
                    Thêm
                  </Button>
                </Stack>
                <Box sx={{ maxHeight: 'calc(100vh - 340px)', overflowY: 'auto' }}>
                  {allowedApps.length === 0 ? (
                    <Typography variant="caption" color="text.disabled" sx={{ fontStyle: 'italic' }}>
                      Chưa có ứng dụng nào trong danh sách. Nếu danh sách trống, mọi ứng dụng đều bị coi là vi phạm.
                    </Typography>
                  ) : (
                    <List dense disablePadding>
                      {allowedApps.map((app) => (
                        <ListItem
                          key={app.id}
                          disablePadding
                          secondaryAction={
                            <IconButton edge="end" size="small" color="error" onClick={() => handleDeleteApp(app.id)}>
                              <Trash size={15} />
                            </IconButton>
                          }
                          sx={{ py: 0.5, borderBottom: '1px solid', borderColor: 'divider' }}
                        >
                          <ListItemText primary={app.applicationName} primaryTypographyProps={{ variant: 'body2', noWrap: true }} />
                        </ListItem>
                      ))}
                    </List>
                  )}
                </Box>
              </Box>
            )}
          </MainCard>
        </Box>
      </Stack>

      {/* Add allowed app dialog */}
      <Dialog
        open={addAppDialogOpen}
        onClose={() => {
          setAddAppDialogOpen(false);
          setNewAppName('');
          setNewAppImageUrl('');
        }}
        maxWidth="xs"
        fullWidth
        slotProps={{ paper: { sx: { borderRadius: 3 } } }}
      >
        <DialogTitle>Thêm ứng dụng được phép</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              autoFocus
              fullWidth
              label="Tên ứng dụng"
              placeholder="VD: Chrome, Word, Calculator..."
              value={newAppName}
              onChange={(e) => setNewAppName(e.target.value)}
            />
            <TextField
              fullWidth
              label="URL hình ảnh (tuỳ chọn)"
              placeholder="https://..."
              value={newAppImageUrl}
              onChange={(e) => setNewAppImageUrl(e.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button
            onClick={() => {
              setAddAppDialogOpen(false);
              setNewAppName('');
              setNewAppImageUrl('');
            }}
            disabled={addAppLoading}
          >
            Hủy
          </Button>
          <Button
            variant="contained"
            onClick={handleAddApp}
            disabled={addAppLoading || !newAppName.trim()}
            startIcon={addAppLoading ? <CircularProgress size={14} color="inherit" /> : <Add size={15} />}
          >
            Thêm
          </Button>
        </DialogActions>
      </Dialog>

      {/* Send message dialog */}
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
        <DialogTitle>Gửi thông báo tới cả phòng thi</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            fullWidth
            multiline
            rows={3}
            label="Nội dung thông báo"
            placeholder="Nhập nội dung..."
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
            onClick={handleSendMessageToExamRoom}
            disabled={msgLoading || !msgInput.trim()}
            startIcon={msgLoading ? <CircularProgress size={14} color="inherit" /> : <MessageText size={15} />}
          >
            Gửi
          </Button>
        </DialogActions>
      </Dialog>

      {/* Access code dialog */}
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
            <Box sx={{ p: 1.5, bgcolor: 'primary.lighter', borderRadius: 1.5, border: '1px solid', borderColor: 'primary.light' }}>
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

      {/* Open website dialog */}
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
        <DialogTitle>Mở trang web cho cả phòng thi</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            fullWidth
            label="Địa chỉ trang web"
            placeholder="https://example.com"
            value={webUrlInput}
            onChange={(e) => setWebUrlInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleOpenWebForExamRoom();
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
            onClick={handleOpenWebForExamRoom}
            disabled={webUrlLoading || !webUrlInput.trim()}
            startIcon={webUrlLoading ? <CircularProgress size={14} color="inherit" /> : <Global size={15} />}
          >
            Mở web
          </Button>
        </DialogActions>
      </Dialog>

      {/* Student action dialog */}
      {liveSelectedStudent && examRoomId && (
        <StudentActionDialog
          open={!!liveSelectedStudent}
          onClose={() => setSelectedStudent(null)}
          student={liveSelectedStudent as unknown as ClassStudentTrackingState}
          classId={examRoomId}
          isExamRoom
          examRoomId={examRoomId}
          isLocked={lockedStudents.has(liveSelectedStudent.userId)}
          isOnline={connectedStudentIds.has(liveSelectedStudent.studentId)}
          onLockChange={handleLockChange}
          isActive={examStatus === 1}
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
    </Stack>
  );
}
