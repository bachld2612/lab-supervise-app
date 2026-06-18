import {
  Alert,
  Box,
  Button,
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
  Snackbar,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography
} from '@mui/material';
import { getStudentsByExamRoomId } from 'api/exam-room';
import { HttpStatusCode } from 'axios';
import MainCard from 'components/MainCard';
import { EmptyTable, HeaderSort } from 'components/third-party/react-table';
import useAuth from 'hooks/useAuth';
import { CloseCircle } from 'iconsax-reactjs';
import { useEffect, useMemo, useState } from 'react';
import { useIntl } from 'react-intl';
import { ExamRoom } from 'types/exam-room';
import { DEFAULT_PAGE_SIZE, PageRequest } from 'types/paging';
import { Student } from 'types/student';
import {
  ColumnDef,
  ColumnFiltersState,
  SortingState,
  flexRender,
  getCoreRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  useReactTable
} from '@tanstack/react-table';

export default function ExamRoomStudentListDialog({
  open,
  onClose,
  examRoom
}: {
  open: boolean;
  onClose: () => void;
  examRoom: ExamRoom | null;
}) {
  const { logout } = useAuth();
  const intl = useIntl();

  const [data, setData] = useState<Student[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [pageNumber, setPageNumber] = useState(0);
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
  const [sorting, setSorting] = useState<SortingState>([]);
  const [globalFilter, setGlobalFilter] = useState('');
  const [pageRequest, setPageRequest] = useState<PageRequest>({
    page: 0,
    size: DEFAULT_PAGE_SIZE,
    sort: '',
    keyword: ''
  });
  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  useEffect(() => {
    if (open && examRoom) {
      const fetchStudents = async () => {
        const response = await getStudentsByExamRoomId(examRoom.id, pageRequest);

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

      fetchStudents();
    }
  }, [open, examRoom, pageRequest, logout, intl]);

  const columns = useMemo<ColumnDef<Student>[]>(
    () => [
      {
        id: 'stt',
        header: 'STT',
        cell: ({ row }) => pageNumber * pageRequest.size! + row.index + 1,
        enableSorting: false,
        enableColumnFilter: false,
        meta: { className: 'cell-center' }
      },
      {
        id: 'fullName',
        header: 'Họ và tên',
        accessorKey: 'fullName',
        meta: { width: '30%' }
      },
      {
        id: 'manageClassName',
        header: 'Lớp quản lý',
        accessorKey: 'manageClassName',
        meta: { className: 'cell-center' }
      },
      {
        id: 'phone',
        header: 'Số điện thoại',
        accessorKey: 'phone',
        meta: { className: 'cell-center' }
      },
      {
        id: 'email',
        header: 'Email',
        accessorKey: 'email',
        meta: { width: '30%' }
      }
    ],
    [pageNumber, pageRequest.size]
  );

  const tableInstance = useReactTable<Student>({
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

  const handleClose = () => {
    setGlobalFilter('');
    setSorting([]);
    setColumnFilters([]);
    setPageRequest({ page: 0, size: DEFAULT_PAGE_SIZE, sort: '', keyword: '' });
    setAlert({ open: false, message: '', severity: 'success' });
    onClose();
  };

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      fullWidth
      slotProps={{
        paper: {
          sx: { width: '80%', maxWidth: 'none', height: '90%' }
        }
      }}
      maxWidth={false}
    >
      <DialogTitle sx={{ fontWeight: 'bold' }}>
        Danh sách học viên phòng thi {examRoom?.code}
        <IconButton onClick={handleClose} sx={{ position: 'absolute', right: 8, top: 8 }}>
          <CloseCircle />
        </IconButton>
      </DialogTitle>

      <DialogContent dividers sx={{ pb: 0 }}>
        <Stack>
          <MainCard content={false}>
            <Snackbar
              open={alert.open}
              autoHideDuration={3000}
              onClose={() => setAlert({ ...alert, open: false })}
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
                <OutlinedInput
                  value={globalFilter ?? ''}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => setGlobalFilter(e.target.value)}
                  onKeyDown={(e: React.KeyboardEvent<HTMLInputElement>) => {
                    if (e.key === 'Enter') {
                      setPageRequest((prev) => ({ ...prev, page: 0, keyword: globalFilter }));
                    }
                  }}
                  placeholder={'Tìm kiếm'}
                  sx={{ minWidth: 100 }}
                />
              </Stack>
              <Typography variant="caption" color="secondary" sx={{ display: 'flex', alignItems: 'center' }}>
                Tổng cộng: {totalElements}&nbsp; bản ghi
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
        </Stack>
      </DialogContent>

      <Box textAlign="right" px={3} py={2}>
        <Button
          variant="contained"
          sx={{ bgcolor: 'orangered', px: 4, '&:hover': { bgcolor: '#ffb324ff' } }}
          size="large"
          onClick={handleClose}
        >
          Đóng
        </Button>
      </Box>
    </Dialog>
  );
}
