import { Alert, Box, Card, CardContent, Chip, CircularProgress, Divider, Grid, Snackbar, Stack, Typography } from '@mui/material';
import MainCard from 'components/MainCard';
import { useState } from 'react';
import { useClassTracking } from 'hooks/useClassTracking';
import { ArrowLeft, Monitor, Timer1, Wifi } from 'iconsax-reactjs';
import { useLocation, useNavigate, useParams } from 'react-router';

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

  const { students, loading } = useClassTracking(classId, (message) => {
    setAlert({ open: true, message, severity: 'error' });
  });

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
            onClick={() => navigate('/dashboard/teacher')}
            sx={{ cursor: 'pointer', display: 'flex', alignItems: 'center', color: 'text.secondary', '&:hover': { color: 'primary.main' } }}
          >
            <ArrowLeft size={20} />
          </Box>
          <Typography variant="h3">Theo dõi sinh viên</Typography>
          <Chip label={`${students.length} sinh viên`} color="primary" size="small" variant="outlined" />
        </Stack>

        <Chip icon={chip.icon} label={chip.label} color={chip.color} size="small" variant="outlined" />
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
              {students.map((student) => (
                <Grid key={student.studentId} size={{ xs: 12, sm: 6, md: 4 }}>
                  <Card
                    sx={{
                      height: '100%',
                      border: '1px solid',
                      borderColor: 'divider',
                      borderRadius: 2,
                      display: 'flex',
                      flexDirection: 'column'
                    }}
                  >
                    <CardContent sx={{ p: 2.5, '&:last-child': { pb: 2.5 }, display: 'flex', flexDirection: 'column', height: '100%' }}>
                      <Stack spacing={1.5} sx={{ flex: 1, minHeight: 0 }}>
                        <Stack spacing={0.5}>
                          <Typography variant="h6" fontWeight="bold">
                            {student.fullName}
                          </Typography>
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
                </Grid>
              ))}
            </Grid>
          )}
        </Box>
      </MainCard>
    </Stack>
  );
}
