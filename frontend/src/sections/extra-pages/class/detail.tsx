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
import { getById } from 'api/class';
import { Classes } from 'types/classes';
import AnimateButton from 'components/@extended/AnimateButton';
import formatDate from 'utils/formatDate';

// ==============================|| CLASS DETAIL PAGE ||============================== //

export default function DetailClass() {
  const { id } = useParams<{ id: string }>();
  const { logout, user } = useAuth();
  const intl = useIntl();
  const navigate = useNavigate();
  const [classDetail, setClassDetail] = useState<Classes>({
    id: 0,
    status: 0,
    name: '',
    maxStudent: 0,
    currentStudent: 0,
    subjectId: 0,
    subjectName: '',
    teacherId: 0,
    teacherName: '',
    scheduleId: 0,
    scheduleName: '',
    startDate: '',
    endDate: '',
    semesterId: 0,
    semesterName: '',
    roomId: 0,
    roomName: ''
  });
  const [hasEditPermission, setHasEditPermission] = useState(false);

  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  useEffect(() => {
    const fetchClass = async () => {
      if (id) {
        const response = await getById(Number(id));

        if (response.statusCode === HttpStatusCode.Ok) {
          setClassDetail(response.data);
        } else if (response.statusCode === HttpStatusCode.Unauthorized) {
          logout();
        } else {
          setAlert({ open: true, message: intl.formatMessage({ id: 'unknown-error' }), severity: 'error' });
        }
      }
    };

    fetchClass();
  }, [id, intl, logout]);

  useEffect(() => {
    if ([1].includes(user?.roleId ?? 0)) {
      setHasEditPermission(true);
    }
  }, [user?.roleId]);

  return (
    <Box>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <Typography variant="h3">Chi tiết thông tin lớp học phần</Typography>
      </Box>

      <Paper elevation={0} sx={{ p: 3, border: '1px solid', borderColor: 'divider', ml: 30, mr: 30 }}>
        <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ mb: 3 }}>
          Thông tin lớp học phần
        </Typography>

        <Grid container spacing={3}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Tên lớp</InputLabel>
              <Typography variant="body1">{classDetail.name}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Sĩ số</InputLabel>
              <Typography variant="body1">
                {classDetail.currentStudent} / {classDetail.maxStudent}
              </Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Môn học</InputLabel>
              <Typography variant="body1">{classDetail.subjectName}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Giảng viên</InputLabel>
              <Typography variant="body1">{classDetail.teacherName}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Lịch học</InputLabel>
              <Typography variant="body1">{classDetail.scheduleName}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Học kỳ</InputLabel>
              <Typography variant="body1">{classDetail.semesterName}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Phòng học</InputLabel>
              <Typography variant="body1">{classDetail.roomName || '—'}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Ngày bắt đầu</InputLabel>
              <Typography variant="body1">{formatDate(classDetail.startDate)}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Ngày kết thúc</InputLabel>
              <Typography variant="body1">{formatDate(classDetail.endDate)}</Typography>
            </Stack>
          </Grid>
        </Grid>

        <Grid container spacing={2} sx={{ mt: 3 }}>
          <Grid size={12}>
            <Stack direction="row" sx={{ justifyContent: 'flex-end', gap: 1 }}>
              <AnimateButton>
                <Button
                  onClick={() => navigate('/class')}
                  variant="contained"
                  sx={{
                    bgcolor: '#7e7e7eff',
                    color: 'white',
                    '&:hover': { bgcolor: '#9a9999ff' }
                  }}
                >
                  Trở về
                </Button>
              </AnimateButton>

              {hasEditPermission && (
                <AnimateButton>
                  <Button variant="contained" onClick={() => navigate(`/class/edit/${id}`)}>
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
