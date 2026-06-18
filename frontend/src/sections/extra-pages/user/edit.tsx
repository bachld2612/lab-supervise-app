import {
  Paper,
  Typography,
  InputLabel,
  TextField,
  Button,
  Snackbar,
  Alert,
  Box,
  FormControl,
  Select,
  SelectChangeEvent,
  MenuItem
} from '@mui/material';
import { Grid, Stack } from '@mui/system';
import AnimateButton from 'components/@extended/AnimateButton';
import { useFormik } from 'formik';
import { useEffect, useState } from 'react';
import { User } from 'types/user';
import * as Yup from 'yup';
import { useIntl } from 'react-intl';
import { getById, update } from 'api/user';
import { HttpStatusCode } from 'axios';
import { useNavigate, useParams } from 'react-router-dom';
import useAuth from 'hooks/useAuth';

export default function EditUser() {
  const intl = useIntl();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [user, setUser] = useState<User>({
    id: 0,
    status: 0,
    phone: '',
    fullName: '',
    birthday: '',
    hometown: '',
    email: '',
    roleId: 0
  });

  const { logout } = useAuth();
  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  const validationSchema = Yup.object({
    phone: Yup.string()
      .matches(/^(\+84|0)(3[2-9]|5[6|8|9]|7[0|6-9]|8[1-6]|9[0-9])\d{7}$/, 'Số điện thoại không hợp lệ')
      .required('Số điện thoại không được phép bỏ trống'),
    fullName: Yup.string().required('Tên không được phép bỏ trống'),
    birthday: Yup.string()
      .matches(/^\d{4}-\d{2}-\d{2}$/, 'Ngày sinh phải có định dạng yyyy-MM-dd')
      .required('Ngày sinh không được phép bỏ trống'),
    email: Yup.string().email('Email không hợp lệ').required('Email không được phép bỏ trống'),
    hometown: Yup.string().required('Quê nhà không được phép bỏ trống'),
    roleId: Yup.number().required('Role không được phép bỏ trống').min(1, 'Role không được phép bỏ trống')
  });

  const formik = useFormik<User>({
    enableReinitialize: true,
    validationSchema: validationSchema,
    initialValues: {
      id: user.id || 0,
      phone: user.phone || '',
      fullName: user.fullName || '',
      birthday: user.birthday || '',
      email: user.email || '',
      hometown: user.hometown || '',
      roleId: user.roleId || 0,
      status: user.status || 1
    },
    onSubmit: async (values) => {
      const response = await update(Number(id), values);

      if (response.statusCode === HttpStatusCode.Ok) {
        navigate('/user', {
          state: { alert: { open: true, severity: 'success', message: 'Cập nhật nhân viên thành công' } }
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
    const fetchUser = async () => {
      if (id) {
        const response = await getById(Number(id));

        if (response.statusCode === HttpStatusCode.Ok) {
          setUser(response.data);
        } else if (response.statusCode === HttpStatusCode.Unauthorized) {
          logout();
        } else {
          setAlert({ open: true, message: intl.formatMessage({ id: 'unknown-error' }), severity: 'error' });
        }
      }
    };

    fetchUser();
  }, [id, intl, logout]);

  return (
    <Box>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <Typography variant="h3">Cập nhật thông tin nhân viên</Typography>
      </Box>

      <Paper elevation={0} sx={{ p: 3, border: '1px solid', borderColor: 'divider', maxWidth: 720, mx: 'auto' }}>
        <form onSubmit={formik.handleSubmit} noValidate>
          <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ mb: 3 }}>
            Thông tin nhân viên
          </Typography>

          <Grid container spacing={2}>
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
              <InputLabel htmlFor="fullName" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Tên nhân viên
              </InputLabel>

              <TextField
                id="fullName"
                name="fullName"
                placeholder="Nhập tên nhân viên"
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
              <InputLabel htmlFor="roleId" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Phân quyền
              </InputLabel>

              <FormControl fullWidth size="small" error={formik.touched.roleId && Boolean(formik.errors.roleId)}>
                <Select
                  id="roleId"
                  name="roleId"
                  value={formik.values.roleId}
                  onChange={(event: SelectChangeEvent<number | string>) => {
                    formik.setFieldValue('roleId', event.target.value);
                  }}
                  displayEmpty
                >
                  <MenuItem value="0">
                    <span style={{ color: 'rgba(0,0,0,0.6)' }}>Phân quyền</span>
                  </MenuItem>

                  <MenuItem value="1">Nhân viên phòng đào tạo</MenuItem>
                  <MenuItem value="4">Nhân viên trung tâm tin học</MenuItem>
                </Select>

                {formik.touched.roleId && formik.errors.roleId && (
                  <Typography variant="caption" color="error" sx={{ mt: 0.5, display: 'block' }}>
                    {formik.errors.roleId}
                  </Typography>
                )}
              </FormControl>
            </Grid>
          </Grid>

          <Grid>
            <Grid size={12} sx={{ p: 0, m: 0 }}>
              <Stack direction="row" sx={{ justifyContent: 'flex-end', mt: 3 }}>
                <AnimateButton>
                  <Button
                    onClick={() => navigate('/user')}
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

                <AnimateButton>
                  <Button variant="contained" type="submit" sx={{ ml: 1 }}>
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
