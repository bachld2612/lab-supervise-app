import { useEffect, useMemo, useState } from 'react';

import {
  Alert,
  Autocomplete,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  Grid,
  Snackbar,
  Stack,
  TextField,
  Typography
} from '@mui/material';
import { getTeacherClasses } from 'api/class';
import { getTeacherExamRooms } from 'api/exam-room';
import { getList as getSemesters } from 'api/semester';
import MainCard from 'components/MainCard';
import useAuth from 'hooks/useAuth';
import { Book1, Calendar, Calendar1, Clock, People, Timer1 } from 'iconsax-reactjs';
import { useIntl } from 'react-intl';
import { useNavigate } from 'react-router';
import { type Classes } from 'types/classes';
import { type ExamRoom } from 'types/exam-room';
import { type Semester } from 'types/semester';
import { HttpStatusCode } from 'axios';
import formatDate, { formatTimeWithoutSecond } from 'utils/formatDate';
import StudentListDialog from 'sections/extra-pages/class/max-student-dialog';
import ExamRoomStudentListDialog from 'sections/extra-pages/exam-room/exam-room-student-list-dialog';

function ExamStatusBadge({ studyStatus }: { studyStatus?: number }) {
  if (studyStatus === 1) return <Chip label="Đang diễn ra" color="success" size="small" />;
  if (studyStatus === 2) return <Chip label="Đã kết thúc" color="default" size="small" />;
  return <Chip label="Sắp diễn ra" color="warning" size="small" />;
}

const parsePeriodValues = (periods?: string) =>
  (periods ?? '')
    .split(',')
    .map((value) => Number(value.trim()))
    .filter((value) => Number.isInteger(value))
    .sort((a, b) => a - b);

const formatExamPeriodRange = (examRoom: ExamRoom) => {
  const values = parsePeriodValues(examRoom.periods);
  const timeRange = `${formatTimeWithoutSecond(examRoom.startTime)} - ${formatTimeWithoutSecond(examRoom.endTime)}`;

  if (values.length === 0) return timeRange;
  if (values.length === 1) return `Tiết ${values[0]} (${timeRange})`;

  return `Tiết ${values[0]} - Tiết ${values[values.length - 1]} (${timeRange})`;
};

// ==============================|| TEACHER DASHBOARD PAGE ||============================== //

export default function TeacherDashboardPage() {
  const { logout } = useAuth();
  const intl = useIntl();
  const navigate = useNavigate();

  const [classes, setClasses] = useState<Classes[]>([]);
  const [examRooms, setExamRooms] = useState<ExamRoom[]>([]);
  const [semesters, setSemesters] = useState<Semester[]>([]);
  const [loading, setLoading] = useState(true);
  const [classSemester, setClassSemester] = useState<Semester | null>(null);
  const [examSemester, setExamSemester] = useState<Semester | null>(null);
  const [classKeyword, setClassKeyword] = useState('');
  const [examKeyword, setExamKeyword] = useState('');
  const [classKeywordInput, setClassKeywordInput] = useState('');
  const [examKeywordInput, setExamKeywordInput] = useState('');
  const [examVisibleCount, setExamVisibleCount] = useState(3);
  const [classVisibleCount, setClassVisibleCount] = useState(3);
  // Student-list dialogs are rendered at page root (NOT inside the card) so their
  // clicks don't bubble through the React tree to the card's navigate onClick.
  const [classSizeItem, setClassSizeItem] = useState<Classes | null>(null);
  const [examSizeItem, setExamSizeItem] = useState<ExamRoom | null>(null);
  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  useEffect(() => {
    const fetchAll = async () => {
      setLoading(true);
      const [classRes, examRes, semRes] = await Promise.all([
        getTeacherClasses(),
        getTeacherExamRooms(),
        getSemesters({ size: 1000, sort: 'startDate,desc' })
      ]);
      if (classRes.statusCode === HttpStatusCode.Ok) {
        setClasses(classRes.data ?? []);
      } else if (classRes.statusCode === HttpStatusCode.Unauthorized) {
        logout();
      } else {
        setAlert({ open: true, message: intl.formatMessage({ id: 'unknown-error' }), severity: 'error' });
      }
      if (examRes.statusCode === HttpStatusCode.Ok) {
        setExamRooms(examRes.data ?? []);
      }
      if (semRes?.statusCode === HttpStatusCode.Ok) {
        const content: Semester[] = semRes.data?.content ?? [];
        setSemesters(content);
        if (content.length > 0) {
          setClassSemester(content[0]);
          setExamSemester(content[0]);
        }
      }
      setLoading(false);
    };
    fetchAll();
  }, [intl, logout]);

  useEffect(() => {
    setExamVisibleCount(3);
  }, [examKeyword, examSemester]);
  useEffect(() => {
    setClassVisibleCount(3);
  }, [classKeyword, classSemester]);

  const filteredClasses = useMemo(() => {
    const kw = classKeyword.trim().toLowerCase();
    let list = classSemester === null ? classes : classes.filter((c) => c.semesterId === classSemester.id);
    if (kw) list = list.filter((c) => c.name?.toLowerCase().includes(kw) || c.subjectName?.toLowerCase().includes(kw));
    const priority = (s?: number) => (s === 1 ? 0 : s === 2 ? 2 : 1);
    return [...list].sort((a, b) => {
      const diff = priority(a.studyStatus) - priority(b.studyStatus);
      if (diff !== 0) return diff;
      return (a.startDate ?? '').localeCompare(b.startDate ?? '');
    });
  }, [classes, classSemester, classKeyword]);

  const filteredExamRooms = useMemo(() => {
    const kw = examKeyword.trim().toLowerCase();
    let list = examSemester === null ? examRooms : examRooms.filter((e) => e.semesterId === examSemester.id);
    if (kw) list = list.filter((e) => e.code?.toLowerCase().includes(kw) || e.subjectName?.toLowerCase().includes(kw));
    const examPriority = (s?: number) => (s === 1 ? 0 : s === 2 ? 2 : 1);
    return [...list].sort((a, b) => {
      const diff = examPriority(a.studyStatus) - examPriority(b.studyStatus);
      if (diff !== 0) return diff;
      return (a.examDate + a.startTime).localeCompare(b.examDate + b.startTime);
    });
  }, [examRooms, examSemester, examKeyword]);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight={400}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Stack spacing={4} sx={{ p: 0 }}>
      <Snackbar
        open={alert.open}
        autoHideDuration={3000}
        onClose={() => setAlert({ ...alert, open: false })}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Alert severity={alert.severity} variant="filled" sx={{ width: '100%', borderRadius: 2, fontSize: 15, py: 1.5, px: 2 }}>
          {alert.message}
        </Alert>
      </Snackbar>

      {/* ── Exam Rooms Section ── */}
      <Box>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          justifyContent="space-between"
          alignItems={{ sm: 'center' }}
          spacing={2}
          sx={{ mb: 2 }}
        >
          <Stack direction="row" spacing={1.5} alignItems="center">
            <Calendar1 size={22} />
            <Typography variant="h4">Lịch thi</Typography>
            <Chip label={`${filteredExamRooms.length} phòng`} color="primary" size="small" variant="outlined" />
          </Stack>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems={{ sm: 'center' }}>
            <TextField
              size="small"
              placeholder="Tìm mã phòng, môn thi..."
              value={examKeywordInput}
              onChange={(e) => setExamKeywordInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') setExamKeyword(examKeywordInput);
              }}
              sx={{ minWidth: 220 }}
            />
            {semesters.length > 0 && (
              <Autocomplete
                size="small"
                sx={{ minWidth: 220 }}
                options={semesters}
                getOptionLabel={(option) => option.name}
                value={examSemester}
                onChange={(_, newValue) => setExamSemester(newValue)}
                isOptionEqualToValue={(option, value) => option.id === value.id}
                renderInput={(params) => <TextField {...params} label="Học kỳ" />}
              />
            )}
          </Stack>
        </Stack>

        <MainCard content={false}>
          <Box sx={{ p: 3 }}>
            {filteredExamRooms.length === 0 ? (
              <Box textAlign="center" py={5}>
                <Typography color="text.secondary" variant="h6">
                  Không có lịch thi nào
                </Typography>
              </Box>
            ) : (
              <Stack spacing={3}>
                <Grid container spacing={3}>
                  {filteredExamRooms.slice(0, examVisibleCount).map((er) => (
                    <Grid key={er.id} size={{ xs: 12, sm: 6, md: 4 }}>
                      <Card
                        onClick={() => navigate(`/teacher/exam-room/${er.id}/tracking`)}
                        sx={{
                          height: '100%',
                          border: '1px solid',
                          borderColor: 'divider',
                          borderRadius: 2,
                          cursor: 'pointer',
                          transition: 'box-shadow 0.2s',
                          '&:hover': { boxShadow: 4 }
                        }}
                      >
                        <CardContent sx={{ p: 2.5, '&:last-child': { pb: 2.5 } }}>
                          <Stack spacing={2}>
                            <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                              <Typography variant="h6" fontWeight="bold" sx={{ flex: 1, pr: 1 }}>
                                {er.code}
                              </Typography>
                              <ExamStatusBadge studyStatus={er.studyStatus} />
                            </Stack>
                            <Divider />
                            <Stack spacing={1.5}>
                              <Stack direction="row" spacing={1} alignItems="center">
                                <Book1 size={16} />
                                <Typography variant="body2" color="text.secondary">
                                  {er.subjectName}
                                </Typography>
                              </Stack>
                              <Stack direction="row" spacing={1} alignItems="center">
                                <Calendar size={16} />
                                <Typography variant="body2" color="text.secondary">
                                  {formatDate(er.examDate)}
                                </Typography>
                              </Stack>
                              <Stack direction="row" spacing={1} alignItems="center">
                                <Clock size={16} />
                                <Typography variant="body2" color="text.secondary">
                                  {formatExamPeriodRange(er)}
                                </Typography>
                              </Stack>
                              <Typography variant="caption" color="text.disabled">
                                {er.semesterName}&nbsp;·&nbsp;Phòng {er.roomName}
                              </Typography>
                            </Stack>
                            <Divider />
                            <Stack direction="row" justifyContent="space-between" alignItems="center">
                              <Stack direction="row" spacing={0.75} alignItems="center">
                                <People size={16} />
                                <Typography variant="body2" color="text.secondary">
                                  Sĩ số
                                </Typography>
                              </Stack>
                              <Typography
                                variant="body1"
                                fontWeight="bold"
                                color="primary.main"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setExamSizeItem(er);
                                }}
                                sx={{ cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}
                              >
                                {er.currentStudent}/{er.maxStudent}
                              </Typography>
                            </Stack>
                          </Stack>
                        </CardContent>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
                {examVisibleCount < filteredExamRooms.length && (
                  <Box textAlign="center">
                    <Button variant="outlined" size="small" onClick={() => setExamVisibleCount((n) => n + 3)}>
                      Xem thêm {Math.min(3, filteredExamRooms.length - examVisibleCount)} phòng thi
                    </Button>
                  </Box>
                )}
                {examVisibleCount >= filteredExamRooms.length && filteredExamRooms.length > 3 && (
                  <Box textAlign="center">
                    <Button variant="text" size="small" color="inherit" onClick={() => setExamVisibleCount(3)}>
                      Ẩn bớt
                    </Button>
                  </Box>
                )}
              </Stack>
            )}
          </Box>
        </MainCard>
      </Box>

      {/* ── Classes Section ── */}
      <Box>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          justifyContent="space-between"
          alignItems={{ sm: 'center' }}
          spacing={2}
          sx={{ mb: 2 }}
        >
          <Stack direction="row" spacing={1.5} alignItems="center">
            <Book1 size={22} />
            <Typography variant="h4">Lớp học</Typography>
            <Chip label={`${filteredClasses.length} lớp`} color="primary" size="small" variant="outlined" />
            {filteredClasses.filter((c) => c.studyStatus === 1).length > 0 && (
              <Chip label={`${filteredClasses.filter((c) => c.studyStatus === 1).length} đang học`} color="success" size="small" />
            )}
          </Stack>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems={{ sm: 'center' }}>
            <TextField
              size="small"
              placeholder="Tìm tên lớp, môn học..."
              value={classKeywordInput}
              onChange={(e) => setClassKeywordInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') setClassKeyword(classKeywordInput);
              }}
              sx={{ minWidth: 220 }}
            />
            {semesters.length > 0 && (
              <Autocomplete
                size="small"
                sx={{ minWidth: 220 }}
                options={semesters}
                getOptionLabel={(option) => option.name}
                value={classSemester}
                onChange={(_, newValue) => setClassSemester(newValue)}
                isOptionEqualToValue={(option, value) => option.id === value.id}
                renderInput={(params) => <TextField {...params} label="Học kỳ" />}
              />
            )}
          </Stack>
        </Stack>

        <MainCard content={false}>
          <Box sx={{ p: 3 }}>
            {filteredClasses.length === 0 ? (
              <Box textAlign="center" py={5}>
                <Typography color="text.secondary" variant="h6">
                  Không có lớp học nào
                </Typography>
              </Box>
            ) : (
              <Stack spacing={3}>
                <Grid container spacing={3}>
                  {filteredClasses.slice(0, classVisibleCount).map((cls) => {
                    const isActive = cls.studyStatus === 1;
                    const isEnded = cls.studyStatus === 2;
                    return (
                      <Grid key={cls.id} size={{ xs: 12, sm: 6, md: 4 }}>
                        <Card
                          onClick={() => navigate(`/teacher/class/${cls.id}/tracking`, { state: { studyStatus: cls.studyStatus } })}
                          sx={{
                            height: '100%',
                            border: '1px solid',
                            borderColor: isActive ? 'success.main' : isEnded ? 'divider' : 'warning.main',
                            borderRadius: 2,
                            cursor: 'pointer',
                            transition: 'box-shadow 0.2s',
                            '&:hover': { boxShadow: 4 }
                          }}
                        >
                          <CardContent sx={{ p: 2.5, '&:last-child': { pb: 2.5 } }}>
                            <Stack spacing={2}>
                              <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                                <Typography variant="h6" fontWeight="bold" sx={{ flex: 1, pr: 1 }}>
                                  {cls.name}
                                </Typography>
                                <Chip
                                  label={isActive ? 'Đang học' : isEnded ? 'Đã kết thúc' : 'Sắp diễn ra'}
                                  color={isActive ? 'success' : isEnded ? 'default' : 'warning'}
                                  size="small"
                                />
                              </Stack>
                              <Divider />
                              <Stack spacing={1.5}>
                                <Stack direction="row" spacing={1} alignItems="center">
                                  <Book1 size={16} />
                                  <Typography variant="body2" color="text.secondary">
                                    {cls.subjectName}
                                  </Typography>
                                </Stack>
                                <Stack direction="row" spacing={1} alignItems="center">
                                  <Calendar size={16} />
                                  <Typography variant="body2" color="text.secondary">
                                    {cls.scheduleName}
                                  </Typography>
                                </Stack>
                                <Stack direction="row" spacing={1} alignItems="center">
                                  <Timer1 size={16} />
                                  <Typography variant="body2" color="text.secondary">
                                    {formatDate(cls.startDate)} → {formatDate(cls.endDate)}
                                  </Typography>
                                </Stack>
                                <Typography variant="caption" color="text.disabled">
                                  {cls.semesterName}&nbsp;·&nbsp;{cls.sessionNumber} buổi học
                                </Typography>
                              </Stack>
                              <Divider />
                              <Stack direction="row" justifyContent="space-between" alignItems="center">
                                <Stack direction="row" spacing={0.75} alignItems="center">
                                  <People size={16} />
                                  <Typography variant="body2" color="text.secondary">
                                    Sĩ số
                                  </Typography>
                                </Stack>
                                <Typography
                                  variant="body1"
                                  fontWeight="bold"
                                  color="primary.main"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    setClassSizeItem(cls);
                                  }}
                                  sx={{ cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}
                                >
                                  {cls.currentStudent}/{cls.maxStudent}
                                </Typography>
                              </Stack>
                            </Stack>
                          </CardContent>
                        </Card>
                      </Grid>
                    );
                  })}
                </Grid>
                {classVisibleCount < filteredClasses.length && (
                  <Box textAlign="center">
                    <Button variant="outlined" size="small" onClick={() => setClassVisibleCount((n) => n + 3)}>
                      Xem thêm {Math.min(3, filteredClasses.length - classVisibleCount)} lớp học
                    </Button>
                  </Box>
                )}
                {classVisibleCount >= filteredClasses.length && filteredClasses.length > 3 && (
                  <Box textAlign="center">
                    <Button variant="text" size="small" color="inherit" onClick={() => setClassVisibleCount(3)}>
                      Ẩn bớt
                    </Button>
                  </Box>
                )}
              </Stack>
            )}
          </Box>
        </MainCard>
      </Box>

      <StudentListDialog open={!!classSizeItem} onClose={() => setClassSizeItem(null)} classItem={classSizeItem} />
      <ExamRoomStudentListDialog open={!!examSizeItem} onClose={() => setExamSizeItem(null)} examRoom={examSizeItem} />
    </Stack>
  );
}
