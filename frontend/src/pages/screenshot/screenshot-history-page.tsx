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
  Autocomplete,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  Grid,
  IconButton,
  MenuItem,
  OutlinedInput,
  Pagination,
  Paper,
  Select,
  SelectChangeEvent,
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
import { CloseCircle, Eye } from 'iconsax-reactjs';

import MainCard from 'components/MainCard';
import { EmptyTable, HeaderSort } from 'components/third-party/react-table';
import useAuth from 'hooks/useAuth';
import {
  getScreenshotContexts,
  getScreenshotHistory,
  getScreenshotStudents,
  ScreenshotContextOption,
  ScreenshotContextType,
  ScreenshotHistoryItem,
  ScreenshotStudentOption
} from 'api/screenshot';

function formatDateTime(value: string) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  });
}

const inputSx = {
  '& .MuiInputBase-root': { height: 40 },
  '& .MuiOutlinedInput-root': { height: 40 }
};

const nonOrderableColumnId: UniqueIdentifier[] = ['view'];

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

function DraggableRow({ row }: { row: Row<ScreenshotHistoryItem> }) {
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

export default function ScreenshotHistoryPage() {
  const { logout } = useAuth();

  const [contextType, setContextType] = useState<ScreenshotContextType>('CLASS');
  const [contexts, setContexts] = useState<ScreenshotContextOption[]>([]);
  const [selectedContext, setSelectedContext] = useState<ScreenshotContextOption | null>(null);
  const [contextInput, setContextInput] = useState('');
  const [loadingContexts, setLoadingContexts] = useState(false);

  const [students, setStudents] = useState<ScreenshotStudentOption[]>([]);
  const [selectedStudent, setSelectedStudent] = useState<ScreenshotStudentOption | null>(null);
  const [studentInput, setStudentInput] = useState('');
  const [loadingStudents, setLoadingStudents] = useState(false);

  const [captureDate, setCaptureDate] = useState('');
  const [rows, setRows] = useState<ScreenshotHistoryItem[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [selectedImage, setSelectedImage] = useState<ScreenshotHistoryItem | null>(null);

  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
  const [sorting, setSorting] = useState<SortingState>([]);
  const [columnOrder, setColumnOrder] = useState<string[]>([]);

  useEffect(() => {
    setSelectedContext(null);
    setContextInput('');
    setSelectedStudent(null);
    setStudentInput('');
    setStudents([]);
    setPage(0);

    setLoadingContexts(true);
    getScreenshotContexts(contextType)
      .then((res) => {
        if (res.statusCode === HttpStatusCode.Ok) setContexts(res.data ?? []);
        else if (res.statusCode === HttpStatusCode.Unauthorized) logout();
      })
      .finally(() => setLoadingContexts(false));
  }, [contextType, logout]);

  useEffect(() => {
    setSelectedStudent(null);
    setStudentInput('');
    setStudents([]);
    setPage(0);
    if (!selectedContext) return;

    setLoadingStudents(true);
    getScreenshotStudents(contextType, selectedContext.id)
      .then((res) => {
        if (res.statusCode === HttpStatusCode.Ok) setStudents(res.data ?? []);
        else if (res.statusCode === HttpStatusCode.Unauthorized) logout();
      })
      .finally(() => setLoadingStudents(false));
  }, [contextType, selectedContext, logout]);

  const loadHistory = useCallback(() => {
    setLoadingHistory(true);
    getScreenshotHistory({
      contextType,
      contextId: selectedContext?.id,
      studentId: selectedStudent?.studentId,
      date: captureDate || undefined,
      page,
      size
    })
      .then((res) => {
        if (res.statusCode === HttpStatusCode.Ok) {
          setRows(res.data?.content ?? []);
          setTotalElements(res.data?.totalElements ?? 0);
          setTotalPages(res.data?.totalPages ?? 0);
        } else if (res.statusCode === HttpStatusCode.Unauthorized) {
          logout();
        }
      })
      .finally(() => setLoadingHistory(false));
  }, [contextType, selectedContext, selectedStudent, captureDate, page, size, logout]);

  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  const columns = useMemo<ColumnDef<ScreenshotHistoryItem>[]>(
    () => [
      {
        id: 'createdAt',
        header: 'Thời gian chụp ảnh',
        cell: ({ row }) => formatDateTime(row.original.createdAt),
        enableGrouping: false
      },
      {
        id: 'student',
        header: 'Sinh viên',
        cell: ({ row }) => `${row.original.studentName} - ${row.original.studentCode}`,
        enableGrouping: false
      },
      {
        id: 'contextName',
        header: contextType === 'CLASS' ? 'Lớp học phần' : 'Phòng thi',
        accessorKey: 'contextName',
        enableGrouping: false
      },
      {
        id: 'applicationName',
        header: 'Ứng dụng đang sử dụng',
        cell: ({ row }) => (
          <Box component="span" sx={{ color: row.original.applicationName ? 'text.primary' : 'text.disabled' }}>
            {row.original.applicationName || 'Chưa có dữ liệu'}
          </Box>
        ),
        enableGrouping: false
      },
      {
        id: 'view',
        header: 'Hành động',
        cell: ({ row }) => (
          <Tooltip title="Xem ảnh">
            <IconButton color="primary" size="small" onClick={() => setSelectedImage(row.original)}>
              <Eye size={18} variant="Outline" />
            </IconButton>
          </Tooltip>
        ),
        enableSorting: false,
        enableGrouping: false,
        meta: { className: 'cell-center', width: '10%' }
      }
    ],
    [contextType]
  );

  useEffect(() => setColumnOrder(columns.map((column) => column.id!)), [columns]);

  const dataIds = useMemo<UniqueIdentifier[]>(() => rows.map((row) => row.id), [rows]);

  const table = useReactTable({
    data: rows,
    columns,
    manualPagination: true,
    getRowId: (row: ScreenshotHistoryItem) => row.id.toString(),
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
      setRows((data) => arrayMove(data, dataIds.indexOf(active.id), dataIds.indexOf(over.id)));
    }
  }

  const columnSensors = useSensors(useSensor(MouseSensor, {}), useSensor(TouchSensor, {}), useSensor(KeyboardSensor, {}));
  const rowSensors = useSensors(useSensor(MouseSensor, {}), useSensor(TouchSensor, {}), useSensor(KeyboardSensor, {}));
  const options = [10, 25, 50, 100];

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
          Lịch sử ảnh màn hình
        </Typography>
      </Stack>

      <MainCard content={false}>
        <Stack
          spacing={2}
          sx={(theme) => ({
            p: 2,
            [theme.breakpoints.down('sm')]: { '& .MuiOutlinedInput-root, & .MuiFormControl-root': { width: 1 } }
          })}
        >
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ flexGrow: 1, flexWrap: 'wrap' }}>
            <Select
              size="small"
              value={contextType}
              onChange={(event) => setContextType(event.target.value as ScreenshotContextType)}
              displayEmpty
              input={<OutlinedInput />}
              sx={{ minWidth: 180, height: 40 }}
            >
              <MenuItem value="CLASS">Lớp học phần</MenuItem>
              <MenuItem value="EXAM_ROOM">Phòng thi</MenuItem>
            </Select>

            <Autocomplete
              size="small"
              sx={{ minWidth: 300, ...inputSx }}
              options={contexts}
              value={selectedContext}
              inputValue={contextInput}
              onChange={(_, value) => {
                setSelectedContext(value);
                setPage(0);
              }}
              onInputChange={(_, value) => setContextInput(value)}
              loading={loadingContexts}
              getOptionLabel={(option) => option.label}
              isOptionEqualToValue={(option, value) => option.id === value.id}
              renderInput={(params) => (
                <TextField
                  {...params}
                  placeholder={contextType === 'CLASS' ? 'Tìm lớp học phần...' : 'Tìm phòng thi...'}
                  slotProps={{
                    input: {
                      ...params.InputProps,
                      endAdornment: (
                        <>
                          {loadingContexts && <CircularProgress size={16} />}
                          {params.InputProps.endAdornment}
                        </>
                      )
                    }
                  }}
                />
              )}
            />

            <Autocomplete
              size="small"
              sx={{ minWidth: 320, ...inputSx }}
              options={students}
              value={selectedStudent}
              inputValue={studentInput}
              disabled={!selectedContext}
              onChange={(_, value) => {
                setSelectedStudent(value);
                setPage(0);
              }}
              onInputChange={(_, value) => setStudentInput(value)}
              loading={loadingStudents}
              getOptionLabel={(option) => `${option.fullName} - ${option.code}`}
              isOptionEqualToValue={(option, value) => option.studentId === value.studentId}
              renderInput={(params) => (
                <TextField
                  {...params}
                  placeholder="Tìm sinh viên..."
                  slotProps={{
                    input: {
                      ...params.InputProps,
                      endAdornment: (
                        <>
                          {loadingStudents && <CircularProgress size={16} />}
                          {params.InputProps.endAdornment}
                        </>
                      )
                    }
                  }}
                />
              )}
            />
          </Stack>

          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            spacing={2}
            sx={{ alignItems: { xs: 'stretch', sm: 'center' }, justifyContent: 'space-between' }}
          >
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ alignItems: { xs: 'stretch', sm: 'center' } }}>
              <TextField
                size="small"
                type="date"
                value={captureDate}
                onChange={(event) => {
                  setCaptureDate(event.target.value);
                  setPage(0);
                }}
                sx={{ minWidth: 200, ...inputSx }}
                slotProps={{ inputLabel: { shrink: true } }}
              />

              <Button
                variant="outlined"
                sx={{ height: 40, minWidth: 110 }}
                onClick={() => {
                  setSelectedContext(null);
                  setContextInput('');
                  setSelectedStudent(null);
                  setStudentInput('');
                  setCaptureDate('');
                  setPage(0);
                }}
              >
                Xóa lọc
              </Button>
            </Stack>

            <Typography variant="caption" color="secondary" sx={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', minHeight: 40 }}>
              Tổng cộng: {totalElements} bản ghi
            </Typography>
          </Stack>
        </Stack>

        <DndContext collisionDetection={closestCenter} modifiers={[restrictToHorizontalAxis]} onDragEnd={handleColumnDragEnd} sensors={columnSensors}>
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
                {loadingHistory ? (
                  <TableRow>
                    <TableCell colSpan={table.getAllColumns().length} align="center" sx={{ py: 4 }}>
                      <CircularProgress size={24} />
                    </TableCell>
                  </TableRow>
                ) : (
                  <DndContext collisionDetection={closestCenter} modifiers={[restrictToVerticalAxis]} onDragEnd={handleRowDragEnd} sensors={rowSensors}>
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
                          <EmptyTable msg="Không có ảnh màn hình" />
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

      <Dialog open={!!selectedImage} onClose={() => setSelectedImage(null)} maxWidth="lg" slotProps={{ paper: { sx: { borderRadius: 2 } } }}>
        <DialogTitle sx={{ py: 1.5 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Typography variant="h6">
              Màn hình — {selectedImage?.studentName} — {selectedImage?.studentCode}
            </Typography>
            <IconButton onClick={() => setSelectedImage(null)} size="small" sx={{ color: 'text.secondary' }}>
              <CloseCircle size={20} />
            </IconButton>
          </Stack>
        </DialogTitle>
        <DialogContent sx={{ p: 1.5, pt: 0 }}>
          {selectedImage?.imageUrl && (
            <Box
              component="img"
              src={selectedImage.imageUrl}
              alt={`Screenshot — ${selectedImage.studentName}`}
              sx={{ width: '100%', display: 'block', borderRadius: 1 }}
            />
          )}
        </DialogContent>
      </Dialog>
    </Stack>
  );
}
