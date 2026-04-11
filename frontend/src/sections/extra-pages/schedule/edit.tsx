import { Paper, Typography, InputLabel, TextField, Button, Snackbar, Alert, Box, Autocomplete } from '@mui/material';
import { Grid, Stack } from '@mui/system';
import AnimateButton from 'components/@extended/AnimateButton';
import { useFormik } from 'formik';
import { useEffect, useState } from 'react';
import { Schedule } from 'types/schedule';
import * as Yup from 'yup';
import { useIntl } from 'react-intl';
import { getById, update } from 'api/schedule';
import { HttpStatusCode } from 'axios';
import { useNavigate, useParams } from 'react-router-dom';
import useAuth from 'hooks/useAuth';
import { ALL_DAYS, ALL_PERIODS } from 'utils/schedule';

export default function EditSchedule() {
  const intl = useIntl();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { logout } = useAuth();
  const [initialSchedule, setInitialSchedule] = useState<Schedule>({
    id: 0,
    status: 1,
    name: '',
    daysOfWeek: '',
    periods: '',
    startTime: '',
    endTime: ''
  });

  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  useEffect(() => {
    const fetchData = async () => {
      if (id) {
        const response = await getById(Number(id));
        if (response.statusCode === HttpStatusCode.Ok) {
          setInitialSchedule(response.data);
        } else if (response.statusCode === HttpStatusCode.Unauthorized) {
          logout();
        } else {
          setAlert({ open: true, message: intl.formatMessage({ id: 'unknown-error' }), severity: 'error' });
        }
      }
    };

    fetchData();
  }, [id, intl, logout]);

  const validationSchema = Yup.object({
    name: Yup.string().required('Tên lịch học không được phép bỏ trống'),
    daysOfWeek: Yup.string().required('Ngày trong tuần không được phép bỏ trống'),
    periods: Yup.string()
      .required('Tiết học không được phép bỏ trống')
      .matches(/^([1-9]|1[0-2])(,[1-9]|,1[0-2])*$/, 'Định dạng tiết học không hợp lệ.')
  });

  const formik = useFormik<Schedule>({
    enableReinitialize: true,
    validationSchema,
    initialValues: initialSchedule,
    onSubmit: async (values) => {
      const response = await update(values, Number(id));

      if (response.statusCode === HttpStatusCode.Ok) {
        navigate('/schedule', {
          state: { alert: { open: true, severity: 'success', message: 'Cập nhật ca học thành công' } }
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

  return (
    <Box>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <Typography variant="h3">Cập nhật ca học</Typography>
      </Box>

      <Paper elevation={0} sx={{ p: 3, border: '1px solid', borderColor: 'divider', ml: 30, mr: 30 }}>
        <form onSubmit={formik.handleSubmit} noValidate>
          <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ mb: 3 }}>
            Thông tin ca học
          </Typography>

          <Grid container spacing={2}>
            <Grid size={{ xs: 12 }}>
              <InputLabel htmlFor="name" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Tên ca học
              </InputLabel>
              <TextField
                id="name"
                name="name"
                placeholder="Nhập tên ca học"
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
              <InputLabel htmlFor="daysOfWeek" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Ngày trong tuần
              </InputLabel>
              <Autocomplete
                multiple
                id="daysOfWeek-autocomplete"
                options={ALL_DAYS}
                getOptionLabel={(option) => option.label}
                value={ALL_DAYS.filter((day) =>
                  formik.values.daysOfWeek
                    .split(',')
                    .filter((d) => d !== '')
                    .includes(day.value)
                )}
                onChange={(_, newValue) => {
                  const values = newValue.map((item) => item.value).join(',');
                  formik.setFieldValue('daysOfWeek', values);
                }}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    placeholder="Chọn ngày trong tuần"
                    size="small"
                    error={formik.touched.daysOfWeek && Boolean(formik.errors.daysOfWeek)}
                    helperText={formik.touched.daysOfWeek && formik.errors.daysOfWeek}
                  />
                )}
              />
            </Grid>

            <Grid size={{ xs: 12 }}>
              <InputLabel htmlFor="periods" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Tiết học
              </InputLabel>
              <Autocomplete
                multiple
                id="periods-autocomplete"
                options={ALL_PERIODS}
                getOptionLabel={(option) => option.label}
                value={ALL_PERIODS.filter((period) =>
                  formik.values.periods
                    .split(',')
                    .filter((d) => d !== '')
                    .includes(period.value)
                )}
                onChange={(_, newValue) => {
                  const values = newValue.map((item) => item.value).join(',');
                  formik.setFieldValue('periods', values);
                }}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    placeholder="Chọn tiết học"
                    size="small"
                    error={formik.touched.periods && Boolean(formik.errors.periods)}
                    helperText={formik.touched.periods && formik.errors.periods}
                  />
                )}
              />
            </Grid>
          </Grid>

          <Grid>
            <Grid size={12} sx={{ p: 0, m: 0 }}>
              <Stack direction="row" sx={{ justifyContent: 'flex-end', mt: 3 }}>
                <AnimateButton>
                  <Button
                    onClick={() => navigate('/schedule')}
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

                <AnimateButton>
                  <Button variant="contained" type="submit">
                    Cập nhật
                  </Button>
                </AnimateButton>
              </Stack>
            </Grid>
          </Grid>
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
