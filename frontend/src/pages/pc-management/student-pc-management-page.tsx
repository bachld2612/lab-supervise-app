import { CSSProperties, Fragment, useCallback, useEffect, useMemo, useState } from 'react';

import {
  DndContext,
  KeyboardSensor,
  MouseSensor,
  TouchSensor,
  closestCenter,
  type DragEndEvent,
  type UniqueIdentifier,
  useSensor,
  useSensors
} from '@dnd-kit/core';
import { restrictToHorizontalAxis, restrictToVerticalAxis } from '@dnd-kit/modifiers';
import { arrayMove, SortableContext, horizontalListSortingStrategy, verticalListSortingStrategy, useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import {
  ColumnDef,
  ColumnFiltersState,
  Header,
  Row,
  SortingState,
  flexRender,
  getCoreRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  useReactTable
} from '@tanstack/react-table';
import { HttpStatusCode } from 'axios';
import {
  Alert,
  Autocomplete,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  Grid,
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
import { Edit } from 'iconsax-reactjs';

import IconButton from 'components/@extended/IconButton';
import MainCard from 'components/MainCard';
import { EmptyTable, HeaderSort } from 'components/third-party/react-table';
import useAuth from 'hooks/useAuth';
import { getTeacherClasses } from 'api/class';
import { getTeacherExamRooms } from 'api/exam-room';
import { getStudentsByClassId, getStudentsByExamRoomId, updateStudentPcIp } from 'api/personal-computer';
import { Classes } from 'types/classes';
import { ExamRoom } from 'types/exam-room';
import { StudentPcInfo } from 'types/student-pc-info';

type FilterType = 'class' | 'exam-room';

const inputSx = {
  '& .MuiInputBase-root': { height: 40 },
  '& .MuiOutlinedInput-root': { height: 40 }
};

const nonOrderableColumnId: UniqueIdentifier[] = ['edit'];

function DraggableTableHeader({ header }: { header: Header<any, unknown> }) {
  const { attributes, isDragging, listeners, setNodeRef, transform } = useSortable({ id: header.column.id });

  const style: CSSProperties = {
    opacity: isDragging ? 0.8 : 1,
    position: 'relative',
    transform: CSS.Translate.toString(transform),
    transition: 'width transform 0.2s ease-in-out',
    whiteSpace: 'nowrap',
    width: header.column.getSize(),
    zIndex: isDragging ? 1 : 0
  };

  return (
    <TableCell colSpan={header.colSpan} ref={setNodeRef} style={style} {...header.column.columnDef.meta}>
      {header.isPlaceholder ? null : (
        <Stack direction="row" sx={{ gap: 1, alignItems: 'center', justifyContent: 'space-between' }}>
          <Box {...(!nonOrderableColumnId.includes(header.id) && { ...attributes, ...listeners, sx: { cursor: 'move' } })}>
            {flexRender(header.column.columnDef.header, header.getContext())}
          </Box>
          {header.column.getCanSort() && <HeaderSort column={header.column} sort />}
        </Stack>
      )}
    </TableCell>
  );
}

function DraggableRow({ row }: { row: Row<StudentPcInfo> }) {
  const { transform, transition, setNodeRef, isDragging } = useSortable({ id: row.original.userId });

  const style: CSSProperties = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.8 : 1,
    zIndex: isDragging ? 1 : 0,
    position: 'relative'
  };

  return (
    <TableRow ref={setNodeRef} style={style}>
      {row.getVisibleCells().map((cell) => (
        <TableCell
          key={cell.id}
          {...cell.column.columnDef.meta}
          sx={{
            width:
              cell.column.columnDef.meta && 'width' in cell.column.columnDef.meta
                ? (cell.column.columnDef.meta as { width?: string | number }).width
                : undefined,
            verticalAlign: 'middle'
          }}
        >
          {flexRender(cell.column.columnDef.cell, cell.getContext())}
        </TableCell>
      ))}
    </TableRow>
  );
}

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
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  const [editTarget, setEditTarget] = useState<StudentPcInfo | null>(null);
  const [editIp, setEditIp] = useState('');
  const [saving, setSaving] = useState(false);
  const [alert, setAlert] = useState({ open: false, message: '', severity: 'success' as 'success' | 'error' });

  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
  const [sorting, setSorting] = useState<SortingState>([]);
  const [columnOrder, setColumnOrder] = useState<string[]>([]);

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
    setPage(0);
    loadOptions();
  }, [filterType, loadOptions]);

  const loadStudents = useCallback(
    async (id: number) => {
      setLoadingStudents(true);
      setStudents([]);
      setPage(0);
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

  const openEditDialog = (student: StudentPcInfo) => {
    setEditTarget(student);
    setEditIp(student.ipAddress || '');
  };

  const handleSaveIp = async () => {
    if (!editTarget) return;
    setSaving(true);
    try {
      const res = await updateStudentPcIp(editTarget.userId, editIp.trim());
      if (res.statusCode === HttpStatusCode.Ok) {
        setStudents((prev) =>
          prev.map((student) => (student.userId === editTarget.userId ? { ...student, ipAddress: editIp.trim() } : student))
        );
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

  const pageRows = useMemo(() => students.slice(page * size, page * size + size), [students, page, size]);
  const totalPages = Math.ceil(students.length / size);
  const dataIds = useMemo<UniqueIdentifier[]>(() => pageRows.map((student) => student.userId), [pageRows]);
  const options = [10, 25, 50, 100];

  const columns = useMemo<ColumnDef<StudentPcInfo>[]>(
    () => [
      {
        id: 'stt',
        header: 'STT',
        cell: ({ row }) => page * size + row.index + 1,
        enableSorting: false,
        enableGrouping: false,
        meta: { className: 'cell-center', width: '8%' }
      },
      {
        id: 'fullName',
        header: 'Tên sinh viên',
        accessorKey: 'fullName',
        enableGrouping: false
      },
      {
        id: 'code',
        header: 'Mã sinh viên',
        accessorKey: 'code',
        enableGrouping: false
      },
      {
        id: 'ipAddress',
        header: 'Địa chỉ IP',
        cell: ({ row }) => (
          <Box
            component="span"
            sx={{
              color: row.original.ipAddress ? 'text.primary' : 'text.disabled',
              fontStyle: row.original.ipAddress ? 'normal' : 'italic'
            }}
          >
            {row.original.ipAddress || 'Chưa cài đặt'}
          </Box>
        ),
        enableGrouping: false
      },
      {
        id: 'edit',
        header: 'Hành động',
        cell: ({ row }) => (
          <Tooltip title="Cập nhật IP">
            <IconButton color="primary" size="small" onClick={() => openEditDialog(row.original)}>
              <Edit size={18} variant="Outline" />
            </IconButton>
          </Tooltip>
        ),
        enableSorting: false,
        enableGrouping: false,
        meta: { className: 'cell-center', width: '10%' }
      }
    ],
    [page, size]
  );

  useEffect(() => setColumnOrder(columns.map((column) => column.id!)), [columns]);

  const table = useReactTable({
    data: pageRows,
    columns,
    manualPagination: true,
    getRowId: (row: StudentPcInfo) => row.userId.toString(),
    state: { columnFilters, sorting, columnOrder },
    onSortingChange: setSorting,
    onColumnFiltersChange: setColumnFilters,
    onColumnOrderChange: setColumnOrder,
    getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel()
  });

  function handleColumnDragEnd(event: DragEndEvent) {
    const { active, over } = event;
    if (active && over && active.id !== over.id) {
      if (nonOrderableColumnId.includes(over.id)) return;
      setColumnOrder((order) => arrayMove(order, order.indexOf(active.id as string), order.indexOf(over.id as string)));
    }
  }

  function handleRowDragEnd(event: DragEndEvent) {
    const { active, over } = event;
    if (active && over && active.id !== over.id) {
      const start = page * size;
      const oldIndex = start + dataIds.indexOf(active.id);
      const newIndex = start + dataIds.indexOf(over.id);
      setStudents((data) => arrayMove(data, oldIndex, newIndex));
    }
  }

  const columnSensors = useSensors(useSensor(MouseSensor, {}), useSensor(TouchSensor, {}), useSensor(KeyboardSensor, {}));
  const rowSensors = useSensors(useSensor(MouseSensor, {}), useSensor(TouchSensor, {}), useSensor(KeyboardSensor, {}));

  const handleChangePageSize = (event: SelectChangeEvent<number>) => {
    setSize(Number(event.target.value));
    setPage(0);
  };

  return (
    <Stack sx={{ p: 0 }}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        sx={(theme) => ({
          gap: 2,
          justifyContent: 'space-between',
          pb: 3,
          [theme.breakpoints.down('sm')]: { '& .MuiOutlinedInput-root, & .MuiFormControl-root': { width: 1 } }
        })}
      >
        <Typography variant="h3" gutterBottom>
          Quản lý IP sinh viên
        </Typography>
      </Stack>

      <MainCard content={false}>
        <Snackbar
          open={alert.open}
          autoHideDuration={3000}
          onClose={() => setAlert((a) => ({ ...a, open: false }))}
          anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
        >
          <Alert
            severity={alert.severity}
            variant="filled"
            sx={{ width: '100%', borderRadius: 2, fontSize: 15, textAlign: 'center', py: 1.5, px: 2 }}
          >
            {alert.message}
          </Alert>
        </Snackbar>

        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          sx={(theme) => ({
            gap: 2,
            justifyContent: 'left',
            p: 2,
            [theme.breakpoints.down('sm')]: { '& .MuiOutlinedInput-root, & .MuiFormControl-root': { width: 1 } }
          })}
        >
          <Stack direction="row" spacing={2} sx={{ flexGrow: 1, flexWrap: 'wrap' }}>
            <Select
              size="small"
              value={filterType}
              onChange={(event) => setFilterType(event.target.value as FilterType)}
              displayEmpty
              input={<OutlinedInput />}
              sx={{ minWidth: 180, height: 40 }}
            >
              <MenuItem value="class">Lớp học phần</MenuItem>
              <MenuItem value="exam-room">Phòng thi</MenuItem>
            </Select>

            {filterType === 'class' ? (
              <Autocomplete
                size="small"
                sx={{ minWidth: 300, ...inputSx }}
                options={classOptions}
                getOptionLabel={(option) => option.name}
                value={selectedClass}
                onChange={(_, value) => {
                  setSelectedClass(value);
                  setPage(0);
                }}
                inputValue={classInput}
                onInputChange={(_, value) => setClassInput(value)}
                loading={loadingOptions}
                isOptionEqualToValue={(option, value) => option.id === value.id}
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
                size="small"
                sx={{ minWidth: 300, ...inputSx }}
                options={examRoomOptions}
                getOptionLabel={(option) => option.code || `Phòng thi #${option.id}`}
                value={selectedExamRoom}
                onChange={(_, value) => {
                  setSelectedExamRoom(value);
                  setPage(0);
                }}
                inputValue={examRoomInput}
                onInputChange={(_, value) => setExamRoomInput(value)}
                loading={loadingOptions}
                isOptionEqualToValue={(option, value) => option.id === value.id}
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

          <Typography variant="caption" color="secondary" sx={{ display: 'flex', alignItems: 'center' }}>
            Tổng cộng: {students.length} bản ghi
          </Typography>
        </Stack>

        <DndContext
          collisionDetection={closestCenter}
          modifiers={[restrictToHorizontalAxis]}
          onDragEnd={handleColumnDragEnd}
          sensors={columnSensors}
        >
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                {table.getHeaderGroups().map((headerGroup) => (
                  <TableRow key={headerGroup.id}>
                    <SortableContext items={columnOrder} strategy={horizontalListSortingStrategy}>
                      {headerGroup.headers.map((header) => (
                        <DraggableTableHeader key={header.id} header={header} />
                      ))}
                    </SortableContext>
                  </TableRow>
                ))}
              </TableHead>

              <TableBody>
                {loadingStudents ? (
                  <TableRow>
                    <TableCell colSpan={table.getAllColumns().length} align="center" sx={{ py: 4 }}>
                      <CircularProgress size={24} />
                    </TableCell>
                  </TableRow>
                ) : (
                  <DndContext
                    collisionDetection={closestCenter}
                    modifiers={[restrictToVerticalAxis]}
                    onDragEnd={handleRowDragEnd}
                    sensors={rowSensors}
                  >
                    {table.getRowModel().rows.length > 0 ? (
                      <SortableContext items={dataIds} strategy={verticalListSortingStrategy}>
                        {table.getRowModel().rows.map((row) => (
                          <Fragment key={row.id}>
                            <DraggableRow row={row} />
                          </Fragment>
                        ))}
                      </SortableContext>
                    ) : (
                      <TableRow sx={{ '&.MuiTableRow-root:hover': { bgcolor: 'transparent' } }}>
                        <TableCell colSpan={table.getAllColumns().length}>
                          <EmptyTable
                            msg={
                              selectedClass || selectedExamRoom ? 'Không có sinh viên' : 'Chọn lớp học phần hoặc phòng thi để xem danh sách'
                            }
                          />
                        </TableCell>
                      </TableRow>
                    )}
                  </DndContext>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </DndContext>

        <Divider />

        <Grid spacing={1} container sx={{ alignItems: 'center', justifyContent: 'space-between', width: 'auto', mt: 2 }}>
          <Grid>
            <Stack direction="row" sx={{ gap: 1, alignItems: 'center', marginLeft: 2 }}>
              <Typography variant="caption" color="secondary">
                Số bản ghi mỗi trang
              </Typography>

              <FormControl sx={{ m: 1 }}>
                <Select value={size} onChange={handleChangePageSize} size="small" sx={{ '& .MuiSelect-select': { py: 0.75, px: 1.25 } }}>
                  {options.map((option) => (
                    <MenuItem key={option} value={option}>
                      {option}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Stack>
          </Grid>

          <Grid sx={{ mt: { xs: 2, sm: 0 } }}>
            <Pagination
              count={totalPages}
              variant="contained"
              page={page + 1}
              onChange={(_, value) => setPage(value - 1)}
              color="primary"
              showFirstButton
              showLastButton
            />
          </Grid>
        </Grid>
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
            onChange={(event) => setEditIp(event.target.value)}
            placeholder="Ví dụ: 192.168.1.100"
            disabled={saving}
            onKeyDown={(event) => {
              if (event.key === 'Enter') handleSaveIp();
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
