import { useEffect, useState } from 'react';

import { Alert, Box, Card, CardContent, Chip, CircularProgress, Divider, Grid, Snackbar, Stack, Typography } from '@mui/material';
import { getTeacherClasses } from 'api/class';
import MainCard from 'components/MainCard';
import useAuth from 'hooks/useAuth';
import { Book1, Calendar, People, Timer1 } from 'iconsax-reactjs';
import { useIntl } from 'react-intl';
import { useNavigate } from 'react-router';
import StudentListDialog from 'sections/extra-pages/class/max-student-dialog';
import { type Classes } from 'types/classes';
import { HttpStatusCode } from 'axios';

// ==============================|| TEACHER DASHBOARD PAGE ||============================== //

export default function TeacherDashboardPage() {
  const { logout } = useAuth();
  const intl = useIntl();
  const navigate = useNavigate();

  const [classes, setClasses] = useState<Classes[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedClass, setSelectedClass] = useState<Classes | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  useEffect(() => {
    const fetchClasses = async () => {
      setLoading(true);
      const response = await getTeacherClasses();

      if (response.statusCode === HttpStatusCode.Ok) {
        setClasses(response.data ?? []);
      } else if (response.statusCode === HttpStatusCode.Unauthorized) {
        logout();
      } else {
        setAlert({ open: true, message: intl.formatMessage({ id: 'unknown-error' }), severity: 'error' });
      }

      setLoading(false);
    };

    fetchClasses();
  }, [intl, logout]);

  const handleOpenStudents = (e: React.MouseEvent, classItem: Classes) => {
    e.stopPropagation();
    setSelectedClass(classItem);
    setDialogOpen(true);
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight={400}>
        <CircularProgress />
      </Box>
    );
  }

  const activeCount = classes.filter((c) => c.studyStatus === 1).length;

  return (
    <Stack sx={{ p: 0 }}>
      <Snackbar
        open={alert.open}
        autoHideDuration={3000}
        onClose={() => setAlert({ ...alert, open: false })}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Alert
          severity={alert.severity}
          variant="filled"
          sx={{ width: '100%', borderRadius: 2, fontSize: 15, textAlign: 'center', py: 1.5, px: 2 }}
        >
          {alert.message}
        </Alert>
      </Snackbar>

      <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ gap: 2, justifyContent: 'space-between', pb: 3 }}>
        <Stack direction="row" spacing={2} alignItems="center">
          <Typography variant="h3" gutterBottom>
            Danh sách lớp học của tôi
          </Typography>
          <Chip label={`${classes.length} lớp`} color="primary" size="small" variant="outlined" />
          {activeCount > 0 && <Chip label={`${activeCount} đang học`} color="success" size="small" />}
        </Stack>
      </Stack>

      <MainCard content={false}>
        <Box sx={{ p: 3 }}>
          {classes.length === 0 ? (
            <Box textAlign="center" py={6}>
              <Typography color="text.secondary" variant="h6">
                Không có lớp học nào trong học kỳ hiện tại
              </Typography>
              <Typography color="text.disabled" variant="body2" mt={1}>
                Các lớp sẽ hiển thị khi nằm trong thời gian học kỳ đang diễn ra
              </Typography>
            </Box>
          ) : (
            <Grid container spacing={3}>
              {classes.map((cls) => {
                const isFull = cls.currentStudent >= cls.maxStudent && cls.maxStudent > 0;
                const isActive = cls.studyStatus === 1;

                return (
                  <Grid key={cls.id} size={{ xs: 12, sm: 6, md: 4 }}>
                    <Card
                      onClick={() => navigate(`/teacher/class/${cls.id}/tracking`, { state: { studyStatus: cls.studyStatus } })}
                      sx={{
                        height: '100%',
                        border: '1px solid',
                        borderColor: isActive ? 'success.main' : 'warning.main',
                        borderRadius: 2,
                        cursor: 'pointer',
                        transition: 'box-shadow 0.2s',
                        '&:hover': { boxShadow: 4 }
                      }}
                    >
                      <CardContent sx={{ p: 2.5, '&:last-child': { pb: 2.5 } }}>
                        <Stack spacing={2}>
                          <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                            <Typography variant="h6" fontWeight="bold" sx={{ flex: 1, pr: 1 }}>
                              {cls.name}
                            </Typography>
                            <Chip
                              label={isActive ? 'Đang học' : 'Sắp diễn ra'}
                              color={isActive ? 'success' : 'warning'}
                              size="small"
                            />
                          </Stack>

                          <Divider />

                          <Stack spacing={1.5}>
                            <Stack direction="row" spacing={1} alignItems="center">
                              <Book1 size={16} />
                              <Typography variant="body2" color="text.secondary">
                                {cls.subjectName}
                              </Typography>
                            </Stack>

                            <Stack direction="row" spacing={1} alignItems="center">
                              <Calendar size={16} />
                              <Typography variant="body2" color="text.secondary">
                                {cls.scheduleName}
                              </Typography>
                            </Stack>

                            <Stack direction="row" spacing={1} alignItems="center">
                              <Timer1 size={16} />
                              <Typography variant="body2" color="text.secondary">
                                {cls.startDate} → {cls.endDate}
                              </Typography>
                            </Stack>

                            <Typography variant="caption" color="text.disabled">
                              {cls.semesterName}&nbsp;·&nbsp;{cls.sessionNumber} buổi học
                            </Typography>
                          </Stack>

                          <Divider />

                          <Stack direction="row" justifyContent="space-between" alignItems="center">
                            <Stack direction="row" spacing={0.75} alignItems="center">
                              <People size={16} />
                              <Typography variant="body2" color="text.secondary">
                                Sĩ số
                              </Typography>
                            </Stack>
                            <Typography
                              variant="body1"
                              fontWeight="bold"
                              onClick={(e) => handleOpenStudents(e, cls)}
                              sx={{
                                color: isFull ? 'error.main' : 'primary.main',
                                cursor: 'pointer',
                                '&:hover': { textDecoration: 'underline' }
                              }}
                            >
                              {cls.currentStudent}/{cls.maxStudent}
                            </Typography>
                          </Stack>
                        </Stack>
                      </CardContent>
                    </Card>
                  </Grid>
                );
              })}
            </Grid>
          )}
        </Box>
      </MainCard>

      <StudentListDialog open={dialogOpen} onClose={() => setDialogOpen(false)} classItem={selectedClass} />
    </Stack>
  );
}
