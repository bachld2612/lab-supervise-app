import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
  Snackbar,
  Stack,
  TextField,
  Typography
} from '@mui/material';
import { CloseCircle, DocumentUpload, Key } from 'iconsax-reactjs';
import { useRef, useState } from 'react';
import { getVeyonPublicKey, importVeyonKey } from 'api/veyon';
import { importVeyonKey as importVeyonKeyForExamRoom } from 'api/exam-room';
import { HttpStatusCode } from 'axios';

async function importRsaPublicKey(base64Key: string): Promise<CryptoKey> {
  const binaryDer = Uint8Array.from(atob(base64Key), (c) => c.charCodeAt(0));
  return await crypto.subtle.importKey('spki', binaryDer.buffer, { name: 'RSA-OAEP', hash: 'SHA-256' }, false, ['encrypt']);
}

async function rsaEncryptLargeString(plainText: string, publicKey: CryptoKey): Promise<string> {
  const bytes = new TextEncoder().encode(plainText);
  const CHUNK_SIZE = 190;
  const encryptedChunks: string[] = [];

  for (let i = 0; i < bytes.length; i += CHUNK_SIZE) {
    const chunk = bytes.slice(i, i + CHUNK_SIZE);
    const encryptedBuffer = await crypto.subtle.encrypt({ name: 'RSA-OAEP' }, publicKey, chunk);
    encryptedChunks.push(btoa(String.fromCharCode(...new Uint8Array(encryptedBuffer))));
  }

  return encryptedChunks.join('|');
}

function extractApiErrorMessage(error: unknown, fallback: string): string {
  if (error && typeof error === 'object') {
    const e = error as Record<string, unknown>;
    if (e.statusCode === HttpStatusCode.UnprocessableEntity && typeof e.message === 'string') return e.message;
  }
  return fallback;
}

interface ImportVeyonKeyDialogProps {
  open: boolean;
  onClose: () => void;
  classId: number;
  isExamRoom?: boolean;
}

export default function ImportVeyonKeyDialog({ open, onClose, classId, isExamRoom }: ImportVeyonKeyDialogProps) {
  const [keyName, setKeyName] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' as 'success' | 'error' });
  const fileInputRef = useRef<HTMLInputElement>(null);

  const showSnackbar = (message: string, severity: 'success' | 'error') => setSnackbar({ open: true, message, severity });

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSelectedFile(e.target.files?.[0] ?? null);
  };

  const handleClose = () => {
    if (loading) return;
    setKeyName('');
    setSelectedFile(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
    onClose();
  };

  const handleSubmit = async () => {
    if (!keyName.trim()) {
      showSnackbar('Vui lòng nhập tên khóa', 'error');
      return;
    }
    if (!selectedFile) {
      showSnackbar('Vui lòng chọn file .pem', 'error');
      return;
    }

    setLoading(true);
    try {
      // Bước 1: Lấy public key từ server (gọi ngay trước khi import, không cache)
      const publicKeyRes = await getVeyonPublicKey();
      if (publicKeyRes.statusCode !== HttpStatusCode.Ok || !publicKeyRes.data) {
        showSnackbar('Không lấy được public key từ server', 'error');
        return;
      }

      // Bước 2: Import public key vào Web Crypto API
      const publicKey = await importRsaPublicKey(publicKeyRes.data);

      // Bước 3: Đọc nội dung file .pem
      const pemContent = await selectedFile.text();

      // Bước 4: Mã hóa RSA-OAEP, chia chunk 190 bytes, nối bằng "|"
      const encryptedKeyData = await rsaEncryptLargeString(pemContent, publicKey);

      // Bước 5: Gửi lên server
      const res = isExamRoom
        ? await importVeyonKeyForExamRoom(classId, keyName.trim(), encryptedKeyData)
        : await importVeyonKey(classId, keyName.trim(), encryptedKeyData);
      if (res.statusCode === HttpStatusCode.Ok) {
        showSnackbar('Import khóa Veyon thành công', 'success');
        setTimeout(() => handleClose(), 1500);
      }
    } catch (error: unknown) {
      const isClientError = error instanceof DOMException || error instanceof TypeError;
      if (isClientError) {
        showSnackbar('Lỗi mã hóa file, vui lòng kiểm tra lại file .pem', 'error');
      } else {
        showSnackbar(extractApiErrorMessage(error, 'Import khóa thất bại, vui lòng thử lại'), 'error');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Dialog open={open} onClose={handleClose} maxWidth="xs" fullWidth slotProps={{ paper: { sx: { borderRadius: 3 } } }}>
        <DialogTitle sx={{ pb: 1, pt: 2.5 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Stack direction="row" spacing={1.5} alignItems="center">
              <Key size={20} />
              <Typography variant="h5">Import khóa Veyon</Typography>
            </Stack>
            <IconButton onClick={handleClose} size="small" disabled={loading} sx={{ color: 'text.secondary' }}>
              <CloseCircle size={20} />
            </IconButton>
          </Stack>
        </DialogTitle>

        <DialogContent sx={{ pt: 1, pb: 2.5 }}>
          <Stack spacing={2.5}>
            <TextField
              label="Tên khóa"
              placeholder="Ví dụ: lab101-key"
              value={keyName}
              onChange={(e) => setKeyName(e.target.value)}
              fullWidth
              size="small"
              disabled={loading}
              onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
            />

            <Stack spacing={0.75}>
              <Typography variant="body2" color="text.secondary" fontWeight="medium">
                File khóa (.pem)
              </Typography>
              <Box
                component="label"
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 1.5,
                  px: 2,
                  py: 1.5,
                  border: '1.5px dashed',
                  borderColor: selectedFile ? 'primary.main' : 'divider',
                  borderRadius: 2,
                  cursor: loading ? 'not-allowed' : 'pointer',
                  bgcolor: selectedFile ? 'primary.lighter' : 'background.paper',
                  transition: 'all 0.15s',
                  '&:hover': loading ? {} : { borderColor: 'primary.main', bgcolor: 'primary.lighter' }
                }}
              >
                <DocumentUpload size={20} color={selectedFile ? 'var(--mui-palette-primary-main)' : 'var(--mui-palette-text-secondary)'} />
                <Typography
                  variant="body2"
                  color={selectedFile ? 'primary.main' : 'text.secondary'}
                  sx={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                >
                  {selectedFile ? selectedFile.name : 'Bấm để chọn file .pem...'}
                </Typography>
                <input ref={fileInputRef} type="file" accept=".pem" hidden disabled={loading} onChange={handleFileChange} />
              </Box>
            </Stack>

            <Button
              variant="contained"
              onClick={handleSubmit}
              disabled={loading || !keyName.trim() || !selectedFile}
              fullWidth
              size="large"
              startIcon={loading ? <CircularProgress size={16} color="inherit" /> : <Key size={18} />}
            >
              {loading ? 'Đang xử lý...' : 'Import khóa'}
            </Button>
          </Stack>
        </DialogContent>
      </Dialog>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar((prev) => ({ ...prev, open: false }))}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Alert severity={snackbar.severity} variant="filled" sx={{ width: '100%', borderRadius: 2, fontSize: 14 }}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}
