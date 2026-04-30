import { useEffect, useMemo, useState } from 'react';

import {
  Alert,
  Avatar,
  Box,
  Button,
  Chip,
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
  TextField,
  Tooltip,
  Typography
} from '@mui/material';
import { getList, create, update, deleteById } from 'api/ban-application';
import IconButton from 'components/@extended/IconButton';
import MainCard from 'components/MainCard';
import { EmptyTable, HeaderSort } from 'components/third-party/react-table';
import useAuth from 'hooks/useAuth';
import { Add, Edit2, Trash } from 'iconsax-reactjs';
import { HttpStatusCode } from 'axios';
import { isBanApplication } from 'types/ban-application';
import { DEFAULT_PAGE_SIZE, PageRequest } from 'types/paging';
import {
  ColumnDef,
  ColumnFiltersState,
  SortingState,
  flexRender,
  getCoreRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  useReactTable,
  Row
} from '@tanstack/react-table';

// ==============================|| FORM DIALOG ||============================== //

function BanApplicationFormDialog({
  open,
  onClose,
  item,
  reload,
  setReload,
  setAlert
}: {
  open: boolean;
  onClose: () => void;
  item: isBanApplication | null;
  reload: boolean;
  setReload: (v: boolean) => void;
  setAlert: React.Dispatch<React.SetStateAction<{ open: boolean; message: string; severity: 'success' | 'error' | 'info' | 'warning' }>>;
}) {
  const { logout } = useAuth();
  const isEdit = item !== null;

  const [applicationName, setApplicationName] = useState('');
  const [applicationNameError, setApplicationNameError] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (open) {
      setApplicationName(item?.applicationName ?? '');
      setImageUrl(item?.imageUrl ?? '');
      setApplicationNameError('');
    }
  }, [open, item]);

  const validate = (): boolean => {
    if (!applicationName.trim()) {
      setApplicationNameError('Tên ứng dụng không được phép bỏ trống');
      return false;
    }
    setApplicationNameError('');
    return true;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    setSubmitting(true);
    try {
      const payload = { applicationName: applicationName.trim(), imageUrl: imageUrl.trim() || null } as isBanApplication;
      const response = isEdit ? await update(item!.id, payload) : await create(payload);

      if (response.statusCode === HttpStatusCode.Ok) {
        setAlert({ open: true, message: isEdit ? 'Cập nhật thành công' : 'Thêm mới thành công', severity: 'success' });
        setReload(!reload);
        onClose();
      } else if (response.statusCode === HttpStatusCode.Unauthorized) {
        logout();
      } else if (response.statusCode === HttpStatusCode.UnprocessableEntity) {
        setAlert({ open: true, message: response.data, severity: 'error' });
      } else {
        setAlert({ open: true, message: 'Lỗi không xác định', severity: 'error' });
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{isEdit ? 'Cập nhật ứng dụng cấm' : 'Thêm ứng dụng cấm'}</DialogTitle>
      <DialogContent>
        <TextField
          autoFocus
          margin="dense"
          label="Tên ứng dụng"
          fullWidth
          variant="outlined"
          value={applicationName}
          onChange={(e) => {
            setApplicationName(e.target.value);
            if (applicationNameError) setApplicationNameError('');
          }}
          error={!!applicationNameError}
          helperText={applicationNameError}
          sx={{ mt: 1 }}
        />
        <TextField
          margin="dense"
          label="URL ảnh (tuỳ chọn)"
          fullWidth
          variant="outlined"
          value={imageUrl}
          onChange={(e) => setImageUrl(e.target.value)}
          sx={{ mt: 1 }}
          placeholder="https://example.com/icon.png"
        />
        {imageUrl && (
          <Box mt={1.5} display="flex" alignItems="center" gap={1}>
            <Avatar src={imageUrl} sx={{ width: 40, height: 40 }}>
              {applicationName[0]}
            </Avatar>
            <Typography variant="caption" color="text.secondary">
              Xem trước ảnh
            </Typography>
          </Box>
        )}
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

// ==============================|| EDIT ACTION ||============================== //

function EditAction({
  row,
  reload,
  setReload,
  setAlert,
  onEdit
}: {
  row: Row<isBanApplication>;
  reload: boolean;
  setReload: (v: boolean) => void;
  setAlert: React.Dispatch<React.SetStateAction<{ open: boolean; message: string; severity: 'success' | 'error' | 'info' | 'warning' }>>;
  onEdit: (item: isBanApplication) => void;
}) {
  const { logout } = useAuth();
  const [openDelete, setOpenDelete] = useState(false);

  const handleDelete = async () => {
    const response = await deleteById(row.original.id);
    if (response.statusCode === HttpStatusCode.Ok) {
      setAlert({ open: true, message: 'Xoá ứng dụng cấm thành công', severity: 'success' });
      setReload(!reload);
    } else if (response.statusCode === HttpStatusCode.Unauthorized) {
      logout();
    } else {
      setAlert({ open: true, message: 'Lỗi không xác định', severity: 'error' });
    }
    setOpenDelete(false);
  };

  return (
    <Stack direction="row" sx={{ gap: 1, alignItems: 'center' }}>
      <Tooltip title="Chỉnh sửa">
        <IconButton color="primary" onClick={() => onEdit(row.original)}>
          <Edit2 variant="Outline" />
        </IconButton>
      </Tooltip>

      <Tooltip title="Xoá">
        <IconButton color="error" onClick={() => setOpenDelete(true)}>
          <Trash variant="Outline" />
        </IconButton>
      </Tooltip>

      <Dialog open={openDelete} onClose={() => setOpenDelete(false)}>
        <DialogTitle>Xác nhận xoá</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Bạn có chắc muốn xoá ứng dụng <strong>{row.original.applicationName}</strong> không?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button variant="contained" color="primary" onClick={() => setOpenDelete(false)}>
            Huỷ
          </Button>
          <Button variant="contained" color="error" onClick={handleDelete} autoFocus>
            Xác nhận
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

// ==============================|| BAN APPLICATION PAGE ||============================== //

export default function BanApplicationPage() {
  const { logout } = useAuth();

  const [reload, setReload] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<isBanApplication | null>(null);

  const [data, setData] = useState<isBanApplication[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [pageNumber, setPageNumber] = useState(0);
  const [globalFilter, setGlobalFilter] = useState('');
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
  const [sorting, setSorting] = useState<SortingState>([]);
  const [pageRequest, setPageRequest] = useState<PageRequest>({ page: 0, size: DEFAULT_PAGE_SIZE, sort: '', keyword: '', status: '' });
  const [alert, setAlert] = useState({ open: false, message: '', severity: 'success' as 'success' | 'error' | 'info' | 'warning' });

  const handleOpenAdd = () => {
    setEditingItem(null);
    setFormOpen(true);
  };

  const handleOpenEdit = (item: isBanApplication) => {
    setEditingItem(item);
    setFormOpen(true);
  };

  useEffect(() => {
    const fetchData = async () => {
      const response = await getList(pageRequest);
      if (response.statusCode === HttpStatusCode.Ok) {
        setData(response.data.content);
        setTotalPages(response.data.totalPages);
        setTotalElements(response.data.totalElements);
        setPageNumber(response.data.pageable.pageNumber);
      } else if (response.statusCode === HttpStatusCode.Unauthorized) {
        logout();
      } else {
        setAlert({ open: true, message: 'Lỗi không xác định', severity: 'error' });
      }
    };
    fetchData();
  }, [pageRequest, reload, logout]);

  const columns = useMemo<ColumnDef<isBanApplication>[]>(
    () => [
      {
        id: 'stt',
        header: 'STT',
        cell: ({ row }) => pageNumber * (pageRequest.size ?? DEFAULT_PAGE_SIZE) + row.index + 1,
        enableSorting: false,
        meta: { className: 'cell-center', width: '60px' }
      },
      {
        id: 'applicationName',
        header: 'Tên ứng dụng',
        accessorKey: 'applicationName',
        cell: ({ row }) => (
          <Stack direction="row" spacing={1.5} alignItems="center">
            <Avatar src={row.original.imageUrl ?? undefined} sx={{ width: 32, height: 32, fontSize: 14 }}>
              {row.original.applicationName[0]?.toUpperCase()}
            </Avatar>
            <Typography variant="body2">{row.original.applicationName}</Typography>
          </Stack>
        )
      },
      {
        id: 'status',
        header: 'Trạng thái',
        accessorKey: 'status',
        cell: ({ row }) => (
          <Chip
            label={row.original.status === 1 ? 'Hoạt động' : 'Dừng hoạt động'}
            color={row.original.status === 1 ? 'success' : 'error'}
            size="small"
          />
        ),
        meta: { className: 'cell-center' }
      },
      {
        id: 'edit',
        header: 'Hành động',
        cell: ({ row }) => <EditAction row={row} reload={reload} setReload={setReload} setAlert={setAlert} onEdit={handleOpenEdit} />,
        enableSorting: false,
        meta: { className: 'cell-center', width: '120px' }
      }
    ],
    [pageNumber, pageRequest.size, reload]
  );

  const tableInstance = useReactTable<isBanApplication>({
    data,
    columns,
    manualPagination: true,
    state: { columnFilters, sorting },
    onSortingChange: setSorting,
    onColumnFiltersChange: setColumnFilters,
    getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel()
  });

  const handleChangePageSize = (event: SelectChangeEvent<number>) => {
    setPageRequest((prev) => ({ ...prev, size: Number(event.target.value), page: 0 }));
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
          Danh sách ứng dụng cấm
        </Typography>

        <Button variant="contained" onClick={handleOpenAdd} startIcon={<Add />}>
          Thêm ứng dụng
        </Button>
      </Stack>

      <MainCard content={false}>
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

        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          sx={(theme) => ({
            gap: 2,
            justifyContent: 'left',
            p: 2,
            [theme.breakpoints.down('sm')]: { '& .MuiOutlinedInput-root, & .MuiFormControl-root': { width: 1 } }
          })}
        >
          <Stack direction="row" spacing={2} sx={{ flexGrow: 1 }}>
            <OutlinedInput
              value={globalFilter}
              onChange={(e) => setGlobalFilter(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') setPageRequest((prev) => ({ ...prev, page: 0, keyword: globalFilter }));
              }}
              placeholder="Tìm kiếm tên ứng dụng"
              sx={{ minWidth: 200 }}
            />
            <Select
              value={pageRequest.status as string || ''}
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

        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              {tableInstance.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableCell key={header.id} colSpan={header.colSpan} {...header.column.columnDef.meta}>
                      {header.isPlaceholder ? null : (
                        <Stack direction="row" sx={{ gap: 1, alignItems: 'center', justifyContent: 'space-between' }}>
                          <Box>{flexRender(header.column.columnDef.header, header.getContext())}</Box>
                          {header.column.getCanSort() && <HeaderSort column={header.column} sort />}
                        </Stack>
                      )}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableHead>

            <TableBody>
              {tableInstance.getRowModel().rows.length > 0 ? (
                tableInstance.getRowModel().rows.map((row) => (
                  <TableRow key={row.id}>
                    {row.getVisibleCells().map((cell) => (
                      <TableCell key={cell.id} {...cell.column.columnDef.meta}>
                        {flexRender(cell.column.columnDef.cell, cell.getContext())}
                      </TableCell>
                    ))}
                  </TableRow>
                ))
              ) : (
                <TableRow sx={{ '&.MuiTableRow-root:hover': { bgcolor: 'transparent' } }}>
                  <TableCell colSpan={columns.length}>
                    <EmptyTable msg="Không có dữ liệu" />
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>

        <Divider />

        <Grid spacing={1} container sx={{ alignItems: 'center', justifyContent: 'space-between', width: 'auto', p: 1 }}>
          <Grid>
            <Stack direction="row" sx={{ gap: 1, alignItems: 'center', marginLeft: 2 }}>
              <Typography variant="caption" color="secondary">
                Số bản ghi mỗi trang
              </Typography>
              <FormControl sx={{ m: 1 }}>
                <Select
                  value={pageRequest.size}
                  onChange={handleChangePageSize}
                  size="small"
                  sx={{ '& .MuiSelect-select': { py: 0.75, px: 1.25 }, borderRadius: 1 }}
                >
                  {[10, 25, 50, 100].map((option) => (
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
              variant="outlined"
              page={pageNumber + 1}
              onChange={(_, value) => setPageRequest((prev) => ({ ...prev, page: value - 1 }))}
              color="primary"
              showFirstButton
              showLastButton
            />
          </Grid>
        </Grid>
      </MainCard>

      <BanApplicationFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        item={editingItem}
        reload={reload}
        setReload={setReload}
        setAlert={setAlert}
      />
    </Stack>
  );
}
