import { ChangeEvent, CSSProperties, Fragment, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Alert,
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
  Tooltip,
  Typography
} from '@mui/material';
import MainCard from 'components/MainCard';
import IconButton from 'components/@extended/IconButton';
import { Add, Edit2, Eye, ExportCurve, ImportCurve, ProfileAdd, ProfileRemove, Trash } from 'iconsax-reactjs';
import { HttpStatusCode } from 'axios';
import useAuth from 'hooks/useAuth';
import { ExamRoom } from 'types/exam-room';
import { getList, deleteById, importStudents } from 'api/exam-room';
import { downloadClassStudentImportTemplate } from 'api/class';
import { DEFAULT_PAGE_SIZE } from 'types/paging';
import ManageExamRoomStudentDialog from 'sections/extra-pages/exam-room/manage-exam-room-student-dialog';
import ExamRoomStudentListDialog from 'sections/extra-pages/exam-room/exam-room-student-list-dialog';
import formatDate, { formatTimeWithoutSecond } from 'utils/formatDate';
import { parsePeriodValues } from 'sections/extra-pages/exam-room/form-helpers';
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
import { EmptyTable, HeaderSort } from 'components/third-party/react-table';

const formatPeriodRange = (periods?: string, startTime?: string, endTime?: string) => {
  const values = parsePeriodValues(periods);
  const timeRange = `${formatTimeWithoutSecond(startTime ?? '')} - ${formatTimeWithoutSecond(endTime ?? '')}`;

  if (values.length === 0) return timeRange;
  if (values.length === 1) return `Tiết ${values[0]} (${timeRange})`;

  return `Tiết ${values[0]} - Tiết ${values[values.length - 1]} (${timeRange})`;
};

function ExamRoomStudentCountCell({ examRoom }: { examRoom: ExamRoom }) {
  const [open, setOpen] = useState(false);
  const isFull = examRoom.currentStudent >= examRoom.maxStudent && examRoom.maxStudent > 0;

  return (
    <>
      <Typography
        onClick={() => setOpen(true)}
        sx={{ color: isFull ? 'error.main' : 'text.primary', cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}
      >
        {examRoom.currentStudent}/{examRoom.maxStudent}
      </Typography>
      <ExamRoomStudentListDialog open={open} onClose={() => setOpen(false)} examRoom={examRoom} />
    </>
  );
}

const nonOrderableColumnId: UniqueIdentifier[] = ['manageStudent', 'actions'];

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

function DraggableRow({ row }: { row: Row<ExamRoom> }) {
  const { transform, transition, setNodeRef, isDragging } = useSortable({ id: row.original.id });

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

export default function ExamRoomPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
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

  const [studentDialog, setStudentDialog] = useState<{ item: ExamRoom; mode: 'add' | 'remove' } | null>(null);
  const [deleteItem, setDeleteItem] = useState<ExamRoom | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  const importFileRef = useRef<HTMLInputElement>(null);
  const [importTargetId, setImportTargetId] = useState<number | null>(null);

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

  useEffect(() => {
    if (location.state?.alert) {
      setAlert(location.state.alert);
      window.history.replaceState({}, document.title);
    }
  }, [location.state]);

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

  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
  const [sorting, setSorting] = useState<SortingState>([]);
  const [columnOrder, setColumnOrder] = useState<string[]>([]);

  const columns = useMemo<ColumnDef<ExamRoom>[]>(
    () => {
      const base: ColumnDef<ExamRoom>[] = [
        {
          id: 'code',
          header: 'Mã phòng',
          accessorKey: 'code',
          cell: ({ row }) => (
            <Typography variant="body2" fontWeight="medium">
              {row.original.code}
            </Typography>
          )
        },
        { id: 'subjectName', header: 'Môn học', accessorKey: 'subjectName' },
        { id: 'roomName', header: 'Phòng', accessorKey: 'roomName' },
        { id: 'teacher1Name', header: 'GV coi thi 1', accessorKey: 'teacher1Name' },
        { id: 'teacher2Name', header: 'GV coi thi 2', accessorKey: 'teacher2Name' },
        { id: 'examDate', header: 'Ngày thi', cell: ({ row }) => formatDate(row.original.examDate) },
        {
          id: 'periods',
          header: 'Giờ thi',
          cell: ({ row }) => (
            <Box component="span" sx={{ whiteSpace: 'nowrap' }}>
              {formatPeriodRange(row.original.periods, row.original.startTime, row.original.endTime)}
            </Box>
          )
        },
        { id: 'semesterName', header: 'Học kỳ', accessorKey: 'semesterName' },
        {
          id: 'currentStudent',
          header: 'Sĩ số',
          enableSorting: false,
          meta: { className: 'cell-center' },
          cell: ({ row }) => <ExamRoomStudentCountCell examRoom={row.original} />
        },
        {
          id: 'status',
          header: 'Trạng thái',
          enableSorting: false,
          meta: { className: 'cell-center' },
          cell: ({ row }) => (
            <Chip
              label={row.original.status === 1 ? 'Hoạt động' : 'Dừng'}
              color={row.original.status === 1 ? 'success' : 'error'}
              size="small"
            />
          )
        }
      ];

      const manageStudentCol: ColumnDef<ExamRoom> = {
        id: 'manageStudent',
        header: 'Sinh viên',
        enableSorting: false,
        meta: { className: 'cell-center' },
        cell: ({ row }) => (
          <Stack direction="row" spacing={0.5} justifyContent="center">
            <Tooltip title={row.original.status === 0 ? 'Không thể thêm sinh viên' : 'Thêm sinh viên vào phòng thi'}>
              <span>
                <IconButton
                  color="primary"
                  size="small"
                  onClick={() => setStudentDialog({ item: row.original, mode: 'add' })}
                  disabled={row.original.status === 0}
                >
                  <ProfileAdd variant="Outline" />
                </IconButton>
              </span>
            </Tooltip>
            <Tooltip title={row.original.status === 0 ? 'Không thể xóa sinh viên' : 'Xóa sinh viên khỏi phòng thi'}>
              <span>
                <IconButton
                  color="error"
                  size="small"
                  onClick={() => setStudentDialog({ item: row.original, mode: 'remove' })}
                  disabled={row.original.status === 0}
                >
                  <ProfileRemove variant="Outline" />
                </IconButton>
              </span>
            </Tooltip>
          </Stack>
        )
      };

      const actionsCol: ColumnDef<ExamRoom> = {
        id: 'actions',
        header: 'Hành động',
        enableSorting: false,
        meta: { className: 'cell-center', width: '10%' },
        cell: ({ row }) => (
          <Stack direction="row" spacing={0.5} justifyContent="center">
            {isTeacher && (
              <Tooltip title="Giám sát phòng thi">
                <span>
                  <IconButton
                    color="primary"
                    size="small"
                    onClick={() => navigate(`/teacher/exam-room/${row.original.id}/tracking`)}
                    disabled={row.original.status === 0}
                  >
                    <Eye variant="Outline" />
                  </IconButton>
                </span>
              </Tooltip>
            )}
            {isAdmin && (
              <>
                <Tooltip title="Xem chi tiết">
                  <span>
                    <IconButton color="primary" size="small" onClick={() => navigate(`/exam-room/detail/${row.original.id}`)}>
                      <Eye variant="Outline" />
                    </IconButton>
                  </span>
                </Tooltip>
                <Tooltip title={row.original.status === 0 ? 'Không thể chỉnh sửa' : 'Chỉnh sửa'}>
                  <span>
                    <IconButton
                      color="primary"
                      size="small"
                      onClick={() => navigate(`/exam-room/edit/${row.original.id}`)}
                      disabled={row.original.status === 0}
                    >
                      <Edit2 variant="Outline" />
                    </IconButton>
                  </span>
                </Tooltip>
                <Tooltip title={row.original.status === 0 ? 'Không thể xóa' : 'Xóa'}>
                  <span>
                    <IconButton color="error" size="small" onClick={() => setDeleteItem(row.original)} disabled={row.original.status === 0}>
                      <Trash variant="Outline" />
                    </IconButton>
                  </span>
                </Tooltip>
                <Tooltip title={row.original.status === 0 ? 'Không thể import' : 'Import sinh viên'}>
                  <span>
                    <IconButton
                      color="primary"
                      size="small"
                      onClick={() => {
                        setImportTargetId(row.original.id);
                        importFileRef.current?.click();
                      }}
                      disabled={row.original.status === 0}
                    >
                      <ImportCurve variant="Outline" />
                    </IconButton>
                  </span>
                </Tooltip>
              </>
            )}
          </Stack>
        )
      };

      return isAdmin ? [...base, manageStudentCol, actionsCol] : [...base, actionsCol];
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [isAdmin, isTeacher, navigate]
  );

  useEffect(() => setColumnOrder(columns.map((c) => c.id!)), [columns]);

  const dataIds = useMemo<UniqueIdentifier[]>(() => data.map((r) => r.id), [data]);

  const table = useReactTable({
    data,
    columns,
    manualPagination: true,
    getRowId: (row: ExamRoom) => row.id.toString(),
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
      setData((rows) => arrayMove(rows, dataIds.indexOf(active.id), dataIds.indexOf(over.id)));
    }
  }

  const columnSensors = useSensors(useSensor(MouseSensor, {}), useSensor(TouchSensor, {}), useSensor(KeyboardSensor, {}));
  const rowSensors = useSensors(useSensor(MouseSensor, {}), useSensor(TouchSensor, {}), useSensor(KeyboardSensor, {}));

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
            <Button variant="contained" startIcon={<Add />} onClick={() => navigate('/exam-room/add')}>
              Thêm phòng thi
            </Button>
          </Stack>
        )}
      </Stack>

      <MainCard content={false}>
        <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ gap: 2, justifyContent: 'space-between', p: 2 }}>
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
              onChange={(e: SelectChangeEvent) => {
                setStatusFilter(e.target.value);
                setPageNumber(0);
              }}
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
                {tableLoading ? (
                  <TableRow>
                    <TableCell colSpan={table.getAllColumns().length} align="center" sx={{ py: 4 }}>
                      <CircularProgress size={28} />
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
                          <EmptyTable msg="Không có dữ liệu" />
                        </TableCell>
                      </TableRow>
                    )}
                  </DndContext>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </DndContext>

        <input ref={importFileRef} type="file" accept=".xlsx,.xls" style={{ display: 'none' }} onChange={handleImportStudents} />

        <ManageExamRoomStudentDialog
          open={!!studentDialog}
          onClose={() => setStudentDialog(null)}
          examRoom={studentDialog?.item ?? null}
          mode={studentDialog?.mode ?? 'add'}
          onChanged={() => setReload((p) => !p)}
        />

        <Divider />

        <Grid container sx={{ alignItems: 'center', justifyContent: 'space-between', px: 2, py: 1.5 }}>
          <Grid>
            <Stack direction="row" spacing={1} alignItems="center">
              <Typography variant="caption" color="secondary">
                Số bản ghi mỗi trang
              </Typography>
              <FormControl size="small">
                <Select
                  value={pageSize}
                  onChange={(e: SelectChangeEvent<number>) => {
                    setPageSize(Number(e.target.value));
                    setPageNumber(0);
                  }}
                  sx={{ '& .MuiSelect-select': { py: 0.75, px: 1.25 } }}
                >
                  {pageSizeOptions.map((o) => (
                    <MenuItem key={o} value={o}>
                      {o}
                    </MenuItem>
                  ))}
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

      {/* Delete confirm dialog */}
      <Dialog open={!!deleteItem} onClose={() => setDeleteItem(null)}>
        <DialogTitle>Xác nhận xóa phòng thi?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Bạn có chắc muốn xóa phòng thi <strong>{deleteItem?.code}</strong>? Tất cả dữ liệu liên quan sẽ bị xóa.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteItem(null)} disabled={deleteLoading}>
            Hủy
          </Button>
          <Button
            variant="contained"
            color="error"
            onClick={handleDelete}
            disabled={deleteLoading}
            startIcon={deleteLoading ? <CircularProgress size={14} color="inherit" /> : undefined}
          >
            Xóa
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
