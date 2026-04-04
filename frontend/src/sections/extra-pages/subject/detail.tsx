import Grid from '@mui/material/Grid';
import InputLabel from '@mui/material/InputLabel';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { Snackbar, Alert, Paper, Box, Button, Chip } from '@mui/material';
import { HttpStatusCode } from 'axios';
import { useEffect, useState } from 'react';
import useAuth from 'hooks/useAuth';
import { useNavigate, useParams } from 'react-router';
import { useIntl } from 'react-intl';
import { getById } from 'api/subject';
import { type Subject } from 'types/subject';
import AnimateButton from 'components/@extended/AnimateButton';

// ==============================|| SUBJECT DETAIL PAGE ||============================== //

export default function DetailSubject() {
  const { id } = useParams<{ id: string }>();
  const { logout, user } = useAuth();
  const intl = useIntl();
  const navigate = useNavigate();
  const [subjectDetail, setSubjectDetail] = useState<Subject>({
    id: 0,
    status: 0,
    name: '',
    code: '',
    creditNumber: 0,
    sectionId: 0,
    sectionName: ''
  });
  const [hasEditPermission, setHasEditPermission] = useState(false);

  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  useEffect(() => {
    const fetchSubject = async () => {
      if (id) {
        const response = await getById(Number(id));

        if (response.statusCode === HttpStatusCode.Ok) {
          setSubjectDetail(response.data);
        } else if (response.statusCode === HttpStatusCode.Unauthorized) {
          logout();
        } else {
          setAlert({ open: true, message: intl.formatMessage({ id: 'unknown-error' }), severity: 'error' });
        }
      }
    };

    fetchSubject();
  }, [id, intl, logout]);

  useEffect(() => {
    if ([1].includes(user?.roleId ?? 0)) {
      setHasEditPermission(true);
    }
  }, [user?.roleId]);

  return (
    <Box>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <Typography variant="h3">Chi tiết thông tin môn học</Typography>
      </Box>

      <Paper elevation={0} sx={{ p: 4, border: '1px solid', borderColor: 'divider', maxWidth: 800, mx: 'auto' }}>
        <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ mb: 3 }}>
          Thông tin môn học
        </Typography>

        <Grid container spacing={3}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Mã môn học</InputLabel>
              <Typography variant="body1" fontWeight={500}>
                {subjectDetail.code}
              </Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Tên môn học</InputLabel>
              <Typography variant="body1" fontWeight={500}>
                {subjectDetail.name}
              </Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Số tín chỉ</InputLabel>
              <Typography variant="body1" fontWeight={500}>
                {subjectDetail.creditNumber}
              </Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Bộ môn</InputLabel>
              <Typography variant="body1" fontWeight={500}>
                {subjectDetail.sectionName}
              </Typography>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, sm: 6 }}>
            <Stack sx={{ gap: 1 }}>
              <InputLabel>Trạng thái</InputLabel>
              <Box>
                <Chip
                  label={subjectDetail.status === 1 ? 'Hoạt động' : 'Dừng hoạt động'}
                  color={subjectDetail.status === 1 ? 'success' : 'error'}
                  size="small"
                />
              </Box>
            </Stack>
          </Grid>
        </Grid>

        <Stack direction="row" sx={{ justifyContent: 'flex-end', mt: 4, gap: 2 }}>
          <AnimateButton>
            <Button
              onClick={() => navigate('/subject')}
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
              <Button variant="contained" onClick={() => navigate(`/subject/edit/${id}`)}>
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
