import { ChangeEvent, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Autocomplete,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Divider,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  OutlinedInput,
  Pagination,
  Paper,
  Select,
  SelectChangeEvent,
  Snackbar,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography
} from '@mui/material';
import MainCard from 'components/MainCard';
import IconButton from 'components/@extended/IconButton';
import { Add, Edit2, Eye, ExportCurve, ImportCurve, Trash } from 'iconsax-reactjs';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import { HttpStatusCode } from 'axios';
import useAuth from 'hooks/useAuth';
import { ExamRoom } from 'types/exam-room';
import { getList, create, update, deleteById, importStudents } from 'api/exam-room';
import { getList as getTeacherList } from 'api/teacher';
import { getList as getSubjectList } from 'api/subject';
import { getList as getSemesterList } from 'api/semester';
import { getList as getRoomList } from 'api/room';
import { downloadClassStudentImportTemplate } from 'api/class';
import { DEFAULT_PAGE_SIZE } from 'types/paging';
import formatDate, { formatTimeWithoutSecond } from 'utils/formatDate';
import { Teacher } from 'types/teacher';
import { Subject } from 'types/subject';
import { Semester } from 'types/semester';
import { Room } from 'types/room';

interface ExamRoomFormValues {
  code: string;
  roomId: number;
  teacher1Id: number;
  teacher2Id: number;
  subjectId: number;
  semesterId: number;
  maxStudent: number | '';
  examDate: string;
  startTime: string;
  endTime: string;
}

const validationSchema = Yup.object({
  code: Yup.string().required('Mã phòng thi không được để trống'),
  roomId: Yup.number().min(1, 'Vui lòng chọn phòng').required('Vui lòng chọn phòng'),
  teacher1Id: Yup.number().min(1, 'Vui lòng chọn giảng viên 1').required('Vui lòng chọn giảng viên 1'),
  teacher2Id: Yup.number().min(1, 'Vui lòng chọn giảng viên 2').required('Vui lòng chọn giảng viên 2'),
  subjectId: Yup.number().min(1, 'Vui lòng chọn môn học').required('Vui lòng chọn môn học'),
  semesterId: Yup.number().min(1, 'Vui lòng chọn học kỳ').required('Vui lòng chọn học kỳ'),
  maxStudent: Yup.number().min(1, 'Sĩ số phải lớn hơn 0').required('Vui lòng nhập sĩ số tối đa'),
  examDate: Yup.string().required('Vui lòng chọn ngày thi'),
  startTime: Yup.string().required('Vui lòng nhập giờ bắt đầu'),
  endTime: Yup.string().required('Vui lòng nhập giờ kết thúc')
});

const emptyValues: ExamRoomFormValues = {
  code: '',
  roomId: 0,
  teacher1Id: 0,
  teacher2Id: 0,
  subjectId: 0,
  semesterId: 0,
  maxStudent: '',
  examDate: '',
  startTime: '',
  endTime: ''
};

interface DropdownData {
  teachers: Teacher[];
  subjects: Subject[];
  semesters: Semester[];
  rooms: Room[];
  loading: boolean;
}

function ExamRoomFormDialog({
  open,
  onClose,
  onSuccess,
  editItem,
  dropdownData
}: {
  open: boolean;
  onClose: () => void;
  onSuccess: (message: string) => void;
  editItem: ExamRoom | null;
  dropdownData: DropdownData;
}) {
  const { teachers, subjects, semesters, rooms, loading } = dropdownData;

  const formik = useFormik<ExamRoomFormValues>({
    validationSchema,
    enableReinitialize: true,
    initialValues: editItem
      ? {
          code: editItem.code,
          roomId: editItem.roomId,
          teacher1Id: editItem.teacher1Id,
          teacher2Id: editItem.teacher2Id,
          subjectId: editItem.subjectId,
          semesterId: editItem.semesterId,
          maxStudent: editItem.maxStudent,
          examDate: editItem.examDate,
          startTime: editItem.startTime,
          endTime: editItem.endTime
        }
      : emptyValues,
    onSubmit: async (values, { setSubmitting, resetForm }) => {
      const payload = { ...values, maxStudent: Number(values.maxStudent) };
      const response = editItem ? await update(payload as Partial<ExamRoom>, editItem.id) : await create(payload as Partial<ExamRoom>);

      setSubmitting(false);
      if (response.statusCode === HttpStatusCode.Ok) {
        onSuccess(editItem ? 'Cập nhật phòng thi thành công' : 'Thêm phòng thi thành công');
        resetForm();
        onClose();
      } else if (response.statusCode === HttpStatusCode.UnprocessableEntity) {
        formik.setStatus(response.message ?? 'Dữ liệu không hợp lệ');
      } else {
        formik.setStatus('Lỗi hệ thống, vui lòng thử lại');
      }
    }
  });

  const handleClose = () => {
    formik.resetForm();
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth slotProps={{ paper: { sx: { borderRadius: 3 } } }}>
      <DialogTitle>{editItem ? 'Chỉnh sửa phòng thi' : 'Thêm phòng thi'}</DialogTitle>
      <form onSubmit={formik.handleSubmit} noValidate>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 0.5 }}>
            {formik.status && (
              <Alert severity="error" sx={{ borderRadius: 2 }}>
                {formik.status}
              </Alert>
            )}

            <Grid container spacing={2}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <InputLabel required sx={{ mb: 0.5 }}>Mã phòng thi</InputLabel>
                <TextField
                  name="code"
                  fullWidth
                  size="small"
                  placeholder="VD: PTHI-101"
                  value={formik.values.code}
                  onChange={formik.handleChange}
                  onBlur={formik.handleBlur}
                  error={formik.touched.code && Boolean(formik.errors.code)}
                  helperText={formik.touched.code && formik.errors.code}
                />
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <InputLabel required sx={{ mb: 0.5 }}>Sĩ số tối đa</InputLabel>
                <TextField
                  name="maxStudent"
                  type="number"
                  fullWidth
                  size="small"
                  placeholder="VD: 30"
                  value={formik.values.maxStudent}
                  onChange={formik.handleChange}
                  onBlur={formik.handleBlur}
                  error={formik.touched.maxStudent && Boolean(formik.errors.maxStudent)}
                  helperText={formik.touched.maxStudent && formik.errors.maxStudent}
                />
              </Grid>

              <Grid size={{ xs: 12 }}>
                <InputLabel required sx={{ mb: 0.5 }}>Môn học</InputLabel>
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
                    />
                  )}
                />
              </Grid>

              <Grid size={{ xs: 12 }}>
                <InputLabel required sx={{ mb: 0.5 }}>Phòng thi</InputLabel>
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

              <Grid size={{ xs: 12, sm: 6 }}>
                <InputLabel required sx={{ mb: 0.5 }}>Giảng viên coi thi 1</InputLabel>
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

              <Grid size={{ xs: 12, sm: 6 }}>
                <InputLabel required sx={{ mb: 0.5 }}>Giảng viên coi thi 2</InputLabel>
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

              <Grid size={{ xs: 12 }}>
                <InputLabel required sx={{ mb: 0.5 }}>Học kỳ</InputLabel>
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

              <Grid size={{ xs: 12, sm: 4 }}>
                <InputLabel required sx={{ mb: 0.5 }}>Ngày thi</InputLabel>
                <TextField
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

              <Grid size={{ xs: 12, sm: 4 }}>
                <InputLabel required sx={{ mb: 0.5 }}>Giờ bắt đầu</InputLabel>
                <TextField
                  name="startTime"
                  type="time"
                  fullWidth
                  size="small"
                  value={formik.values.startTime}
                  onChange={formik.handleChange}
                  onBlur={formik.handleBlur}
                  error={formik.touched.startTime && Boolean(formik.errors.startTime)}
                  helperText={formik.touched.startTime && formik.errors.startTime}
                  slotProps={{ inputLabel: { shrink: true } }}
                />
              </Grid>

              <Grid size={{ xs: 12, sm: 4 }}>
                <InputLabel required sx={{ mb: 0.5 }}>Giờ kết thúc</InputLabel>
                <TextField
                  name="endTime"
                  type="time"
                  fullWidth
                  size="small"
                  value={formik.values.endTime}
                  onChange={formik.handleChange}
                  onBlur={formik.handleBlur}
                  error={formik.touched.endTime && Boolean(formik.errors.endTime)}
                  helperText={formik.touched.endTime && formik.errors.endTime}
                  slotProps={{ inputLabel: { shrink: true } }}
                />
              </Grid>
            </Grid>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button onClick={handleClose} disabled={formik.isSubmitting}>Hủy</Button>
          <Button
            type="submit"
            variant="contained"
            disabled={formik.isSubmitting}
            startIcon={formik.isSubmitting ? <CircularProgress size={14} color="inherit" /> : undefined}
          >
            {editItem ? 'Lưu thay đổi' : 'Thêm'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}

export default function ExamRoomPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const isAdmin = user?.roleId === 1;
  const isTeacher = user?.roleId === 2;

  const [data, setData] = useState<ExamRoom[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [pageNumber, setPageNumber] = useState(0);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [keyword, setKeyword] = useState('');
  const [keywordInput, setKeywordInput] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [reload, setReload] = useState(false);
  const [tableLoading, setTableLoading] = useState(false);

  const [alert, setAlert] = useState({ open: false, message: '', severity: 'success' as 'success' | 'error' | 'info' | 'warning' });

  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [editItem, setEditItem] = useState<ExamRoom | null>(null);
  const [deleteItem, setDeleteItem] = useState<ExamRoom | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  const [dropdownData, setDropdownData] = useState<DropdownData>({
    teachers: [],
    subjects: [],
    semesters: [],
    rooms: [],
    loading: false
  });

  const importFileRef = useRef<HTMLInputElement>(null);
  const [importTargetId, setImportTargetId] = useState<number | null>(null);

  // Load dropdown data once
  useEffect(() => {
    setDropdownData((prev) => ({ ...prev, loading: true }));
    Promise.all([
      getTeacherList({ page: 0, size: 1000, keyword: '', status: '1' }),
      getSubjectList({ page: 0, size: 1000, keyword: '', status: '1' }),
      getSemesterList({ page: 0, size: 1000, keyword: '', status: '1' }),
      getRoomList({ page: 0, size: 1000, keyword: '', status: '1', sort: '' })
    ]).then(([resT, resS, resSem, resR]) => {
      setDropdownData({
        teachers: resT.statusCode === HttpStatusCode.Ok ? resT.data.content : [],
        subjects: resS.statusCode === HttpStatusCode.Ok ? resS.data.content : [],
        semesters: resSem.statusCode === HttpStatusCode.Ok ? resSem.data.content : [],
        rooms: resR.statusCode === HttpStatusCode.Ok ? resR.data.content : [],
        loading: false
      });
    });
  }, []);

  // Load table data
  useEffect(() => {
    setTableLoading(true);
    getList({ page: pageNumber, size: pageSize, keyword, sort: '', status: statusFilter })
      .then((res) => {
        if (res.statusCode === HttpStatusCode.Ok) {
          setData(res.data.content);
          setTotalPages(res.data.totalPages);
          setTotalElements(res.data.totalElements);
        } else if (res.statusCode === HttpStatusCode.Unauthorized) {
          logout();
        }
      })
      .finally(() => setTableLoading(false));
  }, [pageNumber, pageSize, keyword, statusFilter, reload, logout]);

  const handleDelete = async () => {
    if (!deleteItem) return;
    setDeleteLoading(true);
    const res = await deleteById(deleteItem.id);
    setDeleteLoading(false);
    setDeleteItem(null);
    if (res.statusCode === HttpStatusCode.Ok) {
      setAlert({ open: true, message: 'Xóa phòng thi thành công', severity: 'success' });
      setReload((p) => !p);
    } else if (res.statusCode === HttpStatusCode.UnprocessableEntity) {
      setAlert({ open: true, message: res.message ?? 'Không thể xóa', severity: 'error' });
    } else {
      setAlert({ open: true, message: 'Lỗi hệ thống', severity: 'error' });
    }
  };

  const handleImportStudents = async (event: ChangeEvent<HTMLInputElement>) => {
    if (!importTargetId) return;
    const file = event.target.files?.[0];
    if (!file) return;
    const formData = new FormData();
    formData.append('file', file);
    event.target.value = '';
    const res = await importStudents(importTargetId, formData);
    if (res.statusCode === HttpStatusCode.Ok) {
      const errors: string[] = res.data ?? [];
      if (errors.length > 0) {
        setAlert({ open: true, message: `Import xong. Lỗi: ${errors.join('; ')}`, severity: 'warning' });
      } else {
        setAlert({ open: true, message: 'Import sinh viên thành công', severity: 'success' });
      }
      setReload((p) => !p);
    } else if (res.statusCode === HttpStatusCode.UnprocessableEntity) {
      setAlert({ open: true, message: res.message ?? 'Import thất bại', severity: 'error' });
    } else {
      setAlert({ open: true, message: 'Lỗi hệ thống', severity: 'error' });
    }
    setImportTargetId(null);
  };

  const handleDownloadTemplate = async () => {
    const blob = await downloadClassStudentImportTemplate();
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'Mẫu import sinh viên vào phòng thi.xlsx';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const pageSizeOptions = [10, 25, 50, 100];

  return (
    <Stack sx={{ p: 0 }}>
      <Snackbar
        open={alert.open}
        autoHideDuration={4000}
        onClose={() => setAlert((p) => ({ ...p, open: false }))}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Alert severity={alert.severity} variant="filled" sx={{ width: '100%', borderRadius: 2, fontSize: 15 }}>
          {alert.message}
        </Alert>
      </Snackbar>

      <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ gap: 2, justifyContent: 'space-between', pb: 3, alignItems: 'center' }}>
        <Typography variant="h3">Danh sách phòng thi</Typography>

        {isAdmin && (
          <Stack direction="row" spacing={2}>
            <Button variant="contained" color="primary" startIcon={<ExportCurve />} onClick={handleDownloadTemplate}>
              Xuất file mẫu
            </Button>
            <Divider orientation="vertical" flexItem />
            <Button variant="contained" startIcon={<Add />} onClick={() => setAddDialogOpen(true)}>
              Thêm phòng thi
            </Button>
          </Stack>
        )}
      </Stack>

      <MainCard content={false}>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          sx={{ gap: 2, justifyContent: 'space-between', p: 2 }}
        >
          <Stack direction="row" spacing={2} sx={{ flexWrap: 'wrap' }}>
            <OutlinedInput
              value={keywordInput}
              onChange={(e) => setKeywordInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  setPageNumber(0);
                  setKeyword(keywordInput);
                }
              }}
              placeholder="Tìm kiếm mã phòng..."
              sx={{ minWidth: 200 }}
            />
            <Select
              value={statusFilter}
              onChange={(e: SelectChangeEvent) => { setStatusFilter(e.target.value); setPageNumber(0); }}
              displayEmpty
              input={<OutlinedInput />}
            >
              <MenuItem value="">Trạng thái</MenuItem>
              <MenuItem value="1">Hoạt động</MenuItem>
              <MenuItem value="0">Dừng hoạt động</MenuItem>
            </Select>
          </Stack>
          <Typography variant="caption" color="secondary" sx={{ display: 'flex', alignItems: 'center' }}>
            Tổng cộng: {totalElements} bản ghi
          </Typography>
        </Stack>

        <TableContainer component={Paper} elevation={0}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>#</TableCell>
                <TableCell>Mã phòng</TableCell>
                <TableCell>Môn học</TableCell>
                <TableCell>Phòng</TableCell>
                <TableCell>GV coi thi 1</TableCell>
                <TableCell>GV coi thi 2</TableCell>
                <TableCell>Ngày thi</TableCell>
                <TableCell>Giờ thi</TableCell>
                <TableCell>Học kỳ</TableCell>
                <TableCell align="center">Sĩ số</TableCell>
                <TableCell align="center">Trạng thái</TableCell>
                <TableCell align="center">Hành động</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {tableLoading ? (
                <TableRow>
                  <TableCell colSpan={12} align="center" sx={{ py: 4 }}>
                    <CircularProgress size={28} />
                  </TableCell>
                </TableRow>
              ) : data.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={12} align="center" sx={{ py: 4 }}>
                    <Typography color="text.secondary">Không có dữ liệu</Typography>
                  </TableCell>
                </TableRow>
              ) : (
                data.map((row, idx) => (
                  <TableRow key={row.id} hover>
                    <TableCell>{pageNumber * pageSize + idx + 1}</TableCell>
                    <TableCell><Typography variant="body2" fontWeight="medium">{row.code}</Typography></TableCell>
                    <TableCell>{row.subjectName}</TableCell>
                    <TableCell>{row.roomName}</TableCell>
                    <TableCell>{row.teacher1Name}</TableCell>
                    <TableCell>{row.teacher2Name}</TableCell>
                    <TableCell>{formatDate(row.examDate)}</TableCell>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatTimeWithoutSecond(row.startTime)} – {formatTimeWithoutSecond(row.endTime)}</TableCell>
                    <TableCell>{row.semesterName}</TableCell>
                    <TableCell align="center">
                      <Typography
                        variant="body2"
                        sx={{ color: row.currentStudent >= row.maxStudent && row.maxStudent > 0 ? 'error.main' : 'text.primary' }}
                      >
                        {row.currentStudent}/{row.maxStudent}
                      </Typography>
                    </TableCell>
                    <TableCell align="center">
                      <Chip
                        label={row.status === 1 ? 'Hoạt động' : 'Dừng'}
                        color={row.status === 1 ? 'success' : 'error'}
                        size="small"
                      />
                    </TableCell>
                    <TableCell align="center">
                      <Stack direction="row" spacing={0.5} justifyContent="center">
                        {isTeacher && (
                          <Tooltip title="Giám sát phòng thi">
                            <span>
                              <IconButton
                                color="primary"
                                size="small"
                                onClick={() => navigate(`/teacher/exam-room/${row.id}/tracking`)}
                                disabled={row.status === 0}
                              >
                                <Eye variant="Outline" />
                              </IconButton>
                            </span>
                          </Tooltip>
                        )}
                        {isAdmin && (
                          <>
                            <Tooltip title={row.status === 0 ? 'Không thể chỉnh sửa' : 'Chỉnh sửa'}>
                              <span>
                                <IconButton
                                  color="primary"
                                  size="small"
                                  onClick={() => setEditItem(row)}
                                  disabled={row.status === 0}
                                >
                                  <Edit2 variant="Outline" />
                                </IconButton>
                              </span>
                            </Tooltip>
                            <Tooltip title={row.status === 0 ? 'Không thể xóa' : 'Xóa'}>
                              <span>
                                <IconButton
                                  color="error"
                                  size="small"
                                  onClick={() => setDeleteItem(row)}
                                  disabled={row.status === 0}
                                >
                                  <Trash variant="Outline" />
                                </IconButton>
                              </span>
                            </Tooltip>
                            <Tooltip title={row.status === 0 ? 'Không thể import' : 'Import sinh viên'}>
                              <span>
                                <IconButton
                                  color="primary"
                                  size="small"
                                  onClick={() => { setImportTargetId(row.id); importFileRef.current?.click(); }}
                                  disabled={row.status === 0}
                                >
                                  <ImportCurve variant="Outline" />
                                </IconButton>
                              </span>
                            </Tooltip>
                          </>
                        )}
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>

        <input
          ref={importFileRef}
          type="file"
          accept=".xlsx,.xls"
          style={{ display: 'none' }}
          onChange={handleImportStudents}
        />

        <Divider />

        <Grid container sx={{ alignItems: 'center', justifyContent: 'space-between', px: 2, py: 1.5 }}>
          <Grid>
            <Stack direction="row" spacing={1} alignItems="center">
              <Typography variant="caption" color="secondary">Số bản ghi mỗi trang</Typography>
              <FormControl size="small">
                <Select
                  value={pageSize}
                  onChange={(e: SelectChangeEvent<number>) => { setPageSize(Number(e.target.value)); setPageNumber(0); }}
                  sx={{ '& .MuiSelect-select': { py: 0.75, px: 1.25 } }}
                >
                  {pageSizeOptions.map((o) => <MenuItem key={o} value={o}>{o}</MenuItem>)}
                </Select>
              </FormControl>
            </Stack>
          </Grid>
          <Grid sx={{ mb: 1 }}>
            <Pagination
              count={totalPages}
              variant="contained"
              page={pageNumber + 1}
              onChange={(_, value) => setPageNumber(value - 1)}
              color="primary"
              showFirstButton
              showLastButton
            />
          </Grid>
        </Grid>
      </MainCard>

      {/* Add dialog */}
      <ExamRoomFormDialog
        open={addDialogOpen}
        onClose={() => setAddDialogOpen(false)}
        onSuccess={(msg) => { setAlert({ open: true, message: msg, severity: 'success' }); setReload((p) => !p); }}
        editItem={null}
        dropdownData={dropdownData}
      />

      {/* Edit dialog */}
      <ExamRoomFormDialog
        open={!!editItem}
        onClose={() => setEditItem(null)}
        onSuccess={(msg) => { setAlert({ open: true, message: msg, severity: 'success' }); setReload((p) => !p); }}
        editItem={editItem}
        dropdownData={dropdownData}
      />

      {/* Delete confirm dialog */}
      <Dialog open={!!deleteItem} onClose={() => setDeleteItem(null)}>
        <DialogTitle>Xác nhận xóa phòng thi?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Bạn có chắc muốn xóa phòng thi <strong>{deleteItem?.code}</strong>? Tất cả dữ liệu liên quan sẽ bị xóa.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteItem(null)} disabled={deleteLoading}>Hủy</Button>
          <Button variant="contained" color="error" onClick={handleDelete} disabled={deleteLoading}
            startIcon={deleteLoading ? <CircularProgress size={14} color="inherit" /> : undefined}>
            Xóa
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}