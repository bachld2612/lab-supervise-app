import { Paper, Typography, InputLabel, TextField, Button, Snackbar, Alert, Box, Autocomplete, CircularProgress } from '@mui/material';
import { Grid, Stack } from '@mui/system';
import AnimateButton from 'components/@extended/AnimateButton';
import { useFormik } from 'formik';
import { useEffect, useState } from 'react';
import { Classes } from 'types/classes';
import * as Yup from 'yup';
import { useIntl } from 'react-intl';
import { getById, update } from 'api/class';
import { getList as getTeacherList } from 'api/teacher';
import { getList as getSubjectList } from 'api/subject';
import { getList as getScheduleList } from 'api/schedule';
import { getList as getSemesterList } from 'api/semester';
import { getList as getRoomList } from 'api/room';
import { HttpStatusCode } from 'axios';
import { useNavigate, useParams } from 'react-router-dom';
import useAuth from 'hooks/useAuth';
import { Teacher } from 'types/teacher';
import { Subject } from 'types/subject';
import { Schedule } from 'types/schedule';
import { Semester } from 'types/semester';
import { Room } from 'types/room';

export default function EditClass() {
  const intl = useIntl();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { logout } = useAuth();
  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [semesters, setSemesters] = useState<Semester[]>([]);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [loading, setLoading] = useState(false);
  const [classData, setClassData] = useState<Classes>({
    id: 0,
    status: 1,
    name: '',
    maxStudent: 0,
    currentStudent: 0,
    subjectId: 0,
    subjectName: '',
    teacherId: 0,
    teacherName: '',
    scheduleId: 0,
    scheduleName: '',
    startDate: '',
    endDate: '',
    semesterId: 0,
    semesterName: '',
    roomId: 0,
    roomName: ''
  });

  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      const resT = await getTeacherList({ page: 0, size: 1000, keyword: '', status: '1' });
      if (resT.statusCode === HttpStatusCode.Ok) setTeachers(resT.data.content);

      const resS = await getSubjectList({ page: 0, size: 1000, keyword: '', status: '1' });
      if (resS.statusCode === HttpStatusCode.Ok) setSubjects(resS.data.content);

      const resSch = await getScheduleList({ page: 0, size: 1000, keyword: '', status: '1' });
      if (resSch.statusCode === HttpStatusCode.Ok) setSchedules(resSch.data.content);

      const resSem = await getSemesterList({ page: 0, size: 1000, keyword: '', status: '1' });
      if (resSem.statusCode === HttpStatusCode.Ok) setSemesters(resSem.data.content);

      const resR = await getRoomList({ page: 0, size: 1000, keyword: '', status: '1', sort: '' });
      if (resR.statusCode === HttpStatusCode.Ok) setRooms(resR.data.content);

      if (id) {
        const resC = await getById(Number(id));
        if (resC.statusCode === HttpStatusCode.Ok) {
          setClassData(resC.data);
        } else if (resC.statusCode === HttpStatusCode.Unauthorized) {
          logout();
        }
      }
      setLoading(false);
    };
    fetchData();
  }, [id, logout]);

  const validationSchema = Yup.object({
    name: Yup.string().required('Tên lớp không được phép bỏ trống'),
    maxStudent: Yup.number().required('Sĩ số tối đa không được phép bỏ trống').min(1, 'Sĩ số tối đa phải lớn hơn 0'),
    subjectId: Yup.number().required('Môn học không được phép bỏ trống').min(1, 'Môn học không được phép bỏ trống'),
    teacherId: Yup.number().required('Giảng viên không được phép bỏ trống').min(1, 'Giảng viên không được phép bỏ trống'),
    scheduleId: Yup.number().required('Lịch học không được phép bỏ trống').min(1, 'Lịch học không được phép bỏ trống'),
    semesterId: Yup.number().required('Học kì không được phép bỏ trống').min(1, 'Học kì không được phép bỏ trống'),
    roomId: Yup.number().required('Phòng học không được phép bỏ trống').min(1, 'Phòng học không được phép bỏ trống'),
    startDate: Yup.string().required('Ngày bắt đầu không được phép bỏ trống'),
    endDate: Yup.string().required('Ngày kết thúc không được phép bỏ trống')
  });

  const formik = useFormik<Classes>({
    enableReinitialize: true,
    validationSchema: validationSchema,
    initialValues: {
      id: classData.id || 0,
      name: classData.name || '',
      maxStudent: classData.maxStudent || 0,
      currentStudent: classData.currentStudent || 0,
      subjectId: classData.subjectId || 0,
      subjectName: classData.subjectName || '',
      teacherId: classData.teacherId || 0,
      teacherName: classData.teacherName || '',
      scheduleId: classData.scheduleId || 0,
      scheduleName: classData.scheduleName || '',
      semesterId: classData.semesterId || 0,
      semesterName: classData.semesterName || '',
      roomId: classData.roomId || 0,
      roomName: classData.roomName || '',
      startDate: classData.startDate || '',
      endDate: classData.endDate || '',
      status: classData.status || 1
    },
    onSubmit: async (values) => {
      const response = await update(values, Number(id));

      if (response.statusCode === HttpStatusCode.Ok) {
        navigate('/class', {
          state: { alert: { open: true, severity: 'success', message: 'Cập nhật lớp học phần thành công' } }
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
        <Typography variant="h3">Cập nhật lớp học phần</Typography>
      </Box>

      <Paper elevation={0} sx={{ p: 3, border: '1px solid', borderColor: 'divider', ml: 30, mr: 30 }}>
        <form onSubmit={formik.handleSubmit} noValidate>
          <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ mb: 3 }}>
            Thông tin lớp học phần
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
              <InputLabel htmlFor="subjectId" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Môn học
              </InputLabel>

              <Autocomplete
                id="subjectId"
                options={subjects}
                getOptionLabel={(option) => `${option.code} - ${option.name}`}
                value={subjects.find((s) => s.id === formik.values.subjectId) || null}
                onChange={(_, newValue) => {
                  formik.setFieldValue('subjectId', newValue ? newValue.id : 0);
                }}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    placeholder="Chọn môn học"
                    size="small"
                    error={formik.touched.subjectId && Boolean(formik.errors.subjectId)}
                    helperText={formik.touched.subjectId && formik.errors.subjectId}
                    InputProps={{
                      ...params.InputProps,
                      endAdornment: (
                        <>
                          {loading ? <CircularProgress color="inherit" size={20} /> : null}
                          {params.InputProps.endAdornment}
                        </>
                      )
                    }}
                  />
                )}
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
                    InputProps={{
                      ...params.InputProps,
                      endAdornment: (
                        <>
                          {loading ? <CircularProgress color="inherit" size={20} /> : null}
                          {params.InputProps.endAdornment}
                        </>
                      )
                    }}
                  />
                )}
              />
            </Grid>

            <Grid size={{ xs: 12 }}>
              <InputLabel htmlFor="scheduleId" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Lịch học
              </InputLabel>

              <Autocomplete
                id="scheduleId"
                options={schedules}
                getOptionLabel={(option) => option.name || ''}
                value={schedules.find((s) => s.id === formik.values.scheduleId) || null}
                onChange={(_, newValue) => {
                  formik.setFieldValue('scheduleId', newValue ? newValue.id : 0);
                }}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    placeholder="Chọn lịch học"
                    size="small"
                    error={formik.touched.scheduleId && Boolean(formik.errors.scheduleId)}
                    helperText={formik.touched.scheduleId && formik.errors.scheduleId}
                    InputProps={{
                      ...params.InputProps,
                      endAdornment: (
                        <>
                          {loading ? <CircularProgress color="inherit" size={20} /> : null}
                          {params.InputProps.endAdornment}
                        </>
                      )
                    }}
                  />
                )}
              />
            </Grid>

            <Grid size={{ xs: 12 }}>
              <InputLabel htmlFor="semesterId" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Học kì
              </InputLabel>

              <Autocomplete
                id="semesterId"
                options={semesters}
                getOptionLabel={(option) => option.name || ''}
                value={semesters.find((s) => s.id === formik.values.semesterId) || null}
                onChange={(_, newValue) => {
                  formik.setFieldValue('semesterId', newValue ? newValue.id : 0);
                }}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    placeholder="Chọn học kì"
                    size="small"
                    error={formik.touched.semesterId && Boolean(formik.errors.semesterId)}
                    helperText={formik.touched.semesterId && formik.errors.semesterId}
                    InputProps={{
                      ...params.InputProps,
                      endAdornment: (
                        <>
                          {loading ? <CircularProgress color="inherit" size={20} /> : null}
                          {params.InputProps.endAdornment}
                        </>
                      )
                    }}
                  />
                )}
              />
            </Grid>

            <Grid size={{ xs: 12 }}>
              <InputLabel htmlFor="roomId" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Phòng học
              </InputLabel>

              <Autocomplete
                id="roomId"
                options={rooms}
                getOptionLabel={(option) => option.name || ''}
                value={rooms.find((r) => r.id === formik.values.roomId) || null}
                onChange={(_, newValue) => {
                  formik.setFieldValue('roomId', newValue ? newValue.id : 0);
                  formik.setFieldValue('roomName', newValue ? newValue.name : '');
                }}
                loading={loading}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    placeholder="Chọn phòng học"
                    size="small"
                    error={formik.touched.roomId && Boolean(formik.errors.roomId)}
                    helperText={formik.touched.roomId && formik.errors.roomId}
                    InputProps={{
                      ...params.InputProps,
                      endAdornment: (
                        <>
                          {loading ? <CircularProgress color="inherit" size={20} /> : null}
                          {params.InputProps.endAdornment}
                        </>
                      )
                    }}
                  />
                )}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel htmlFor="startDate" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Ngày bắt đầu
              </InputLabel>

              <TextField
                id="startDate"
                name="startDate"
                type="date"
                fullWidth
                size="small"
                value={formik.values.startDate}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.startDate && Boolean(formik.errors.startDate)}
                helperText={formik.touched.startDate && formik.errors.startDate}
                slotProps={{
                  inputLabel: {
                    shrink: true
                  }
                }}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel htmlFor="endDate" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Ngày kết thúc
              </InputLabel>

              <TextField
                id="endDate"
                name="endDate"
                type="date"
                fullWidth
                size="small"
                value={formik.values.endDate}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.endDate && Boolean(formik.errors.endDate)}
                helperText={formik.touched.endDate && formik.errors.endDate}
                slotProps={{
                  inputLabel: {
                    shrink: true
                  }
                }}
              />
            </Grid>
          </Grid>

          <Grid>
            <Grid size={12} sx={{ p: 0, m: 0 }}>
              <Stack direction="row" sx={{ justifyContent: 'flex-end', mt: 3 }}>
                <AnimateButton>
                  <Button
                    onClick={() => navigate('/class')}
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
