import { Paper, Typography, InputLabel, TextField, Button, Snackbar, Alert, Box, Autocomplete } from '@mui/material';
import { Grid, Stack } from '@mui/system';
import AnimateButton from 'components/@extended/AnimateButton';
import { useFormik } from 'formik';
import { useState, useEffect } from 'react';
import { ManageClass } from 'types/manageClass';
import * as Yup from 'yup';
import { useIntl } from 'react-intl';
import { create } from 'api/manageClass';
import { getList as getTeacherList } from 'api/teacher';
import { getList as getMajorList } from 'api/major';
import { HttpStatusCode } from 'axios';
import { useNavigate } from 'react-router-dom';
import useAuth from 'hooks/useAuth';
import { Teacher } from 'types/teacher';
import { Major } from 'types/major';

export default function AddManageClass() {
  const intl = useIntl();
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [majors, setMajors] = useState<Major[]>([]);
  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  useEffect(() => {
    const fetchData = async () => {
      const resT = await getTeacherList({ page: 0, size: 1000, keyword: '', status: '1' });
      if (resT.statusCode === HttpStatusCode.Ok) setTeachers(resT.data.content);

      const resM = await getMajorList({ page: 0, size: 1000, keyword: '', status: '1' });
      if (resM.statusCode === HttpStatusCode.Ok) setMajors(resM.data.content);
    };
    fetchData();
  }, []);

  const validationSchema = Yup.object({
    name: Yup.string().required('Tên lớp không được phép bỏ trống'),
    maxStudent: Yup.number().required('Sĩ số tối đa không được phép bỏ trống').min(1, 'Sĩ số tối đa phải lớn hơn 0'),
    teacherId: Yup.number().required('Giảng viên không được phép bỏ trống').min(1, 'Giảng viên không được phép bỏ trống'),
    majorId: Yup.number().required('Chuyên ngành không được phép bỏ trống').min(1, 'Chuyên ngành không được phép bỏ trống')
  });

  const initialValues: ManageClass = {
    id: 0,
    status: 1,
    name: '',
    maxStudent: '' as any,
    teacherId: 0,
    teacherName: '',
    majorId: 0,
    majorName: ''
  };

  const formik = useFormik<ManageClass>({
    validationSchema,
    initialValues: initialValues,
    onSubmit: async (values) => {
      const response = await create(values);

      if (response.statusCode === HttpStatusCode.Ok) {
        navigate('/manage-class', {
          state: { alert: { open: true, severity: 'success', message: 'Thêm lớp thành công' } }
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
        <Typography variant="h3">Thêm lớp quản lý</Typography>
      </Box>

      <Paper elevation={0} sx={{ p: 3, border: '1px solid', borderColor: 'divider', ml: 30, mr: 30 }}>
        <form onSubmit={formik.handleSubmit} noValidate>
          <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ mb: 3 }}>
            Thông tin lớp quản lý
          </Typography>

          <Grid container spacing={2}>
            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel htmlFor="name" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Tên lớp
              </InputLabel>

              <TextField
                id="name"
                name="name"
                placeholder="Nhập tên lớp"
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
              <InputLabel htmlFor="maxStudent" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Sĩ số tối đa
              </InputLabel>

              <TextField
                id="maxStudent"
                name="maxStudent"
                type="number"
                placeholder="Nhập sĩ số tối đa"
                size="small"
                fullWidth
                value={formik.values.maxStudent}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.maxStudent && Boolean(formik.errors.maxStudent)}
                helperText={formik.touched.maxStudent && formik.errors.maxStudent}
              />
            </Grid>

            <Grid size={{ xs: 12 }}>
              <InputLabel htmlFor="teacherId" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Giảng viên
              </InputLabel>

              <Autocomplete
                id="teacherId"
                options={teachers}
                getOptionLabel={(option) => `${option.code} - ${option.fullName}`}
                value={teachers.find((t) => t.id === formik.values.teacherId) || null}
                onChange={(_, newValue) => {
                  formik.setFieldValue('teacherId', newValue ? newValue.id : 0);
                }}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    placeholder="Chọn giảng viên"
                    size="small"
                    error={formik.touched.teacherId && Boolean(formik.errors.teacherId)}
                    helperText={formik.touched.teacherId && formik.errors.teacherId}
                  />
                )}
              />
            </Grid>

            <Grid size={{ xs: 12 }}>
              <InputLabel htmlFor="majorId" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Chuyên ngành
              </InputLabel>

              <Autocomplete
                id="majorId"
                options={majors}
                getOptionLabel={(option) => option.name}
                value={majors.find((m) => m.id === formik.values.majorId) || null}
                onChange={(_, newValue) => {
                  formik.setFieldValue('majorId', newValue ? newValue.id : 0);
                }}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    placeholder="Chọn chuyên ngành"
                    size="small"
                    error={formik.touched.majorId && Boolean(formik.errors.majorId)}
                    helperText={formik.touched.majorId && formik.errors.majorId}
                  />
                )}
              />
            </Grid>
          </Grid>

          <Grid>
            <Grid size={12} sx={{ p: 0, m: 0 }}>
              <Stack direction="row" sx={{ justifyContent: 'flex-end' }}>
                <AnimateButton>
                  <Button
                    onClick={() => navigate('/manage-class')}
                    variant="contained"
                    sx={{
                      my: 3,
                      ml: 1,
                      bgcolor: '#7e7e7eff',
                      color: 'white',
                      '&:hover': { bgcolor: '#9a9999ff' }
                    }}
                  >
                    Trở về
                  </Button>
                </AnimateButton>

                <AnimateButton>
                  <Button variant="contained" type="submit" sx={{ my: 3, ml: 1 }}>
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
