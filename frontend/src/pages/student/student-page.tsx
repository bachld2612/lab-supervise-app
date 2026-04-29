import { ChangeEvent, CSSProperties, Fragment, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

// material-ui
import { Autocomplete, CircularProgress, TextField } from '@mui/material';
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

// third-party
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

// project-imports
import MainCard from 'components/MainCard';
import IconButton from 'components/@extended/IconButton';

import { EmptyTable, HeaderSort, RowEditable } from 'components/third-party/react-table';

// assets
import { Add, ArrowDown2, ArrowRight2, Command, Edit2, ExportCurve, Eye, ImportCurve, Lock, TableDocument, Trash } from 'iconsax-reactjs';
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
import { type Student } from 'types/student';
import { getList, deleteById, resetPassword, downloadStudentImportTemplate, importStudent } from 'api/student';
import { getList as getManageClasses } from 'api/manageClass';
import { ManageClass } from 'types/manageClass';
import { DEFAULT_PAGE_SIZE, PageRequest } from 'types/paging';

const fuzzyFilter: FilterFn<Student> = (row, columnId, value, addMeta) => {
  // rank the item
  const itemRank = rankItem(row.getValue(columnId), value);

  // store the ranking info
  addMeta(itemRank);

  // return if the item should be filtered in/out
  return itemRank.passed;
};

// ==============================|| REACT TABLE - EDIT ACTION ||============================== //

function EditAction({
  row,
  reload,
  setReload,
  setAlert
}: {
  row: Row<Student>;
  reload: boolean;
  setReload: (e: boolean) => void;
  setAlert: React.Dispatch<
    React.SetStateAction<{
      open: boolean;
      message: string;
      severity: 'success' | 'error' | 'info' | 'warning';
    }>
  >;
}) {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [hasDetailPermission, setHasDetailPermission] = useState(false);
  const [hasEditPermission, setHasEditPermission] = useState(false);
  const [hasDeletePermission, setHasDeletePermission] = useState(false);
  const [hasResetPasswordPermission, setHasResetPasswordPermission] = useState(false);
  const [open, setOpen] = useState(false);
  const [openDelete, setOpenDelete] = useState(false);

  useEffect(() => {
    if ([1].includes(user?.roleId ?? 0)) {
      setHasDetailPermission(true);
      setHasEditPermission(true);
      setHasDeletePermission(true);
      setHasResetPasswordPermission(true);
    }
  }, [user?.roleId]);

  const handleDelete = async () => {
    const response = await deleteById(row.original.id);

    if (response.statusCode == HttpStatusCode.Ok) {
      setAlert({ open: true, message: 'Xóa sinh viên thành công', severity: 'success' });
      setReload(!reload);
    } else if (response.statusCode == HttpStatusCode.Unauthorized) {
      logout();
    } else if (response.statusCode == HttpStatusCode.UnprocessableEntity) {
      setAlert({ open: true, message: response.data, severity: 'error' });
    } else {
      setAlert({ open: true, message: 'Lỗi không xác định', severity: 'error' });
    }

    setOpenDelete(false);
  };

  const handleResetPassword = async () => {
    const response = await resetPassword(Number(row.original.id));

    if (response.statusCode == HttpStatusCode.Ok) {
      setAlert({ open: true, message: 'Đặt lại mật khẩu thành công', severity: 'success' });
      setReload(!reload);
    } else if (response.statusCode == HttpStatusCode.Unauthorized) {
      logout();
    } else if (response.statusCode == HttpStatusCode.UnprocessableEntity) {
      setAlert({ open: true, message: response.data, severity: 'error' });
    } else {
      setAlert({ open: true, message: 'Lỗi không xác định', severity: 'error' });
    }

    setOpen(false);
  };

  return (
    <Stack direction="row" sx={{ gap: 1, alignItems: 'center' }}>
      {hasDetailPermission && (
        <Tooltip title={row.original.status === 0 ? 'Không thể xem chi tiết khi sinh viên dừng hoạt động' : 'Xem chi tiết'}>
          <span>
            <IconButton color="primary" onClick={() => navigate(`/student/detail/${row.original.id}`)} disabled={row.original.status == 0}>
              <Eye variant="Outline" />
            </IconButton>
          </span>
        </Tooltip>
      )}

      {hasEditPermission && (
        <Tooltip title={row.original.status === 0 ? 'Không thể chỉnh sửa khi sinh viên dừng hoạt động' : 'Chỉnh sửa'}>
          <span>
            <IconButton color="primary" onClick={() => navigate(`/student/edit/${row.original.id}`)} disabled={row.original.status == 0}>
              <Edit2 variant="Outline" />
            </IconButton>
          </span>
        </Tooltip>
      )}

      {hasDeletePermission && (
        <Tooltip title={row.original.status === 0 ? 'Không thể xóa khi sinh viên dừng hoạt động' : 'Xóa'}>
          <span>
            <IconButton disabled={row.original.status == 0} color="primary" onClick={() => setOpenDelete(true)}>
              <Trash variant="Outline" />
            </IconButton>
          </span>
        </Tooltip>
      )}

      {hasResetPasswordPermission && (
        <Tooltip title={row.original.status === 0 ? 'Không thể đặt lại mật khẩu khi sinh viên dừng hoạt động' : 'Đặt lại mật khẩu'}>
          <span>
            <IconButton color="primary" onClick={() => setOpen(true)} disabled={row.original.status == 0}>
              <Lock variant="Outline" />
            </IconButton>
          </span>
        </Tooltip>
      )}

      <Dialog
        open={openDelete}
        onClose={() => setOpenDelete(false)}
        aria-labelledby="alert-dialog-title"
        aria-describedby="alert-dialog-description"
      >
        <DialogTitle id="alert-dialog-title">Bạn có muốn xóa sinh viên này không?</DialogTitle>

        <DialogContent>
          <DialogContentText id="alert-dialog-description">Khi xóa sinh viên, tất cả thông tin đi kèm cũng sẽ bị xóa.</DialogContentText>
        </DialogContent>

        <DialogActions>
          <Button variant="contained" color="primary" onClick={() => setOpenDelete(false)}>
            Huỷ
          </Button>

          <Button variant="contained" color="error" onClick={() => handleDelete()} autoFocus>
            Xác nhận
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={open} onClose={() => setOpen(false)} aria-labelledby="alert-dialog-title" aria-describedby="alert-dialog-description">
        <DialogTitle id="alert-dialog-title">Bạn có muốn đặt lại mật khẩu không?</DialogTitle>

        <DialogContent>
          <DialogContentText id="alert-dialog-description">
            Hành động sẽ gây ra thay đổi dữ liệu, bạn có chắc chắn muốn đặt lại mật khẩu không?
          </DialogContentText>
        </DialogContent>

        <DialogActions>
          <Button variant="contained" color="primary" onClick={() => setOpen(false)}>
            Huỷ
          </Button>

          <Button variant="contained" color="error" onClick={() => handleResetPassword()} autoFocus>
            Xác nhận
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

const nonOrderableColumnId: UniqueIdentifier[] = ['drag-handle', 'expander', 'select'];

// ==============================|| REACT TABLE - DRAGGABLE HEADER ||============================== //

function DraggableTableHeader({ header }: { header: Header<any, unknown> }) {
  const { attributes, isDragging, listeners, setNodeRef, transform } = useSortable({
    id: header.column.id
  });

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

// ==============================|| STUDENT - MAIN ||============================== //

export default function StudentPage() {
  const { logout, user } = useAuth();
  const intl = useIntl();
  const [reload, setReload] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const columns = useMemo<ColumnDef<Student>[]>(
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
        id: 'code',
        header: 'Mã sinh viên',
        accessorKey: 'code',
        dataType: 'text',
        enableGrouping: false
      },
      {
        id: 'fullName',
        header: 'Họ tên',
        accessorKey: 'fullName',
        dataType: 'text',
        enableGrouping: false
      },
      {
        id: 'phone',
        header: 'Số điện thoại',
        accessorKey: 'phone',
        dataType: 'text',
        enableGrouping: false
      },
      {
        id: 'email',
        header: 'Email',
        accessorKey: 'email',
        dataType: 'text',
        enableGrouping: false
      },
      {
        id: 'manageClassName',
        header: 'Lớp quản lý',
        accessorKey: 'manageClassName',
        dataType: 'text',
        enableGrouping: false
      },
      {
        id: 'status',
        header: 'Trạng thái',
        accessorKey: 'status',
        cell: (cell) => {
          const { status } = cell.row.original;

          return (
            <Chip
              label={status === 1 ? 'Hoạt động' : status === 0 ? 'Dừng hoạt động' : 'UNKNOWN'}
              color={status === 1 ? 'success' : 'error'}
            />
          );
        },
        dataType: 'select',
        enableGrouping: false
      },
      {
        id: 'edit',
        header: 'Hành động',
        cell: ({ row }) => <EditAction row={row} reload={reload} setReload={setReload} setAlert={setAlert} />,
        enableGrouping: false,
        meta: { className: 'cell-center', width: '10%' }
      }
    ],
    [reload]
  );
  let options: number[] = [10, 25, 50, 100];
  const [data, setData] = useState<Student[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [pageNumber, setPageNumber] = useState(0);
  const [classes, setClasses] = useState<ManageClass[]>([]);
  const [loadingClasses, setLoadingClasses] = useState(false);
  const [pageRequest, setPageRequest] = useState<PageRequest>({
    page: 0,
    size: DEFAULT_PAGE_SIZE,
    sort: '',
    keyword: '',
    status: '',
    manageClassId: ''
  });
  const [hasAddPermission, setHasAddPermission] = useState(false);
  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });
  const navigate = useNavigate();
  const location = useLocation();
  const [columnOrder, setColumnOrder] = useState<string[]>(() => columns.map((c) => c.id!));

  const dataIds = useMemo<UniqueIdentifier[]>(() => data?.map(({ id }: any) => id), [data]);

  const [rowSelection, setRowSelection] = useState({});
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
  const [globalFilter, setGlobalFilter] = useState('');
  const [sorting, setSorting] = useState<SortingState>([]);
  const [grouping, setGrouping] = useState<GroupingState>([]);

  const [columnVisibility, setColumnVisibility] = useState({});

  const [originalData, setOriginalData] = useState(() => [...data]);
  const [selectedRow, setSelectedRow] = useState({});

  const handleChangePageSize = (event: SelectChangeEvent<number>) => {
    setPageRequest((prev) => ({ ...prev, size: event.target.value, page: 0 }));
  };

  useEffect(() => {
    const fetchStudents = async () => {
      const response = await getList(pageRequest);

      if (response.statusCode === HttpStatusCode.Ok) {
        const data = response.data;
        setData(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
        setPageNumber(data.pageable.pageNumber);
      } else if (response.statusCode === HttpStatusCode.Unauthorized) {
        logout();
      } else {
        setAlert({ open: true, message: intl.formatMessage({ id: 'unknown-error' }), severity: 'error' });
      }
    };

    fetchStudents();
  }, [pageRequest, intl, logout, reload]);

  useEffect(() => {
    const fetchClasses = async () => {
      setLoadingClasses(true);
      const response = await getManageClasses({ page: 0, size: 1000, status: '1' });
      if (response.statusCode === HttpStatusCode.Ok) {
        setClasses(response.data.content);
      } else if (response.statusCode === HttpStatusCode.Unauthorized) {
        logout();
      }
      setLoadingClasses(false);
    };
    fetchClasses();
  }, [logout]);

  useEffect(() => {
    if (location.state?.alert) {
      setAlert(location.state.alert);
      window.history.replaceState({}, document.title);
    }
  }, [location.state]);

  const table = useReactTable({
    data: data,
    columns,
    manualPagination: true,
    defaultColumn: { cell: RowEditable },
    getRowId: (row: Student) => (row.id ?? '').toString(),
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
          setData((old: Student[]) => old.map((row, index) => (index === rowIndex ? originalData[rowIndex] : row)));
        } else {
          setOriginalData((old) => old.map((row, index) => (index === rowIndex ? data[rowIndex] : row)));
        }
      },
      updateData: (rowIndex, columnId, value) => {
        setData((old: Student[]) =>
          old.map((row, index) => {
            if (index === rowIndex) {
              return { ...old[rowIndex]!, [columnId]: value };
            }
            return row;
          })
        );
      }
    }
  });

  // Handle Column Drag End
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

  // Handle Row Drag End
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

  useEffect(() => {
    if ([1].includes(user?.roleId ?? 0)) {
      setHasAddPermission(true);
    }
  }, [user?.roleId]);

  const handleImportButtonClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileImportChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files ? event.target.files[0] : null;

    if (file) {
      const fileTypes = ['application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'application/vnd.ms-excel'];

      if (!fileTypes.includes(file.type)) {
        setAlert({
          open: true,
          message: 'File lỗi định dạng. Vui lòng thử lại',
          severity: 'error'
        });

        event.target.value = '';
        return;
      }

      const formData = new FormData();
      formData.append('file', file);

      try {
        const response = await importStudent(formData);

        if (response.statusCode === HttpStatusCode.Ok) {
          setAlert({ open: true, message: 'Nhập từ Excel thành công', severity: 'success' });
          setReload(!reload);
        } else if (response.statusCode === HttpStatusCode.Unauthorized) {
          logout();
        } else if (response.statusCode === HttpStatusCode.UnprocessableEntity) {
          setAlert({ open: true, message: response.message, severity: 'error' });
        } else {
          setAlert({ open: true, message: 'Lỗi hệ thống, vui lòng thử lại sau', severity: 'error' });
        }
      } catch {
        setAlert({ open: true, message: 'Lỗi hệ thống, vui lòng thử lại sau', severity: 'error' });
      }

      event.target.value = '';
    }
  };

  const handleDownloadExcelForm = async () => {
    const blob = await downloadStudentImportTemplate();
    const fileURL = URL.createObjectURL(blob);
    const link = document.createElement('a');

    link.href = fileURL;
    link.download = 'Mẫu import sinh viên.xlsx';

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    URL.revokeObjectURL(fileURL);
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
          Danh sách sinh viên
        </Typography>

        {hasAddPermission && (
          <Stack direction="row" spacing={2}>
            <input
              type="file"
              ref={fileInputRef}
              onChange={(e) => handleFileImportChange(e)}
              style={{ display: 'none' }}
              accept=".xlsx, .xls"
            />
            <Button
              sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 1 }}
              variant="contained"
              color="primary"
              onClick={() => handleImportButtonClick()}
            >
              <ImportCurve />
              <Typography variant="h6">Nhập từ Excel</Typography>
            </Button>

            <Divider orientation="vertical" flexItem />

            <Button
              sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 1 }}
              variant="contained"
              onClick={() => handleDownloadExcelForm()}
              color="primary"
              size="medium"
            >
              <ExportCurve />
              Xuất file mẫu
            </Button>

            <Divider orientation="vertical" flexItem />

            <Button
              sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}
              variant="contained"
              onClick={() => navigate('/student/add')}
              startIcon={<Add />}
            >
              Thêm sinh viên
            </Button>
          </Stack>
        )}
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
            sx={{
              width: '100%',
              borderRadius: 2,
              fontSize: 15,
              textAlign: 'center',
              py: 1.5,
              px: 2
            }}
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
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => setGlobalFilter(e.target.value)}
              onKeyDown={(e: React.KeyboardEvent<HTMLInputElement>) => {
                if (e.key === 'Enter') {
                  setPageRequest({ ...pageRequest, page: 0, keyword: globalFilter });
                }
              }}
              placeholder={'Tìm kiếm mã, tên, email'}
              sx={{ minWidth: 200 }}
            />

            <Autocomplete
              id="manageClassId"
              options={classes}
              loading={loadingClasses}
              getOptionLabel={(option) => option.name || ''}
              value={classes.find((c) => c.id === Number(pageRequest.manageClassId)) || null}
              onChange={(_, newValue) => {
                setPageRequest({ ...pageRequest, page: 0, manageClassId: newValue ? newValue.id.toString() : '' });
              }}
              sx={{
                minWidth: 200,
                '& .MuiOutlinedInput-root': {
                  height: '48.2px'
                }
              }}
              renderInput={(params) => (
                <TextField
                  {...params}
                  placeholder="Lớp quản lý"
                  InputProps={{
                    ...params.InputProps,
                    endAdornment: (
                      <>
                        {loadingClasses ? <CircularProgress color="inherit" size={20} /> : null}
                        {params.InputProps.endAdornment}
                      </>
                    )
                  }}
                />
              )}
            />

            <Select
              value={pageRequest.status}
              onChange={(event) => setPageRequest({ ...pageRequest, page: 0, status: event.target.value })}
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
              <Stack direction="row" sx={{ gap: 1, alignItems: 'center' }}>
                <Typography variant="caption" color="secondary">
                  Số bản ghi mỗi trang
                </Typography>

                <FormControl sx={{ m: 1 }}>
                  <Select
                    id="demo-controlled-open-select"
                    value={pageRequest.size}
                    onChange={handleChangePageSize}
                    size="small"
                    sx={{ '& .MuiSelect-select': { py: 0.75, px: 1.25 } }}
                  >
                    {options.map((option: number) => (
                      <MenuItem key={option} value={option}>
                        {option}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Stack>
            </Stack>
          </Grid>

          <Grid sx={{ mt: { xs: 2, sm: 0 }, mb: 2 }}>
            <Pagination
              count={totalPages}
              variant="contained"
              page={pageNumber + 1}
              onChange={(event, value) => setPageRequest({ ...pageRequest, page: value - 1 })}
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
