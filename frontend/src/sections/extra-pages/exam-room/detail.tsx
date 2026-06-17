import Grid from '@mui/material/Grid';
import InputLabel from '@mui/material/InputLabel';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { Snackbar, Alert, Paper, Box, Button } from '@mui/material';
import { HttpStatusCode } from 'axios';
import { useEffect, useState } from 'react';
import useAuth from 'hooks/useAuth';
import { useNavigate, useParams } from 'react-router';
import { useIntl } from 'react-intl';
import { getById } from 'api/exam-room';
import { ExamRoom } from 'types/exam-room';
import AnimateButton from 'components/@extended/AnimateButton';
import formatDate, { formatTimeWithoutSecond } from 'utils/formatDate';
import { parsePeriodValues } from 'sections/extra-pages/exam-room/form-helpers';

function formatPeriods(periods?: string) {
  const values = parsePeriodValues(periods);
  if (values.length === 0) return '—';
  if (values.length === 1) return `Tiết ${values[0]}`;
  return `Tiết ${values[0]} - Tiết ${values[values.length - 1]}`;
}

// ==============================|| EXAM ROOM DETAIL PAGE ||============================== //

export default function DetailExamRoom() {
  const { id } = useParams<{ id: string }>();
  const { logout, user } = useAuth();
  const intl = useIntl();
  const navigate = useNavigate();
  const [examRoom, setExamRoom] = useState<ExamRoom | null>(null);
  const [hasEditPermission, setHasEditPermission] = useState(false);

  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  useEffect(() => {
    const fetchExamRoom = async () => {
      if (id) {
        const response = await getById(Number(id));
        if (response.statusCode === HttpStatusCode.Ok) {
          setExamRoom(response.data);
        } else if (response.statusCode === HttpStatusCode.Unauthorized) {
          logout();
        } else {
          setAlert({ open: true, message: intl.formatMessage({ id: 'unknown-error' }), severity: 'error' });
        }
      }
    };

    fetchExamRoom();
  }, [id, intl, logout]);

  useEffect(() => {
    if ([1].includes(user?.roleId ?? 0)) {
      setHasEditPermission(true);
    }
  }, [user?.roleId]);

  return (
    <Box>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <Typography variant="h3">Chi tiết thông tin phòng thi</Typography>
      </Box>

      <Paper elevation={0} sx={{ p: 3, border: '1px solid', borderColor: 'divider', ml: 30, mr: 30 }}>
        <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ mb: 3 }}>
          Thông tin phòng thi
        </Typography>

        <Grid container spacing={3}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Mã phòng thi</InputLabel>
              <Typography variant="body1">{examRoom?.code}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Sĩ số</InputLabel>
              <Typography variant="body1">
                {examRoom?.currentStudent} / {examRoom?.maxStudent}
              </Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Môn học</InputLabel>
              <Typography variant="body1">{examRoom?.subjectName}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Phòng thi</InputLabel>
              <Typography variant="body1">{examRoom?.roomName || '—'}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Giảng viên coi thi 1</InputLabel>
              <Typography variant="body1">{examRoom?.teacher1Name}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Giảng viên coi thi 2</InputLabel>
              <Typography variant="body1">{examRoom?.teacher2Name}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Học kỳ</InputLabel>
              <Typography variant="body1">{examRoom?.semesterName}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Ngày thi</InputLabel>
              <Typography variant="body1">{formatDate(examRoom?.examDate ?? '')}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Tiết thi</InputLabel>
              <Typography variant="body1">{formatPeriods(examRoom?.periods)}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Giờ thi</InputLabel>
              <Typography variant="body1">
                {formatTimeWithoutSecond(examRoom?.startTime ?? '')} - {formatTimeWithoutSecond(examRoom?.endTime ?? '')}
              </Typography>
            </Stack>
          </Grid>
        </Grid>

        <Grid container spacing={2} sx={{ mt: 3 }}>
          <Grid size={12}>
            <Stack direction="row" sx={{ justifyContent: 'flex-end', gap: 1 }}>
              <AnimateButton>
                <Button
                  onClick={() => navigate('/exam-room')}
                  variant="contained"
                  sx={{ bgcolor: '#7e7e7eff', color: 'white', '&:hover': { bgcolor: '#9a9999ff' } }}
                >
                  Trở về
                </Button>
              </AnimateButton>

              {hasEditPermission && (
                <AnimateButton>
                  <Button variant="contained" onClick={() => navigate(`/exam-room/edit/${id}`)}>
                    Cập nhật
                  </Button>
                </AnimateButton>
              )}
            </Stack>
          </Grid>
        </Grid>
      </Paper>

      <Snackbar
        open={alert.open}
        autoHideDuration={3000}
        onClose={() => setAlert({ ...alert, open: false })}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Alert onClose={() => setAlert({ ...alert, open: false })} severity={alert.severity} variant="filled" sx={{ width: '100%' }}>
          {alert.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
