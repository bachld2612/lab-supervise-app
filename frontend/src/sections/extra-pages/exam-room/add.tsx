import { Paper, Typography, InputLabel, TextField, Button, Snackbar, Alert, Box, Autocomplete, CircularProgress } from '@mui/material';
import { Grid, Stack } from '@mui/system';
import AnimateButton from 'components/@extended/AnimateButton';
import { useFormik } from 'formik';
import { useState, useEffect } from 'react';
import { useIntl } from 'react-intl';
import { useNavigate } from 'react-router-dom';
import { HttpStatusCode } from 'axios';
import useAuth from 'hooks/useAuth';
import { create } from 'api/exam-room';
import { getList as getTeacherList } from 'api/teacher';
import { getList as getSubjectList } from 'api/subject';
import { getList as getSemesterList } from 'api/semester';
import { getList as getRoomList } from 'api/room';
import { Teacher } from 'types/teacher';
import { Subject } from 'types/subject';
import { Semester } from 'types/semester';
import { Room } from 'types/room';
import { ExamRoom } from 'types/exam-room';
import { ALL_PERIODS } from 'utils/schedule';
import {
  ExamRoomFormValues,
  emptyExamRoomValues,
  examRoomValidationSchema,
  normalizePeriods,
  parsePeriodValues
} from 'sections/extra-pages/exam-room/form-helpers';

export default function AddExamRoom() {
  const intl = useIntl();
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [semesters, setSemesters] = useState<Semester[]>([]);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [loading, setLoading] = useState(false);
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

      const resSem = await getSemesterList({ page: 0, size: 1000, keyword: '', status: '1' });
      if (resSem.statusCode === HttpStatusCode.Ok) setSemesters(resSem.data.content);

      const resR = await getRoomList({ page: 0, size: 1000, keyword: '', status: '1', sort: '' });
      if (resR.statusCode === HttpStatusCode.Ok) setRooms(resR.data.content);

      setLoading(false);
    };
    fetchData();
  }, []);

  const formik = useFormik<ExamRoomFormValues>({
    validationSchema: examRoomValidationSchema,
    initialValues: emptyExamRoomValues,
    onSubmit: async (values) => {
      const payload = { ...values, maxStudent: Number(values.maxStudent), periods: normalizePeriods(values.periods) };
      const response = await create(payload as Partial<ExamRoom>);

      if (response.statusCode === HttpStatusCode.Ok) {
        navigate('/exam-room', {
          state: { alert: { open: true, severity: 'success', message: 'Thêm phòng thi thành công' } }
        });
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
        <Typography variant="h3">Thêm phòng thi</Typography>
      </Box>

      <Paper elevation={0} sx={{ p: 3, border: '1px solid', borderColor: 'divider', maxWidth: 720, mx: 'auto' }}>
        <form onSubmit={formik.handleSubmit} noValidate>
          <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ mb: 3 }}>
            Thông tin phòng thi
          </Typography>

          <Grid container spacing={2}>
            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel htmlFor="code" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Mã phòng thi
              </InputLabel>
              <TextField
                id="code"
                name="code"
                placeholder="VD: PTHI-101"
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
              <InputLabel htmlFor="maxStudent" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Sĩ số tối đa
              </InputLabel>
              <TextField
                id="maxStudent"
                name="maxStudent"
                type="number"
                placeholder="VD: 30"
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
              <InputLabel required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Môn học
              </InputLabel>
              <Autocomplete
                options={subjects}
                getOptionLabel={(o) => `${o.code} - ${o.name}`}
                value={subjects.find((s) => s.id === formik.values.subjectId) ?? null}
                onChange={(_, v) => formik.setFieldValue('subjectId', v?.id ?? 0)}
                loading={loading}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    size="small"
                    placeholder="Chọn môn học"
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
              <InputLabel required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Phòng thi
              </InputLabel>
              <Autocomplete
                options={rooms}
                getOptionLabel={(o) => o.name ?? ''}
                value={rooms.find((r) => r.id === formik.values.roomId) ?? null}
                onChange={(_, v) => formik.setFieldValue('roomId', v?.id ?? 0)}
                loading={loading}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    size="small"
                    placeholder="Chọn phòng"
                    error={formik.touched.roomId && Boolean(formik.errors.roomId)}
                    helperText={formik.touched.roomId && formik.errors.roomId}
                  />
                )}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Giảng viên coi thi 1
              </InputLabel>
              <Autocomplete
                options={teachers}
                getOptionLabel={(o) => `${o.code} - ${o.fullName}`}
                value={teachers.find((t) => t.id === formik.values.teacher1Id) ?? null}
                onChange={(_, v) => formik.setFieldValue('teacher1Id', v?.id ?? 0)}
                loading={loading}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    size="small"
                    placeholder="Chọn giảng viên 1"
                    error={formik.touched.teacher1Id && Boolean(formik.errors.teacher1Id)}
                    helperText={formik.touched.teacher1Id && formik.errors.teacher1Id}
                  />
                )}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Giảng viên coi thi 2
              </InputLabel>
              <Autocomplete
                options={teachers}
                getOptionLabel={(o) => `${o.code} - ${o.fullName}`}
                value={teachers.find((t) => t.id === formik.values.teacher2Id) ?? null}
                onChange={(_, v) => formik.setFieldValue('teacher2Id', v?.id ?? 0)}
                loading={loading}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    size="small"
                    placeholder="Chọn giảng viên 2"
                    error={formik.touched.teacher2Id && Boolean(formik.errors.teacher2Id)}
                    helperText={formik.touched.teacher2Id && formik.errors.teacher2Id}
                  />
                )}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Học kỳ
              </InputLabel>
              <Autocomplete
                options={semesters}
                getOptionLabel={(o) => o.name ?? ''}
                value={semesters.find((s) => s.id === formik.values.semesterId) ?? null}
                onChange={(_, v) => formik.setFieldValue('semesterId', v?.id ?? 0)}
                loading={loading}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    size="small"
                    placeholder="Chọn học kỳ"
                    error={formik.touched.semesterId && Boolean(formik.errors.semesterId)}
                    helperText={formik.touched.semesterId && formik.errors.semesterId}
                  />
                )}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <InputLabel htmlFor="examDate" required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Ngày thi
              </InputLabel>
              <TextField
                id="examDate"
                name="examDate"
                type="date"
                fullWidth
                size="small"
                value={formik.values.examDate}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.examDate && Boolean(formik.errors.examDate)}
                helperText={formik.touched.examDate && formik.errors.examDate}
                slotProps={{ inputLabel: { shrink: true } }}
              />
            </Grid>

            <Grid size={{ xs: 12 }}>
              <InputLabel required sx={{ '& .MuiInputLabel-asterisk': { color: 'error.main' }, mb: 1 }}>
                Tiết thi
              </InputLabel>
              <Autocomplete
                multiple
                options={ALL_PERIODS}
                getOptionLabel={(option) => option.label}
                value={ALL_PERIODS.filter((period) => parsePeriodValues(formik.values.periods).includes(Number(period.value)))}
                onChange={(_, newValue) => {
                  const values = newValue
                    .map((item) => Number(item.value))
                    .sort((a, b) => a - b)
                    .join(',');
                  formik.setFieldValue('periods', values);
                }}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    size="small"
                    placeholder="Chọn tiết thi"
                    error={formik.touched.periods && Boolean(formik.errors.periods)}
                    helperText={formik.touched.periods && formik.errors.periods}
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
                    onClick={() => navigate('/exam-room')}
                    variant="contained"
                    sx={{ my: 3, ml: 1, bgcolor: '#7e7e7eff', color: 'white', '&:hover': { bgcolor: '#9a9999ff' } }}
                  >
                    Trở về
                  </Button>
                </AnimateButton>

                <AnimateButton>
                  <Button variant="contained" type="submit" disabled={formik.isSubmitting} sx={{ my: 3, ml: 1 }}>
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
