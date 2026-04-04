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
import { getById } from 'api/teacher';
import { Teacher } from 'types/teacher';
import AnimateButton from 'components/@extended/AnimateButton';

// ==============================|| TEACHER DETAIL PAGE ||============================== //

export default function DetailTeacher() {
  const { id } = useParams<{ id: string }>();
  const { logout, user } = useAuth();
  const intl = useIntl();
  const navigate = useNavigate();
  const [teacherDetail, setTeacher] = useState<Teacher>({
    id: 0,
    status: 0,
    phone: '',
    fullName: '',
    birthday: '',
    email: '',
    hometown: '',
    code: '',
    sectionId: 0
  });
  const [hasEditPermission, setHasEditPermission] = useState(false);

  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  useEffect(() => {
    const fetchTeacher = async () => {
      if (id) {
        const response = await getById(Number(id));

        if (response.statusCode === HttpStatusCode.Ok) {
          setTeacher(response.data);
        } else if (response.statusCode === HttpStatusCode.Unauthorized) {
          logout();
        } else {
          setAlert({ open: true, message: intl.formatMessage({ id: 'unknown-error' }), severity: 'error' });
        }
      }
    };

    fetchTeacher();
  }, [id, intl, logout]);

  useEffect(() => {
    if ([1].includes(user?.roleId ?? 0)) {
      setHasEditPermission(true);
    }
  }, [user?.roleId]);

  return (
    <Box>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <Typography variant="h3">Chi tiết thông tin giảng viên</Typography>
      </Box>

      <Paper elevation={0} sx={{ p: 3, border: '1px solid', borderColor: 'divider', ml: 30, mr: 30 }}>
        <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ mb: 3 }}>
          Thông tin giảng viên
        </Typography>

        <Grid container spacing={3}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Mã giảng viên</InputLabel>
              <Typography variant="body1">{teacherDetail.code}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Tên giảng viên</InputLabel>
              <Typography variant="body1">{teacherDetail.fullName}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Email</InputLabel>
              <Typography variant="body1">{teacherDetail.email}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Số điện thoại</InputLabel>
              <Typography variant="body1">{teacherDetail.phone}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Ngày sinh</InputLabel>
              <Typography variant="body1">{teacherDetail.birthday}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Quê quán</InputLabel>
              <Typography variant="body1">{teacherDetail.hometown}</Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Bộ môn</InputLabel>
              <Typography variant="body1">{teacherDetail.sectionName}</Typography>
            </Stack>
          </Grid>

          {teacherDetail.rawPassword && teacherDetail.rawPassword !== '' && (
            <Grid size={{ xs: 12, sm: 6 }}>
              <Stack sx={{ gap: 1 }}>
                <InputLabel>Mật khẩu</InputLabel>
                <Typography variant="body1" sx={{ fontWeight: 600, color: 'primary.main' }}>
                  {teacherDetail.rawPassword}
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
                  onClick={() => navigate('/teacher')}
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
                  <Button variant="contained" onClick={() => navigate(`/teacher/edit/${id}`)}>
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
