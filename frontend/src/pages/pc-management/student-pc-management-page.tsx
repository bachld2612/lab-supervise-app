import { useEffect, useState, useCallback } from 'react';

import Autocomplete from '@mui/material/Autocomplete';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import Snackbar from '@mui/material/Snackbar';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import Paper from '@mui/material/Paper';
import OutlinedInput from '@mui/material/OutlinedInput';

import MainCard from 'components/MainCard';
import IconButton from 'components/@extended/IconButton';
import { Edit } from 'iconsax-reactjs';

import { HttpStatusCode } from 'axios';
import useAuth from 'hooks/useAuth';

import { getTeacherClasses } from 'api/class';
import { getTeacherExamRooms } from 'api/exam-room';
import { getStudentsByClassId, getStudentsByExamRoomId, updateStudentPcIp } from 'api/personal-computer';

import { Classes } from 'types/classes';
import { ExamRoom } from 'types/exam-room';
import { StudentPcInfo } from 'types/student-pc-info';

type FilterType = 'class' | 'exam-room';

export default function StudentPcManagementPage() {
  const { logout } = useAuth();

  const [filterType, setFilterType] = useState<FilterType>('class');
  const [classOptions, setClassOptions] = useState<Classes[]>([]);
  const [examRoomOptions, setExamRoomOptions] = useState<ExamRoom[]>([]);
  const [classInput, setClassInput] = useState('');
  const [examRoomInput, setExamRoomInput] = useState('');
  const [selectedClass, setSelectedClass] = useState<Classes | null>(null);
  const [selectedExamRoom, setSelectedExamRoom] = useState<ExamRoom | null>(null);
  const [loadingOptions, setLoadingOptions] = useState(false);

  const [students, setStudents] = useState<StudentPcInfo[]>([]);
  const [loadingStudents, setLoadingStudents] = useState(false);

  const [editTarget, setEditTarget] = useState<StudentPcInfo | null>(null);
  const [editIp, setEditIp] = useState('');
  const [saving, setSaving] = useState(false);

  const [alert, setAlert] = useState({ open: false, message: '', severity: 'success' as 'success' | 'error' });

  const loadOptions = useCallback(async () => {
    setLoadingOptions(true);
    try {
      if (filterType === 'class') {
        const res = await getTeacherClasses();
        if (res.statusCode === HttpStatusCode.Ok) setClassOptions(res.data || []);
        else if (res.statusCode === HttpStatusCode.Unauthorized) logout();
      } else {
        const res = await getTeacherExamRooms();
        if (res.statusCode === HttpStatusCode.Ok) setExamRoomOptions(res.data || []);
        else if (res.statusCode === HttpStatusCode.Unauthorized) logout();
      }
    } finally {
      setLoadingOptions(false);
    }
  }, [filterType, logout]);

  useEffect(() => {
    setSelectedClass(null);
    setSelectedExamRoom(null);
    setClassInput('');
    setExamRoomInput('');
    setStudents([]);
    loadOptions();
  }, [filterType]);

  const loadStudents = useCallback(
    async (id: number) => {
      setLoadingStudents(true);
      setStudents([]);
      try {
        const res = filterType === 'class' ? await getStudentsByClassId(id) : await getStudentsByExamRoomId(id);
        if (res.statusCode === HttpStatusCode.Ok) setStudents(res.data || []);
        else if (res.statusCode === HttpStatusCode.Unauthorized) logout();
      } finally {
        setLoadingStudents(false);
      }
    },
    [filterType, logout]
  );

  useEffect(() => {
    if (filterType === 'class' && selectedClass) loadStudents(selectedClass.id);
    else if (filterType === 'exam-room' && selectedExamRoom) loadStudents(selectedExamRoom.id);
    else setStudents([]);
  }, [selectedClass, selectedExamRoom, filterType, loadStudents]);

  const openEditDialog = (s: StudentPcInfo) => {
    setEditTarget(s);
    setEditIp(s.ipAddress || '');
  };

  const handleSaveIp = async () => {
    if (!editTarget) return;
    setSaving(true);
    try {
      const res = await updateStudentPcIp(editTarget.userId, editIp.trim());
      if (res.statusCode === HttpStatusCode.Ok) {
        setStudents((prev) => prev.map((s) => (s.userId === editTarget.userId ? { ...s, ipAddress: editIp.trim() } : s)));
        setAlert({ open: true, message: 'Cập nhật địa chỉ IP thành công', severity: 'success' });
        setEditTarget(null);
      } else if (res.statusCode === HttpStatusCode.Unauthorized) {
        logout();
      } else {
        setAlert({ open: true, message: 'Cập nhật thất bại', severity: 'error' });
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <Stack sx={{ p: 0 }}>
      <Typography variant="h3" gutterBottom>
        Quản lý máy tính sinh viên
      </Typography>

      <MainCard content={false}>
        <Snackbar
          open={alert.open}
          autoHideDuration={3000}
          onClose={() => setAlert((a) => ({ ...a, open: false }))}
          anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
        >
          <Alert severity={alert.severity} variant="filled" sx={{ width: '100%' }}>
            {alert.message}
          </Alert>
        </Snackbar>

        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ p: 2, flexWrap: 'wrap' }}>
          <Select
            value={filterType}
            onChange={(e) => setFilterType(e.target.value as FilterType)}
            input={<OutlinedInput />}
            sx={{ minWidth: 180 }}
          >
            <MenuItem value="class">Lớp học phần</MenuItem>
            <MenuItem value="exam-room">Phòng thi</MenuItem>
          </Select>

          {filterType === 'class' ? (
            <Autocomplete
              sx={{ minWidth: 300 }}
              options={classOptions}
              getOptionLabel={(o) => o.name}
              value={selectedClass}
              onChange={(_, v) => setSelectedClass(v)}
              inputValue={classInput}
              onInputChange={(_, v) => setClassInput(v)}
              loading={loadingOptions}
              isOptionEqualToValue={(o, v) => o.id === v.id}
              renderInput={(params) => (
                <TextField
                  {...params}
                  placeholder="Tìm lớp học phần..."
                  slotProps={{
                    input: {
                      ...params.InputProps,
                      endAdornment: (
                        <>
                          {loadingOptions && <CircularProgress size={16} />}
                          {params.InputProps.endAdornment}
                        </>
                      )
                    }
                  }}
                />
              )}
            />
          ) : (
            <Autocomplete
              sx={{ minWidth: 300 }}
              options={examRoomOptions}
              getOptionLabel={(o) => o.code || `Phòng thi #${o.id}`}
              value={selectedExamRoom}
              onChange={(_, v) => setSelectedExamRoom(v)}
              inputValue={examRoomInput}
              onInputChange={(_, v) => setExamRoomInput(v)}
              loading={loadingOptions}
              isOptionEqualToValue={(o, v) => o.id === v.id}
              renderInput={(params) => (
                <TextField
                  {...params}
                  placeholder="Tìm phòng thi..."
                  slotProps={{
                    input: {
                      ...params.InputProps,
                      endAdornment: (
                        <>
                          {loadingOptions && <CircularProgress size={16} />}
                          {params.InputProps.endAdornment}
                        </>
                      )
                    }
                  }}
                />
              )}
            />
          )}
        </Stack>

        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>STT</TableCell>
                <TableCell>Tên sinh viên</TableCell>
                <TableCell>Mã sinh viên</TableCell>
                <TableCell>Địa chỉ IP</TableCell>
                <TableCell align="center">Hành động</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loadingStudents ? (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 4 }}>
                    <CircularProgress size={24} />
                  </TableCell>
                </TableRow>
              ) : students.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    {selectedClass || selectedExamRoom ? 'Không có sinh viên' : 'Chọn lớp học phần hoặc phòng thi để xem danh sách'}
                  </TableCell>
                </TableRow>
              ) : (
                students.map((s, idx) => (
                  <TableRow key={s.userId} hover>
                    <TableCell>{idx + 1}</TableCell>
                    <TableCell>{s.fullName}</TableCell>
                    <TableCell>{s.code}</TableCell>
                    <TableCell>
                      <Box
                        component="span"
                        sx={{ color: s.ipAddress ? 'text.primary' : 'text.disabled', fontStyle: s.ipAddress ? 'normal' : 'italic' }}
                      >
                        {s.ipAddress || 'Chưa cài đặt'}
                      </Box>
                    </TableCell>
                    <TableCell align="center">
                      <Tooltip title="Cập nhật IP">
                        <IconButton color="primary" size="small" onClick={() => openEditDialog(s)}>
                          <Edit size={18} variant="Outline" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </MainCard>

      <Dialog open={!!editTarget} onClose={() => setEditTarget(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Cập nhật địa chỉ IP</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {editTarget?.fullName} ({editTarget?.code})
          </Typography>
          <TextField
            fullWidth
            label="Địa chỉ IP"
            value={editIp}
            onChange={(e) => setEditIp(e.target.value)}
            placeholder="Ví dụ: 192.168.1.100"
            disabled={saving}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleSaveIp();
            }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditTarget(null)} disabled={saving}>
            Hủy
          </Button>
          <Button onClick={handleSaveIp} variant="contained" disabled={saving || !editIp.trim()}>
            Lưu
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
