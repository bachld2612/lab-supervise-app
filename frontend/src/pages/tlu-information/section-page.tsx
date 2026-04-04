import { CSSProperties, Fragment, useEffect, useMemo, useState } from 'react';
import { useLocation } from 'react-router-dom';

// material-ui
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
import TextField from '@mui/material/TextField';
import Autocomplete from '@mui/material/Autocomplete';
import CircularProgress from '@mui/material/CircularProgress';

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
import { Add, ArrowDown2, ArrowRight2, Command, Edit2, TableDocument, Trash } from 'iconsax-reactjs';
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
import { type Section } from 'types/section';
import { type Department } from 'types/department';
import { getList, deleteById, create, update } from 'api/section';
import { getList as getDepartments } from 'api/department';
import { DEFAULT_PAGE_SIZE, PageRequest } from 'types/paging';

const fuzzyFilter: FilterFn<Section> = (row, columnId, value, addMeta) => {
  const itemRank = rankItem(row.getValue(columnId), value);
  addMeta(itemRank);
  return itemRank.passed;
};

// ==============================|| SECTION FORM DIALOG ||============================== //

function SectionFormDialog({
  open,
  onClose,
  section,
  reload,
  setReload,
  setAlert
}: {
  open: boolean;
  onClose: () => void;
  section: Section | null;
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
  const { logout } = useAuth();
  const [name, setName] = useState('');
  const [nameError, setNameError] = useState('');
  const [departmentId, setDepartmentId] = useState<number>(0);
  const [departmentError, setDepartmentError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [departments, setDepartments] = useState<Department[]>([]);
  const [loadingDepartments, setLoadingDepartments] = useState(false);

  const isEdit = section !== null;

  useEffect(() => {
    const fetchDepartments = async () => {
      setLoadingDepartments(true);
      const response = await getDepartments({ page: 0, size: 1000, status: '1' });
      if (response.statusCode === HttpStatusCode.Ok) {
        setDepartments(response.data.content);
      } else if (response.statusCode === HttpStatusCode.Unauthorized) {
        logout();
      }
      setLoadingDepartments(false);
    };
    fetchDepartments();
  }, [logout]);

  useEffect(() => {
    if (open) {
      if (section) {
        setName(section.name);
        setDepartmentId(section.departmentId);
      } else {
        setName('');
        setDepartmentId(0);
      }
      setNameError('');
      setDepartmentError('');
    }
  }, [open, section]);

  const validate = (): boolean => {
    let valid = true;
    if (!name.trim()) {
      setNameError('Tên bộ môn không được phép bỏ trống');
      valid = false;
    } else {
      setNameError('');
    }
    if (!departmentId || departmentId === 0) {
      setDepartmentError('Khoa không được phép bỏ trống');
      valid = false;
    } else {
      setDepartmentError('');
    }
    return valid;
  };

  const handleSubmit = async () => {
    if (!validate()) return;

    setSubmitting(true);

    try {
      if (isEdit) {
        const response = await update({ name, departmentId } as Section, section.id);

        if (response.statusCode == HttpStatusCode.Ok) {
          setAlert({ open: true, message: 'Cập nhật bộ môn thành công', severity: 'success' });
          setReload(!reload);
          onClose();
        } else if (response.statusCode == HttpStatusCode.Unauthorized) {
          logout();
        } else if (response.statusCode == HttpStatusCode.UnprocessableEntity) {
          setAlert({ open: true, message: response.data, severity: 'error' });
        } else {
          setAlert({ open: true, message: 'Lỗi không xác định', severity: 'error' });
        }
      } else {
        const response = await create({ name, departmentId } as Section);

        if (response.statusCode == HttpStatusCode.Ok) {
          setAlert({ open: true, message: 'Thêm bộ môn thành công', severity: 'success' });
          setReload(!reload);
          onClose();
        } else if (response.statusCode == HttpStatusCode.Unauthorized) {
          logout();
        } else if (response.statusCode == HttpStatusCode.UnprocessableEntity) {
          setAlert({ open: true, message: response.data, severity: 'error' });
        } else {
          setAlert({ open: true, message: 'Lỗi không xác định', severity: 'error' });
        }
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} aria-labelledby="section-form-dialog-title" maxWidth="sm" fullWidth>
      <DialogTitle id="section-form-dialog-title">{isEdit ? 'Cập nhật bộ môn' : 'Thêm bộ môn mới'}</DialogTitle>

      <DialogContent>
        <TextField
          autoFocus
          margin="dense"
          label="Tên bộ môn"
          type="text"
          fullWidth
          variant="outlined"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            if (nameError) setNameError('');
          }}
          error={!!nameError}
          helperText={nameError}
          sx={{ mt: 1 }}
        />

        <Autocomplete
          options={departments}
          loading={loadingDepartments}
          getOptionLabel={(option) => option.name || ''}
          value={departments.find((d) => d.id === departmentId) || null}
          onChange={(_, newValue) => {
            setDepartmentId(newValue ? newValue.id : 0);
            if (departmentError) setDepartmentError('');
          }}
          isOptionEqualToValue={(option, value) => option.id === value.id}
          renderInput={(params) => (
            <TextField
              {...params}
              margin="dense"
              label="Khoa"
              variant="outlined"
              fullWidth
              error={!!departmentError}
              helperText={departmentError}
              InputProps={{
                ...params.InputProps,
                endAdornment: (
                  <>
                    {loadingDepartments ? <CircularProgress color="inherit" size={20} /> : null}
                    {params.InputProps.endAdornment}
                  </>
                )
              }}
            />
          )}
          sx={{ mt: 1 }}
        />
      </DialogContent>

      <DialogActions>
        <Button variant="contained" color="primary" onClick={onClose}>
          Huỷ
        </Button>

        <Button variant="contained" color="success" onClick={handleSubmit} disabled={submitting}>
          {isEdit ? 'Cập nhật' : 'Thêm mới'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

// ==============================|| REACT TABLE - EDIT ACTION ||============================== //

function EditAction({
  row,
  reload,
  setReload,
  setAlert,
  onEdit
}: {
  row: Row<Section>;
  reload: boolean;
  setReload: (e: boolean) => void;
  setAlert: React.Dispatch<
    React.SetStateAction<{
      open: boolean;
      message: string;
      severity: 'success' | 'error' | 'info' | 'warning';
    }>
  >;
  onEdit: (section: Section) => void;
}) {
  const { user, logout } = useAuth();
  const [hasEditPermission, setHasEditPermission] = useState(false);
  const [hasDeletePermission, setHasDeletePermission] = useState(false);
  const [openDelete, setOpenDelete] = useState(false);

  useEffect(() => {
    if ([1].includes(user?.roleId ?? 0)) {
      setHasEditPermission(true);
      setHasDeletePermission(true);
    }
  }, [user?.roleId]);

  const handleDelete = async () => {
    const response = await deleteById(row.original.id);

    if (response.statusCode == HttpStatusCode.Ok) {
      setAlert({ open: true, message: 'Xóa bộ môn thành công', severity: 'success' });
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

  return (
    <Stack direction="row" sx={{ gap: 1, alignItems: 'center' }}>
      {hasEditPermission && (
        <Tooltip title="Chỉnh sửa">
          <IconButton color="primary" onClick={() => onEdit(row.original)} disabled={row.original.status == 0}>
            <Edit2 variant="Outline" />
          </IconButton>
        </Tooltip>
      )}

      {hasDeletePermission && (
        <Tooltip title="Xóa">
          <IconButton disabled={row.original.status == 0} color="primary" onClick={() => setOpenDelete(true)}>
            <Trash variant="Outline" />
          </IconButton>
        </Tooltip>
      )}

      <Dialog
        open={openDelete}
        onClose={() => setOpenDelete(false)}
        aria-labelledby="alert-dialog-title"
        aria-describedby="alert-dialog-description"
      >
        <DialogTitle id="alert-dialog-title">Bạn có muốn xóa bộ môn này không?</DialogTitle>

        <DialogContent>
          <DialogContentText id="alert-dialog-description">Khi xóa bộ môn, tất cả thông tin đi kèm cũng sẽ bị xóa.</DialogContentText>
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

// ==============================|| REACT TABLE - MAIN ||============================== //

export default function SectionPage() {
  const { logout, user } = useAuth();
  const intl = useIntl();
  const [reload, setReload] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [editingSection, setEditingSection] = useState<Section | null>(null);

  const handleOpenAdd = () => {
    setEditingSection(null);
    setFormOpen(true);
  };

  const handleOpenEdit = (section: Section) => {
    setEditingSection(section);
    setFormOpen(true);
  };

  const handleCloseForm = () => {
    setFormOpen(false);
    setEditingSection(null);
  };

  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  const columns = useMemo<ColumnDef<Section>[]>(
    () => [
      {
        id: 'id',
        title: 'Id',
        header: '#',
        accessorKey: 'id',
        dataType: 'text',
        enableColumnFilter: false,
        enableGrouping: false,
        meta: { className: 'cell-center' }
      },
      {
        id: 'name',
        header: 'Tên bộ môn',
        accessorKey: 'name',
        dataType: 'text',
        enableGrouping: false
      },
      {
        id: 'departmentName',
        header: 'Tên khoa',
        accessorKey: 'departmentName',
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
        cell: ({ row }) => <EditAction row={row} reload={reload} setReload={setReload} setAlert={setAlert} onEdit={handleOpenEdit} />,
        enableGrouping: false,
        meta: { className: 'cell-center', width: '10%' }
      }
    ],
    [reload]
  );
  let options: number[] = [10, 25, 50, 100];
  const [data, setData] = useState<Section[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [pageNumber, setPageNumber] = useState(0);
  const [pageRequest, setPageRequest] = useState<PageRequest>({
    page: 0,
    size: DEFAULT_PAGE_SIZE,
    sort: '',
    keyword: '',
    status: ''
  });
  const [hasAddPermission, setHasAddPermission] = useState(false);
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
    const fetchSections = async () => {
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

    fetchSections();
  }, [pageRequest, intl, logout, reload]);

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
    getRowId: (row: Section) => (row.id ?? '').toString(),
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
          setData((old: Section[]) => old.map((row, index) => (index === rowIndex ? originalData[rowIndex] : row)));
        } else {
          setOriginalData((old) => old.map((row, index) => (index === rowIndex ? data[rowIndex] : row)));
        }
      },
      updateData: (rowIndex, columnId, value) => {
        setData((old: Section[]) =>
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

  return (
    <Stack
      sx={() => ({
        p: 0
      })}
    >
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
          Danh sách bộ môn
        </Typography>

        {hasAddPermission && (
          <Button
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
            variant="contained"
            onClick={handleOpenAdd}
            startIcon={<Add />}
          >
            Thêm bộ môn
          </Button>
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
              placeholder={'Tìm kiếm tên bộ môn'}
              sx={{ minWidth: 100 }}
              inputProps={{
                sx: {
                  textOverflow: 'ellipsis',
                  '&::placeholder': {
                    textOverflow: 'ellipsis',
                    overflow: 'hidden',
                    whiteSpace: 'nowrap'
                  }
                }
              }}
            />

            <Select
              value={pageRequest.status}
              onChange={(event) => setPageRequest({ ...pageRequest, page: 0, status: event.target.value })}
              displayEmpty
              input={<OutlinedInput />}
              slotProps={{ input: { 'aria-label': 'Status Filter' } }}
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

        {/* Column DnD Context */}
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
                {/* Row DnD Context */}
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

          <Grid sx={{ mt: { xs: 2, sm: 0 } }}>
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

      {/* Section Form Dialog */}
      <SectionFormDialog
        open={formOpen}
        onClose={handleCloseForm}
        section={editingSection}
        reload={reload}
        setReload={setReload}
        setAlert={setAlert}
      />
    </Stack>
  );
}
