import { Paper, Typography, InputLabel, TextField, Button, Snackbar, Alert, Box, Autocomplete, CircularProgress } from '@mui/material';
import { Grid, Stack } from '@mui/system';
import AnimateButton from 'components/@extended/AnimateButton';
import { useFormik } from 'formik';
import { useEffect, useState } from 'react';
import { Teacher } from 'types/teacher';
import { Section } from 'types/section';
import * as Yup from 'yup';
import { useIntl } from 'react-intl';
import teacherApi from 'api/teacher';
import { getList as getSections } from 'api/section';
import { HttpStatusCode } from 'axios';
import { useNavigate } from 'react-router-dom';
import useAuth from 'hooks/useAuth';

export default function AddTeacher() {
  const intl = useIntl();
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [sections, setSections] = useState<Section[]>([]);
  const [loadingSections, setLoadingSections] = useState(false);
  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  useEffect(() => {
    const fetchSections = async () => {
      setLoadingSections(true);
      const response = await getSections({ page: 0, size: 1000, status: '1' });
      if (response.statusCode === HttpStatusCode.Ok) {
        setSections(response.data.content);
      } else if (response.statusCode === HttpStatusCode.Unauthorized) {
        logout();
      }
      setLoadingSections(false);
    };
    fetchSections();
  }, [logout]);

  const validationSchema = Yup.object({
    email: Yup.string().email('Email không hợp lệ').required('Email không được phép bỏ trống'),
    code: Yup.string().required('Mã giảng viên không được phép bỏ trống'),
    fullName: Yup.string().required('Tên không được phép bỏ trống'),
    hometown: Yup.string().required('Quê nhà không được phép bỏ trống'),
    phone: Yup.string().required('Số điện thoại không được phép bỏ trống'),
    sectionId: Yup.number().required('Bộ môn không được phép bỏ trống').min(1, 'Bộ môn không được phép bỏ trống'),
    birthday: Yup.string()
      .matches(/^\d{4}-\d{2}-\d{2}$/, 'Ngày sinh phải có định dạng yyyy-MM-dd')
      .required('Ngày sinh không được phép bỏ trống')
  });

  const initialValues: Teacher = {
    id: 0,
    status: 1,
    phone: '',
    fullName: '',
    birthday: '',
    hometown: '',
    email: '',
    code: '',
    sectionId: 0
  };

  const formik = useFormik<Teacher>({
    validationSchema,
    initialValues,
    onSubmit: async (values) => {
      const response = await teacherApi.create(values);

      if (response.statusCode === HttpStatusCode.Ok) {
        navigate('/teacher', {
          state: { alert: { open: true, severity: 'success', message: 'Thêm giảng viên thành công' } }
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
        <Typography variant="h3">Thêm giảng viên</Typography>
      </Box>

      <Paper elevation={0} sx={{ p: 3, border: '1px solid', borderColor: 'divider', ml: 30, mr: 30 }}>
        <form onSubmit={formik.handleSubmit} noValidate>
          <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ mb: 3 }}>
            Thông tin giảng viên
          </Typography>

          <Grid container spacing={2}>
            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel htmlFor="code" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Mã giảng viên
              </InputLabel>
              <TextField
                id="code"
                name="code"
                placeholder="Nhập mã giảng viên"
                size="small"
                fullWidth
                value={formik.values.code}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.code && Boolean(formik.errors.code)}
                helperText={formik.touched.code && formik.errors.code}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel htmlFor="fullName" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Tên giảng viên
              </InputLabel>
              <TextField
                id="fullName"
                name="fullName"
                placeholder="Nhập tên giảng viên"
                size="small"
                fullWidth
                value={formik.values.fullName}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.fullName && Boolean(formik.errors.fullName)}
                helperText={formik.touched.fullName && formik.errors.fullName}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel htmlFor="email" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Email
              </InputLabel>
              <TextField
                id="email"
                name="email"
                type="email"
                placeholder="Nhập email"
                size="small"
                fullWidth
                value={formik.values.email}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.email && Boolean(formik.errors.email)}
                helperText={formik.touched.email && formik.errors.email}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel htmlFor="phone" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Số điện thoại
              </InputLabel>
              <TextField
                id="phone"
                name="phone"
                placeholder="Nhập số điện thoại"
                size="small"
                fullWidth
                value={formik.values.phone}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.phone && Boolean(formik.errors.phone)}
                helperText={formik.touched.phone && formik.errors.phone}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel htmlFor="birthday" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Ngày sinh
              </InputLabel>
              <TextField
                id="birthday"
                name="birthday"
                type="date"
                size="small"
                fullWidth
                value={formik.values.birthday}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.birthday && Boolean(formik.errors.birthday)}
                helperText={formik.touched.birthday && formik.errors.birthday}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel htmlFor="hometown" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Quê quán
              </InputLabel>
              <TextField
                id="hometown"
                name="hometown"
                placeholder="Nhập quê quán"
                size="small"
                fullWidth
                value={formik.values.hometown}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.hometown && Boolean(formik.errors.hometown)}
                helperText={formik.touched.hometown && formik.errors.hometown}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel htmlFor="sectionId" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Bộ môn
              </InputLabel>
              <Autocomplete
                id="sectionId"
                options={sections}
                loading={loadingSections}
                getOptionLabel={(option) => option.name || ''}
                value={sections.find((s) => s.id === formik.values.sectionId) || null}
                onChange={(_, newValue) => {
                  formik.setFieldValue('sectionId', newValue ? newValue.id : 0);
                }}
                onBlur={formik.handleBlur}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    placeholder="Chọn bộ môn"
                    size="small"
                    error={formik.touched.sectionId && Boolean(formik.errors.sectionId)}
                    helperText={formik.touched.sectionId && formik.errors.sectionId}
                    InputProps={{
                      ...params.InputProps,
                      endAdornment: (
                        <>
                          {loadingSections ? <CircularProgress color="inherit" size={20} /> : null}
                          {params.InputProps.endAdornment}
                        </>
                      )
                    }}
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
                    onClick={() => navigate('/teacher')}
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
                    Thêm
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
