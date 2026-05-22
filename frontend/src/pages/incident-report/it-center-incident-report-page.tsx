import { CSSProperties, Fragment, useEffect, useMemo, useState } from 'react';
import { useLocation } from 'react-router-dom';

import Divider from '@mui/material/Divider';
import MenuItem from '@mui/material/MenuItem';
import OutlinedInput from '@mui/material/OutlinedInput';
import Paper from '@mui/material/Paper';
import Select, { SelectChangeEvent } from '@mui/material/Select';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Tooltip from '@mui/material/Tooltip';
import Box from '@mui/material/Box';
import Pagination from '@mui/material/Pagination';
import Typography from '@mui/material/Typography';

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

import { rankItem } from '@tanstack/match-sorter-utils';
import {
  getCoreRowModel,
  getFilteredRowModel,
  getFacetedRowModel,
  getFacetedMinMaxValues,
  getFacetedUniqueValues,
  getPaginationRowModel,
  getSortedRowModel,
  getGroupedRowModel,
  getExpandedRowModel,
  flexRender,
  useReactTable,
  ColumnDef,
  ColumnFiltersState,
  SortingState,
  GroupingState,
  Row,
  FilterFn,
  Header
} from '@tanstack/react-table';

import MainCard from 'components/MainCard';
import IconButton from 'components/@extended/IconButton';
import { EmptyTable, HeaderSort, RowEditable } from 'components/third-party/react-table';

import { ArrowDown2, ArrowRight2, Command, TableDocument, TickCircle, CloseCircle } from 'iconsax-reactjs';
import {
  Alert,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormControl,
  Grid,
  Snackbar
} from '@mui/material';
import { useIntl } from 'react-intl';
import { HttpStatusCode } from 'axios';
import useAuth from 'hooks/useAuth';
import { type IncidentReport } from 'types/incident-report';
import { getListForItCenter, resolve, reject } from 'api/incident-report';
import { getList as getRoomList } from 'api/room';
import { type Room } from 'types/room';
import { DEFAULT_PAGE_SIZE } from 'types/paging';

const fuzzyFilter: FilterFn<IncidentReport> = (row, columnId, value, addMeta) => {
  const itemRank = rankItem(row.getValue(columnId), value);
  addMeta(itemRank);
  return itemRank.passed;
};

function IncidentStatusChip({ status }: { status: number }) {
  if (status === 0) return <Chip label="Chờ xử lý" color="warning" size="small" />;
  if (status === 1) return <Chip label="Đã xử lý" color="success" size="small" />;
  if (status === 2) return <Chip label="Từ chối" color="error" size="small" />;
  return <Chip label="UNKNOWN" size="small" />;
}

function ReporterRoleChip({ role }: { role: string }) {
  if (role === 'STUDENT') return <Chip label="Sinh viên" color="info" size="small" />;
  if (role === 'TEACHER') return <Chip label="Giảng viên" color="secondary" size="small" />;
  return <Chip label={role} size="small" />;
}

// ==============================|| ROW ACTIONS ||============================== //

function ConfirmDialog({
  open,
  title,
  content,
  confirmColor,
  confirmLabel,
  onConfirm,
  onCancel,
  working
}: {
  open: boolean;
  title: string;
  content: string;
  confirmColor: 'success' | 'error';
  confirmLabel: string;
  onConfirm: () => void;
  onCancel: () => void;
  working: boolean;
}) {
  return (
    <Dialog open={open} onClose={onCancel} maxWidth="xs" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <DialogContentText>{content}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel} disabled={working}>
          Hủy
        </Button>
        <Button onClick={onConfirm} color={confirmColor} variant="contained" disabled={working} autoFocus>
          {confirmLabel}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function RowActions({
  row,
  reload,
  setReload,
  setAlert
}: {
  row: Row<IncidentReport>;
  reload: boolean;
  setReload: (e: boolean) => void;
  setAlert: React.Dispatch<React.SetStateAction<{ open: boolean; message: string; severity: 'success' | 'error' | 'info' | 'warning' }>>;
}) {
  const { logout } = useAuth();
  const [working, setWorking] = useState(false);
  const [confirmDialog, setConfirmDialog] = useState<'resolve' | 'reject' | null>(null);
  const { status, id } = row.original;

  const handleResolve = async () => {
    setWorking(true);
    const response = await resolve(id);
    if (response.statusCode == HttpStatusCode.Ok) {
      setAlert({ open: true, message: 'Đã xác nhận xử lý sự cố', severity: 'success' });
      setReload(!reload);
    } else if (response.statusCode == HttpStatusCode.Unauthorized) {
      logout();
    } else {
      setAlert({ open: true, message: response.data || 'Lỗi không xác định', severity: 'error' });
    }
    setWorking(false);
    setConfirmDialog(null);
  };

  const handleReject = async () => {
    setWorking(true);
    const response = await reject(id);
    if (response.statusCode == HttpStatusCode.Ok) {
      setAlert({ open: true, message: 'Đã từ chối xử lý sự cố', severity: 'success' });
      setReload(!reload);
    } else if (response.statusCode == HttpStatusCode.Unauthorized) {
      logout();
    } else {
      setAlert({ open: true, message: response.data || 'Lỗi không xác định', severity: 'error' });
    }
    setWorking(false);
    setConfirmDialog(null);
  };

  if (status !== 0) return null;

  return (
    <>
      <Stack direction="row" sx={{ gap: 1, alignItems: 'center' }}>
        <Tooltip title="Xác nhận xử lý">
          <IconButton color="success" onClick={() => setConfirmDialog('resolve')} disabled={working}>
            <TickCircle variant="Outline" />
          </IconButton>
        </Tooltip>
        <Tooltip title="Từ chối xử lý">
          <IconButton color="error" onClick={() => setConfirmDialog('reject')} disabled={working}>
            <CloseCircle variant="Outline" />
          </IconButton>
        </Tooltip>
      </Stack>

      <ConfirmDialog
        open={confirmDialog === 'resolve'}
        title="Xác nhận xử lý sự cố"
        content="Bạn có chắc chắn muốn xác nhận đã xử lý sự cố này không?"
        confirmColor="success"
        confirmLabel="Xác nhận"
        onConfirm={handleResolve}
        onCancel={() => setConfirmDialog(null)}
        working={working}
      />
      <ConfirmDialog
        open={confirmDialog === 'reject'}
        title="Từ chối xử lý sự cố"
        content="Bạn có chắc chắn muốn từ chối xử lý sự cố này không?"
        confirmColor="error"
        confirmLabel="Từ chối"
        onConfirm={handleReject}
        onCancel={() => setConfirmDialog(null)}
        working={working}
      />
    </>
  );
}

const nonOrderableColumnId: UniqueIdentifier[] = ['drag-handle', 'expander', 'select'];

// ==============================|| REACT TABLE - DRAGGABLE HEADER ||============================== //

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
    <TableCell colSpan={header.colSpan} ref={setNodeRef} style={style}>
      {header.isPlaceholder ? null : (
        <Stack direction="row" sx={{ gap: 1, alignItems: 'center', justifyContent: 'space-between' }}>
          {header.column.getCanGroup() && (
            <IconButton
              color={header.column.getIsGrouped() ? 'error' : 'primary'}
              onClick={header.column.getToggleGroupingHandler()}
              size="small"
              sx={{ p: 0, width: 24, height: 24, fontSize: '1rem', mr: 0.75 }}
            >
              {header.column.getIsGrouped() ? (
                <Command size="32" color="#FF8A65" variant="Bold" />
              ) : (
                <TableDocument size="32" variant="Outline" />
              )}
            </IconButton>
          )}
          <Box {...(!nonOrderableColumnId.includes(header.id) && { ...attributes, ...listeners, sx: { cursor: 'move' } })}>
            {flexRender(header.column.columnDef.header, header.getContext())}
          </Box>
          {header.column.getCanSort() && <HeaderSort column={header.column} sort />}
        </Stack>
      )}
    </TableCell>
  );
}

// ==============================|| REACT TABLE - DRAGGABLE ROW ||============================== //

function DraggableRow({ row }: { row: Row<any> }) {
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
      {row.getVisibleCells().map((cell) => {
        let bgcolor = 'background.paper';
        if (cell.getIsGrouped()) bgcolor = 'primary.lighter';
        if (cell.getIsAggregated()) bgcolor = 'warning.lighter';
        if (cell.getIsPlaceholder()) bgcolor = 'error.lighter';

        if (cell.column.columnDef.meta !== undefined && cell.column.getCanSort()) {
          Object.assign(cell.column.columnDef.meta, { style: { background: bgcolor } });
        }

        return (
          <TableCell
            key={cell.id}
            {...cell.column.columnDef.meta}
            {...(cell.getIsGrouped() && cell.column.columnDef.meta === undefined && { style: { background: bgcolor } })}
            sx={{
              width:
                cell.column.columnDef.meta && 'width' in cell.column.columnDef.meta
                  ? (cell.column.columnDef.meta as { width?: string | number }).width
                  : undefined,
              justifyContent: cell.column.id === 'edit' ? 'center' : 'left',
              verticalAlign: 'middle'
            }}
          >
            {cell.getIsGrouped() ? (
              <Stack direction="row" sx={{ gap: 0.5, alignItems: 'center' }}>
                <IconButton color="secondary" onClick={row.getToggleExpandedHandler()} size="small" sx={{ p: 0, width: 24, height: 24 }}>
                  {row.getIsExpanded() ? <ArrowDown2 size="32" variant="Outline" /> : <ArrowRight2 size="32" variant="Outline" />}
                </IconButton>
                <Box>{flexRender(cell.column.columnDef.cell, cell.getContext())}</Box> <Box>({row.subRows.length})</Box>
              </Stack>
            ) : cell.getIsAggregated() ? (
              flexRender(cell.column.columnDef.aggregatedCell ?? cell.column.columnDef.cell, cell.getContext())
            ) : cell.getIsPlaceholder() ? null : (
              flexRender(cell.column.columnDef.cell, cell.getContext())
            )}
          </TableCell>
        );
      })}
    </TableRow>
  );
}

function formatDate(isoString: string): string {
  if (!isoString) return '-';
  try {
    const d = new Date(isoString);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${pad(d.getHours())}:${pad(d.getMinutes())} ${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
  } catch {
    return isoString;
  }
}

// ==============================|| REACT TABLE - MAIN ||============================== //

export default function ItCenterIncidentReportPage() {
  const { logout } = useAuth();
  const intl = useIntl();
  const [reload, setReload] = useState(false);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [roomFilter, setRoomFilter] = useState('');

  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  useEffect(() => {
    getRoomList({ page: 0, size: 1000, keyword: '', status: '1', sort: '' }).then((res) => {
      if (res.statusCode === HttpStatusCode.Ok) setRooms(res.data.content);
    });
  }, []);

  const columns = useMemo<ColumnDef<IncidentReport>[]>(
    () => [
      {
        id: 'id',
        header: '#',
        accessorKey: 'id',
        dataType: 'text',
        enableColumnFilter: false,
        enableGrouping: false,
        meta: { className: 'cell-center' }
      },
      {
        id: 'title',
        header: 'Tiêu đề sự cố',
        accessorKey: 'title',
        dataType: 'text',
        enableGrouping: false
      },
      {
        id: 'roomName',
        header: 'Phòng học',
        accessorKey: 'roomName',
        cell: (cell) => cell.row.original.roomName || '-',
        dataType: 'text',
        enableGrouping: false
      },
      {
        id: 'reporter',
        header: 'Người báo cáo',
        accessorKey: 'reporterName',
        cell: (cell) => cell.row.original.reporterName,
        dataType: 'text',
        enableGrouping: false
      },
      {
        id: 'reporterRole',
        header: 'Vai trò',
        accessorKey: 'reporterRole',
        cell: (cell) => <ReporterRoleChip role={cell.row.original.reporterRole} />,
        dataType: 'text',
        enableGrouping: false
      },
      {
        id: 'status',
        header: 'Trạng thái',
        accessorKey: 'status',
        cell: (cell) => <IncidentStatusChip status={cell.row.original.status} />,
        dataType: 'select',
        enableGrouping: false
      },
      {
        id: 'handlerName',
        header: 'Người xử lý',
        accessorKey: 'handlerName',
        cell: (cell) => cell.row.original.handlerName || '-',
        dataType: 'text',
        enableGrouping: false
      },
      {
        id: 'createdAt',
        header: 'Thời gian',
        accessorKey: 'createdAt',
        cell: (cell) => formatDate(cell.row.original.createdAt),
        dataType: 'text',
        enableGrouping: false
      },
      {
        id: 'edit',
        header: 'Hành động',
        cell: ({ row }) => <RowActions row={row} reload={reload} setReload={setReload} setAlert={setAlert} />,
        enableGrouping: false,
        meta: { className: 'cell-center', width: '12%' }
      }
    ],
    [reload]
  );

  const options: number[] = [10, 25, 50, 100];
  const [data, setData] = useState<IncidentReport[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [pageNumber, setPageNumber] = useState(0);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [globalFilter, setGlobalFilter] = useState('');

  const location = useLocation();
  const [columnOrder, setColumnOrder] = useState<string[]>(() => [
    'id',
    'title',
    'roomName',
    'reporter',
    'reporterRole',
    'status',
    'handlerName',
    'createdAt',
    'edit'
  ]);
  const dataIds = useMemo<UniqueIdentifier[]>(() => data?.map(({ id }: any) => id), [data]);

  const [rowSelection, setRowSelection] = useState({});
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
  const [sorting, setSorting] = useState<SortingState>([]);
  const [grouping, setGrouping] = useState<GroupingState>([]);
  const [columnVisibility, setColumnVisibility] = useState({});
  const [originalData, setOriginalData] = useState(() => [...data]);
  const [selectedRow, setSelectedRow] = useState({});

  const handleChangePageSize = (event: SelectChangeEvent<number>) => {
    setPageSize(event.target.value as number);
    setPage(0);
  };

  useEffect(() => {
    const fetchData = async () => {
      const params: any = { page, size: pageSize, keyword };
      if (statusFilter !== '') params.status = Number(statusFilter);
      if (roomFilter !== '') params.roomId = Number(roomFilter);

      const response = await getListForItCenter(params);
      if (response.statusCode === HttpStatusCode.Ok) {
        setData(response.data.content);
        setTotalPages(response.data.totalPages);
        setTotalElements(response.data.totalElements);
        setPageNumber(response.data.pageable.pageNumber);
      } else if (response.statusCode === HttpStatusCode.Unauthorized) {
        logout();
      } else {
        setAlert({ open: true, message: intl.formatMessage({ id: 'unknown-error' }), severity: 'error' });
      }
    };

    fetchData();
  }, [page, pageSize, keyword, statusFilter, roomFilter, reload, intl, logout]);

  useEffect(() => {
    if (location.state?.alert) {
      setAlert(location.state.alert);
      window.history.replaceState({}, document.title);
    }
  }, [location.state]);

  const table = useReactTable({
    data,
    columns,
    manualPagination: true,
    defaultColumn: { cell: RowEditable },
    getRowId: (row: IncidentReport) => (row.id ?? '').toString(),
    state: { rowSelection, columnFilters, sorting, grouping, columnOrder, columnVisibility },
    enableRowSelection: true,
    onRowSelectionChange: setRowSelection,
    onSortingChange: setSorting,
    onGroupingChange: setGrouping,
    onColumnFiltersChange: setColumnFilters,
    onGlobalFilterChange: setGlobalFilter,
    onColumnOrderChange: setColumnOrder,
    onColumnVisibilityChange: setColumnVisibility,
    getRowCanExpand: () => true,
    getExpandedRowModel: getExpandedRowModel(),
    getGroupedRowModel: getGroupedRowModel(),
    getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getFacetedRowModel: getFacetedRowModel(),
    getFacetedUniqueValues: getFacetedUniqueValues(),
    getFacetedMinMaxValues: getFacetedMinMaxValues(),
    globalFilterFn: fuzzyFilter,
    debugTable: true,
    debugHeaders: true,
    debugColumns: true,
    meta: {
      selectedRow,
      setSelectedRow,
      revertData: (rowIndex: number, revert: unknown) => {
        if (revert) {
          setData((old) => old.map((row, index) => (index === rowIndex ? originalData[rowIndex] : row)));
        } else {
          setOriginalData((old) => old.map((row, index) => (index === rowIndex ? data[rowIndex] : row)));
        }
      },
      updateData: (rowIndex: number, columnId: string, value: unknown) => {
        setData((old) =>
          old.map((row, index) => {
            if (index === rowIndex) return { ...old[rowIndex]!, [columnId]: value };
            return row;
          })
        );
      }
    }
  });

  function handleColumnDragEnd(event: DragEndEvent) {
    const { active, over } = event;
    if (active && over && active.id !== over.id) {
      if (nonOrderableColumnId.includes(over.id)) return;
      setColumnOrder((columnOrder) => {
        const oldIndex = columnOrder.indexOf(active.id as string);
        const newIndex = columnOrder.indexOf(over.id as string);
        return arrayMove(columnOrder, oldIndex, newIndex);
      });
    }
  }

  function handleRowDragEnd(event: DragEndEvent) {
    const { active, over } = event;
    if (active && over && active.id !== over.id) {
      setData((data: any) => {
        const oldIndex = dataIds.indexOf(active.id);
        const newIndex = dataIds.indexOf(over.id);
        return arrayMove(data, oldIndex, newIndex);
      });
    }
  }

  const columnSensors = useSensors(useSensor(MouseSensor, {}), useSensor(TouchSensor, {}), useSensor(KeyboardSensor, {}));
  const rowSensors = useSensors(useSensor(MouseSensor, {}), useSensor(TouchSensor, {}), useSensor(KeyboardSensor, {}));

  useEffect(() => setColumnVisibility({ id: false }), []);

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
          Báo cáo sự cố
        </Typography>
      </Stack>

      <MainCard content={false}>
        <Snackbar
          open={alert?.open}
          autoHideDuration={3000}
          onClose={() => setAlert({ ...alert, open: false })}
          anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
        >
          <Alert
            severity={alert?.severity || 'info'}
            variant="filled"
            sx={{ width: '100%', borderRadius: 2, fontSize: 15, textAlign: 'center', py: 1.5, px: 2 }}
          >
            {alert?.message || 'Không có thông báo'}
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
            <OutlinedInput
              value={globalFilter ?? ''}
              onChange={(e) => setGlobalFilter(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  setKeyword(globalFilter);
                  setPage(0);
                }
              }}
              placeholder="Tìm kiếm tiêu đề"
              sx={{ minWidth: 180 }}
              inputProps={{
                sx: {
                  textOverflow: 'ellipsis',
                  '&::placeholder': { textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }
                }
              }}
            />

            <Select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              displayEmpty
              input={<OutlinedInput />}
              slotProps={{ input: { 'aria-label': 'Status Filter' } }}
              sx={{ minWidth: 150 }}
            >
              <MenuItem value="">Trạng thái</MenuItem>
              <MenuItem value="0">Chờ xử lý</MenuItem>
              <MenuItem value="1">Đã xử lý</MenuItem>
              <MenuItem value="2">Từ chối</MenuItem>
            </Select>

            <Select
              value={roomFilter}
              onChange={(e) => {
                setRoomFilter(e.target.value);
                setPage(0);
              }}
              displayEmpty
              input={<OutlinedInput />}
              slotProps={{ input: { 'aria-label': 'Room Filter' } }}
              sx={{ minWidth: 150 }}
            >
              <MenuItem value="">Phòng học</MenuItem>
              {rooms.map((r) => (
                <MenuItem key={r.id} value={r.id.toString()}>
                  {r.name}
                </MenuItem>
              ))}
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
                <Select
                  value={pageSize}
                  onChange={handleChangePageSize}
                  size="small"
                  sx={{ '& .MuiSelect-select': { py: 0.75, px: 1.25 } }}
                >
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
              page={pageNumber + 1}
              onChange={(_, value) => setPage(value - 1)}
              color="primary"
              showFirstButton
              showLastButton
            />
          </Grid>
        </Grid>
      </MainCard>
    </Stack>
  );
}
