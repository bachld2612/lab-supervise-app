import { useEffect, useState } from 'react';
import { Alert, Box, Button, CircularProgress, Divider, Snackbar, Stack, TextField, Typography } from '@mui/material';
import MainCard from 'components/MainCard';
import { getMyPC, updateMyPC } from 'api/personal-computer';

function extractErrorMessage(error: unknown, fallback: string): string {
  if (error && typeof error === 'object') {
    const e = error as Record<string, unknown>;
    if (typeof e.message === 'string') return e.message;
  }
  return fallback;
}

// ==============================|| PERSONAL COMPUTER PAGE ||============================== //

export default function PersonalComputerPage() {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [ipAddress, setIpAddress] = useState('');
  const [ipError, setIpError] = useState('');
  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  useEffect(() => {
    const fetchCurrentIP = async () => {
      try {
        const res = await getMyPC();
        if (res.data?.ipAddress) {
          setIpAddress(res.data.ipAddress);
        }
      } finally {
        setLoading(false);
      }
    };
    fetchCurrentIP();
  }, []);

  const handleSave = async () => {
    if (!ipAddress.trim()) {
      setIpError('Địa chỉ IP không được phép bỏ trống');
      return;
    }
    setIpError('');
    setSaving(true);
    try {
      await updateMyPC(ipAddress.trim());
      setAlert({ open: true, message: 'Cập nhật thành công', severity: 'success' });
    } catch (error: unknown) {
      setAlert({ open: true, message: extractErrorMessage(error, 'Cập nhật thất bại, vui lòng thử lại'), severity: 'error' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
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

      <MainCard>
        <Stack spacing={0.5} sx={{ mb: 2.5 }}>
          <Typography variant="h5" fontWeight="bold">
            Thông tin máy tính
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Cập nhật địa chỉ IP của máy tính cá nhân
          </Typography>
        </Stack>

        <Divider sx={{ mb: 3 }} />

        {loading ? (
          <Box display="flex" justifyContent="center" py={4}>
            <CircularProgress />
          </Box>
        ) : (
          <Stack spacing={2.5} sx={{ maxWidth: 480 }}>
            <Stack spacing={1}>
              <Typography variant="body2" fontWeight="bold">
                Địa chỉ IP
              </Typography>
              <TextField
                fullWidth
                value={ipAddress}
                onChange={(e) => {
                  setIpAddress(e.target.value);
                  if (ipError) setIpError('');
                }}
                placeholder="Chưa đăng ký"
                error={!!ipError}
                helperText={ipError}
              />
            </Stack>

            <Box>
              <Button variant="contained" onClick={handleSave} disabled={saving} sx={{ px: 3.5 }}>
                {saving ? <CircularProgress size={20} color="inherit" /> : 'Lưu thay đổi'}
              </Button>
            </Box>
          </Stack>
        )}
      </MainCard>
    </>
  );
}
