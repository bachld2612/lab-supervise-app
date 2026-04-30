import { Alert, Box, Button, Card, CardContent, Chip, CircularProgress, Divider, Grid, Snackbar, Stack, Tooltip, Typography } from '@mui/material';
import MainCard from 'components/MainCard';
import { useState } from 'react';
import { useClassTracking, StudentTrackingState } from 'hooks/useClassTracking';
import { ArrowLeft, Key, Lock1, Monitor, Timer1, Wifi } from 'iconsax-reactjs';
import { useLocation, useNavigate, useParams } from 'react-router';
import StudentActionDialog from 'sections/extra-pages/class/student-action-dialog';
import ImportVeyonKeyDialog from 'sections/extra-pages/class/import-veyon-key-dialog';

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

  const { students, loading } = useClassTracking(classId, (message) => {
    setAlert({ open: true, message, severity: 'error' });
  });

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
                                <Typography variant="h6" fontWeight="bold">
                                  {student.fullName}
                                </Typography>
                                {isLocked && (
                                  <Tooltip title="Màn hình đang bị khoá" arrow>
                                    <Box sx={{ color: 'error.main', display: 'flex', alignItems: 'center' }}>
                                      <Lock1 size={16} />
                                    </Box>
                                  </Tooltip>
                                )}
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
          onLockChange={handleLockChange}
        />
      )}

      {classId && <ImportVeyonKeyDialog open={importKeyOpen} onClose={() => setImportKeyOpen(false)} classId={classId} />}
    </Stack>
  );
}
