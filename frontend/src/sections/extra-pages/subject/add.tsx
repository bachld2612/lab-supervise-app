import { Paper, Typography, InputLabel, TextField, Button, Snackbar, Alert, Box, Autocomplete } from '@mui/material';
import { Grid, Stack } from '@mui/system';
import AnimateButton from 'components/@extended/AnimateButton';
import { useFormik } from 'formik';
import { useEffect, useState } from 'react';
import { type Subject } from 'types/subject';
import * as Yup from 'yup';
import { useIntl } from 'react-intl';
import { create } from 'api/subject';
import { getList as getSectionList } from 'api/section';
import { HttpStatusCode } from 'axios';
import { useNavigate } from 'react-router-dom';
import useAuth from 'hooks/useAuth';
import { Section } from 'types/section';

export default function AddSubject() {
  const intl = useIntl();
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [sections, setSections] = useState<Section[]>([]);
  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  useEffect(() => {
    const fetchSections = async () => {
      const response = await getSectionList({ page: 0, size: 1000, keyword: '', status: '1' });
      if (response.statusCode === HttpStatusCode.Ok) {
        setSections(response.data.content);
      }
    };
    fetchSections();
  }, []);

  const validationSchema = Yup.object({
    name: Yup.string().required('Tên môn học không được phép bỏ trống'),
    code: Yup.string().required('Mã môn học không được phép bỏ trống'),
    creditNumber: Yup.number()
      .typeError('Số tín chỉ phải là số')
      .integer('Số tín chỉ phải là số nguyên')
      .required('Số tín chỉ không được phép bỏ trống')
      .min(1, 'Số tín chỉ phải lớn hơn hoặc bằng 1'),
    sectionId: Yup.number().required('Bộ môn không được phép bỏ trống').min(1, 'Bộ môn không được phép bỏ trống')
  });

  const formik = useFormik({
    initialValues: {
      name: '',
      code: '',
      creditNumber: 0,
      sectionId: 0,
      status: 1
    },
    validationSchema,
    onSubmit: async (values) => {
      const response = await create(values as unknown as Subject);

      if (response.statusCode === HttpStatusCode.Ok) {
        navigate('/subject', {
          state: { alert: { open: true, severity: 'success', message: 'Thêm môn học thành công' } }
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
        <Typography variant="h3">Thêm môn học</Typography>
      </Box>

      <Paper elevation={0} sx={{ p: 4, border: '1px solid', borderColor: 'divider', maxWidth: 800, mx: 'auto' }}>
        <form onSubmit={formik.handleSubmit} noValidate>
          <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ mb: 3 }}>
            Thông tin môn học
          </Typography>

          <Grid container spacing={3}>
            <Grid size={{ xs: 12 }}>
              <InputLabel htmlFor="code" required sx={{ mb: 1, '& .MuiInputLabel-asterisk': { color: 'error.main' } }}>
                Mã môn học
              </InputLabel>
              <TextField
                id="code"
                name="code"
                placeholder="CSE461"
                size="small"
                fullWidth
                value={formik.values.code}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.code && Boolean(formik.errors.code)}
                helperText={formik.touched.code && formik.errors.code}
              />
            </Grid>

            <Grid size={{ xs: 12 }}>
              <InputLabel htmlFor="name" required sx={{ mb: 1, '& .MuiInputLabel-asterisk': { color: 'error.main' } }}>
                Tên môn học
              </InputLabel>
              <TextField
                id="name"
                name="name"
                placeholder="Lập trình Java"
                size="small"
                fullWidth
                value={formik.values.name}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.name && Boolean(formik.errors.name)}
                helperText={formik.touched.name && formik.errors.name}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel htmlFor="creditNumber" required sx={{ mb: 1, '& .MuiInputLabel-asterisk': { color: 'error.main' } }}>
                Số tín chỉ
              </InputLabel>
              <TextField
                id="creditNumber"
                name="creditNumber"
                type="number"
                size="small"
                fullWidth
                value={formik.values.creditNumber}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.creditNumber && Boolean(formik.errors.creditNumber)}
                helperText={formik.touched.creditNumber && formik.errors.creditNumber}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel htmlFor="sectionId" required sx={{ mb: 1, '& .MuiInputLabel-asterisk': { color: 'error.main' } }}>
                Bộ môn
              </InputLabel>
              <Autocomplete
                id="sectionId"
                options={sections}
                getOptionLabel={(option) => option.name || ''}
                value={sections.find((s) => s.id === formik.values.sectionId) || null}
                onChange={(_, newValue) => {
                  formik.setFieldValue('sectionId', newValue ? newValue.id : 0);
                }}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    placeholder="Chọn bộ môn"
                    size="small"
                    error={formik.touched.sectionId && Boolean(formik.errors.sectionId)}
                    helperText={formik.touched.sectionId && formik.errors.sectionId}
                  />
                )}
                fullWidth
              />
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

            <AnimateButton>
              <Button variant="contained" type="submit">
                Thêm môn học
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
