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
import { getById } from 'api/student';
import { Student } from 'types/student';
import AnimateButton from 'components/@extended/AnimateButton';

export default function DetailStudent() {
  const { id } = useParams<{ id: string }>();
  const { logout, user } = useAuth();
  const intl = useIntl();
  const navigate = useNavigate();
  const [studentDetail, setStudent] = useState<Student>({
    id: 0,
    status: 0,
    phone: '',
    fullName: '',
    birthday: '',
    email: '',
    hometown: '',
    code: '',
    manageClassId: 0,
    manageClassName: ''
  });
  const [hasEditPermission, setHasEditPermission] = useState(false);

  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  useEffect(() => {
    const fetchStudent = async () => {
      if (id) {
        const response = await getById(Number(id));

        if (response.statusCode === HttpStatusCode.Ok) {
          setStudent(response.data);
        } else if (response.statusCode === HttpStatusCode.Unauthorized) {
          logout();
        } else {
          setAlert({ open: true, message: intl.formatMessage({ id: 'unknown-error' }), severity: 'error' });
        }
      }
    };

    fetchStudent();
  }, [id, intl, logout]);

  useEffect(() => {
    if ([1].includes(user?.roleId ?? 0)) {
      setHasEditPermission(true);
    }
  }, [user?.roleId]);

  return (
    <Box>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <Typography variant="h3">Chi tiết thông tin sinh viên</Typography>
      </Box>

      <Paper elevation={0} sx={{ p: 3, border: '1px solid', borderColor: 'divider', ml: 30, mr: 30 }}>
        <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ mb: 3 }}>
          Thông tin sinh viên
        </Typography>

        <Grid container spacing={3}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Mã sinh viên</InputLabel>
              <Typography variant="body1">{studentDetail.code}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Tên sinh viên</InputLabel>
              <Typography variant="body1">{studentDetail.fullName}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Email</InputLabel>
              <Typography variant="body1">{studentDetail.email}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Số điện thoại</InputLabel>
              <Typography variant="body1">{studentDetail.phone}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Ngày sinh</InputLabel>
              <Typography variant="body1">{studentDetail.birthday}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Quê quán</InputLabel>
              <Typography variant="body1">{studentDetail.hometown}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Lớp quản lý</InputLabel>
              <Typography variant="body1">{studentDetail.manageClassName}</Typography>
            </Stack>
          </Grid>

          {studentDetail.rawPassword && studentDetail.rawPassword !== '' && (
            <Grid size={{ xs: 12, sm: 6 }}>
              <Stack sx={{ gap: 1 }}>
                <InputLabel>Mật khẩu</InputLabel>
                <Typography variant="body1" sx={{ fontWeight: 600, color: 'primary.main' }}>
                  {studentDetail.rawPassword}
                </Typography>
              </Stack>
            </Grid>
          )}
        </Grid>

        <Grid container spacing={2} sx={{ mt: 3 }}>
          <Grid size={12}>
            <Stack direction="row" sx={{ justifyContent: 'flex-end', gap: 1 }}>
              <AnimateButton>
                <Button
                  onClick={() => navigate('/student')}
                  variant="contained"
                  sx={{
                    mr: 1,
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
                  <Button variant="contained" onClick={() => navigate(`/student/edit/${id}`)}>
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
