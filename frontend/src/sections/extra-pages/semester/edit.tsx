import { Paper, Typography, InputLabel, TextField, Button, Snackbar, Alert, Box } from '@mui/material';
import { Grid, Stack } from '@mui/system';
import AnimateButton from 'components/@extended/AnimateButton';
import { useFormik } from 'formik';
import { useEffect, useState } from 'react';
import { Semester } from 'types/semester';
import * as Yup from 'yup';
import { useIntl } from 'react-intl';
import { getById, update } from 'api/semester';
import { HttpStatusCode } from 'axios';
import { useNavigate, useParams } from 'react-router-dom';
import useAuth from 'hooks/useAuth';

export default function EditSemester() {
  const intl = useIntl();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { logout } = useAuth();
  const [loading, setLoading] = useState(true);

  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  const validationSchema = Yup.object({
    name: Yup.string().required('Tên học kỳ không được phép bỏ trống'),
    startYear: Yup.string()
      .matches(/^\d{4}$/, 'Năm bắt đầu phải là 4 số')
      .required('Năm bắt đầu không được phép bỏ trống'),
    endYear: Yup.string()
      .matches(/^\d{4}$/, 'Năm kết thúc phải là 4 số')
      .required('Năm kết thúc không được phép bỏ trống'),
    startDate: Yup.string()
      .matches(/^\d{4}-\d{2}-\d{2}$/, 'Ngày bắt đầu phải có định dạng yyyy-MM-dd')
      .required('Ngày bắt đầu không được phép bỏ trống'),
    endDate: Yup.string()
      .matches(/^\d{4}-\d{2}-\d{2}$/, 'Ngày kết thúc phải có định dạng yyyy-MM-dd')
      .required('Ngày kết thúc không được phép bỏ trống')
  });

  const formik = useFormik({
    initialValues: {
      name: '',
      startYear: '',
      endYear: '',
      startDate: '',
      endDate: '',
      status: 1
    },
    enableReinitialize: true,
    validationSchema,
    onSubmit: async (values) => {
      const semesterData: Semester = {
        id: Number(id),
        name: values.name,
        studyYear: `${values.startYear} - ${values.endYear}`,
        startDate: values.startDate,
        endDate: values.endDate,
        status: values.status
      };

      const response = await update(semesterData, Number(id));

      if (response.statusCode === HttpStatusCode.Ok) {
        navigate('/semester', {
          state: { alert: { open: true, severity: 'success', message: 'Cập nhật học kỳ thành công' } }
        });
      } else if (response.statusCode === HttpStatusCode.BadRequest) {
        const errors = Object.entries(response.data);
        if (errors.length > 0) {
          const firstError = errors[0][1];
          setAlert({ open: true, message: firstError as string, severity: 'error' });
        }
      } else if (response.statusCode === HttpStatusCode.Unauthorized) {
        logout();
      } else if (response.statusCode === HttpStatusCode.UnprocessableEntity) {
        setAlert({ open: true, message: response.message, severity: 'error' });
      } else {
        setAlert({ open: true, message: intl.formatMessage({ id: 'unknown-error' }), severity: 'error' });
      }
    }
  });

  useEffect(() => {
    const fetchSemester = async () => {
      if (id) {
        setLoading(true);
        const response = await getById(Number(id));

        if (response.statusCode === HttpStatusCode.Ok) {
          const data: Semester = response.data;
          const [start, end] = data.studyYear.split(' - ');
          formik.setValues({
            name: data.name,
            startYear: start || '',
            endYear: end || '',
            startDate: data.startDate,
            endDate: data.endDate,
            status: data.status
          });
        } else if (response.statusCode === HttpStatusCode.Unauthorized) {
          logout();
        } else {
          setAlert({ open: true, message: intl.formatMessage({ id: 'unknown-error' }), severity: 'error' });
        }
        setLoading(false);
      }
    };

    fetchSemester();
  }, [id, intl, logout]);

  if (loading) return null;

  return (
    <Box>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <Typography variant="h3">Cập nhật học kỳ</Typography>
      </Box>

      <Paper elevation={0} sx={{ p: 4, border: '1px solid', borderColor: 'divider', maxWidth: 800, mx: 'auto' }}>
        <form onSubmit={formik.handleSubmit} noValidate>
          <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ mb: 3 }}>
            Thông tin học kỳ
          </Typography>

          <Grid container spacing={3}>
            <Grid size={{ xs: 12 }}>
              <InputLabel htmlFor="name" required sx={{ mb: 1, '& .MuiInputLabel-asterisk': { color: 'error.main' } }}>
                Tên học kỳ
              </InputLabel>
              <TextField
                id="name"
                name="name"
                placeholder="Học kỳ 1"
                size="small"
                fullWidth
                value={formik.values.name}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.name && Boolean(formik.errors.name)}
                helperText={formik.touched.name && formik.errors.name}
              />
            </Grid>

            <Grid size={{ xs: 12 }}>
              <InputLabel required sx={{ mb: 1, '& .MuiInputLabel-asterisk': { color: 'error.main' } }}>
                Năm học
              </InputLabel>
              <Stack direction="row" alignItems="center" spacing={2}>
                <TextField
                  id="startYear"
                  name="startYear"
                  placeholder="Năm bắt đầu"
                  size="small"
                  fullWidth
                  value={formik.values.startYear}
                  onChange={formik.handleChange}
                  onBlur={formik.handleBlur}
                  error={formik.touched.startYear && Boolean(formik.errors.startYear)}
                  helperText={formik.touched.startYear && formik.errors.startYear}
                />
                <Typography variant="h5">-</Typography>
                <TextField
                  id="endYear"
                  name="endYear"
                  placeholder="Năm kết thúc"
                  size="small"
                  fullWidth
                  value={formik.values.endYear}
                  onChange={formik.handleChange}
                  onBlur={formik.handleBlur}
                  error={formik.touched.endYear && Boolean(formik.errors.endYear)}
                  helperText={formik.touched.endYear && formik.errors.endYear}
                />
              </Stack>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel htmlFor="startDate" required sx={{ mb: 1, '& .MuiInputLabel-asterisk': { color: 'error.main' } }}>
                Ngày bắt đầu
              </InputLabel>
              <TextField
                id="startDate"
                name="startDate"
                type="date"
                size="small"
                fullWidth
                value={formik.values.startDate}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.startDate && Boolean(formik.errors.startDate)}
                helperText={formik.touched.startDate && formik.errors.startDate}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel htmlFor="endDate" required sx={{ mb: 1, '& .MuiInputLabel-asterisk': { color: 'error.main' } }}>
                Ngày kết thúc
              </InputLabel>
              <TextField
                id="endDate"
                name="endDate"
                type="date"
                size="small"
                fullWidth
                value={formik.values.endDate}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.endDate && Boolean(formik.errors.endDate)}
                helperText={formik.touched.endDate && formik.errors.endDate}
              />
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

            <AnimateButton>
              <Button variant="contained" type="submit">
                Cập nhật
              </Button>
            </AnimateButton>
          </Stack>
        </form>
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
