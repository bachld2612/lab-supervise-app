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
import { getById } from 'api/semester';
import { Semester } from 'types/semester';
import AnimateButton from 'components/@extended/AnimateButton';
import formatDate from 'utils/formatDate';

// ==============================|| SEMESTER DETAIL PAGE ||============================== //

export default function DetailSemester() {
  const { id } = useParams<{ id: string }>();
  const { logout, user } = useAuth();
  const intl = useIntl();
  const navigate = useNavigate();
  const [semesterDetail, setSemesterDetail] = useState<Semester>({
    id: 0,
    status: 0,
    name: '',
    studyYear: '',
    startDate: '',
    endDate: ''
  });
  const [hasEditPermission, setHasEditPermission] = useState(false);

  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  useEffect(() => {
    const fetchSemester = async () => {
      if (id) {
        const response = await getById(Number(id));

        if (response.statusCode === HttpStatusCode.Ok) {
          setSemesterDetail(response.data);
        } else if (response.statusCode === HttpStatusCode.Unauthorized) {
          logout();
        } else {
          setAlert({ open: true, message: intl.formatMessage({ id: 'unknown-error' }), severity: 'error' });
        }
      }
    };

    fetchSemester();
  }, [id, intl, logout]);

  useEffect(() => {
    if ([1].includes(user?.roleId ?? 0)) {
      setHasEditPermission(true);
    }
  }, [user?.roleId]);

  return (
    <Box>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <Typography variant="h3">Chi tiết thông tin học kỳ</Typography>
      </Box>

      <Paper elevation={0} sx={{ p: 4, border: '1px solid', borderColor: 'divider', maxWidth: 800, mx: 'auto' }}>
        <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ mb: 3 }}>
          Thông tin học kỳ
        </Typography>

        <Grid container spacing={3}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Tên học kỳ</InputLabel>
              <Typography variant="body1" fontWeight={500}>
                {semesterDetail.name}
              </Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Năm học</InputLabel>
              <Typography variant="body1" fontWeight={500}>
                {semesterDetail.studyYear}
              </Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Ngày bắt đầu</InputLabel>
              <Typography variant="body1" fontWeight={500}>
                {formatDate(semesterDetail.startDate)}
              </Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Ngày kết thúc</InputLabel>
              <Typography variant="body1" fontWeight={500}>
                {formatDate(semesterDetail.endDate)}
              </Typography>
            </Stack>
          </Grid>
        </Grid>

        <Stack direction="row" sx={{ justifyContent: 'flex-end', mt: 4, gap: 2 }}>
          <AnimateButton>
            <Button
              onClick={() => navigate('/semester')}
              variant="contained"
              sx={{
                bgcolor: 'secondary.main',
                color: 'white',
                '&:hover': { bgcolor: 'secondary.dark' }
              }}
            >
              Trở về
            </Button>
          </AnimateButton>

          {hasEditPermission && (
            <AnimateButton>
              <Button variant="contained" onClick={() => navigate(`/semester/edit/${id}`)}>
                Cập nhật
              </Button>
            </AnimateButton>
          )}
        </Stack>
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
