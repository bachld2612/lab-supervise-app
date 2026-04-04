import { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  InputAdornment,
  Snackbar,
  TextField
} from '@mui/material';
import { HttpStatusCode } from 'axios';
import { changePassword as changePasswordApi } from 'api/user';
import useAuth from 'hooks/useAuth';
import { Eye, EyeSlash } from 'iconsax-reactjs';

interface ChangePasswordDialogProps {
  open: boolean;
  onClose: () => void;
}

export default function ChangePasswordDialog({ open, onClose }: ChangePasswordDialogProps) {
  const { logout } = useAuth();

  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [oldPasswordError, setOldPasswordError] = useState('');
  const [newPasswordError, setNewPasswordError] = useState('');
  const [confirmPasswordError, setConfirmPasswordError] = useState('');

  const [showOldPassword, setShowOldPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [submitting, setSubmitting] = useState(false);
  const [alert, setAlert] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'info' | 'warning'
  });

  useEffect(() => {
    if (open) {
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setOldPasswordError('');
      setNewPasswordError('');
      setConfirmPasswordError('');
      setShowOldPassword(false);
      setShowNewPassword(false);
      setShowConfirmPassword(false);
    }
  }, [open]);

  const validate = (): boolean => {
    let valid = true;

    if (!oldPassword.trim()) {
      setOldPasswordError('Mật khẩu hiện tại không được phép bỏ trống');
      valid = false;
    } else {
      setOldPasswordError('');
    }

    if (!newPassword.trim()) {
      setNewPasswordError('Mật khẩu mới không được phép bỏ trống');
      valid = false;
    } else if (newPassword.length < 6) {
      setNewPasswordError('Mật khẩu phải chứa 6 kí tự trở lên');
      valid = false;
    } else {
      setNewPasswordError('');
    }

    if (!confirmPassword.trim()) {
      setConfirmPasswordError('Xác nhận mật khẩu không được phép bỏ trống');
      valid = false;
    } else if (newPassword !== confirmPassword) {
      setConfirmPasswordError('Mật khẩu mới và xác nhận mật khẩu không trùng khớp');
      valid = false;
    } else {
      setConfirmPasswordError('');
    }

    return valid;
  };

  const handleSubmit = async () => {
    if (!validate()) return;

    setSubmitting(true);
    try {
      const response = await changePasswordApi({ oldPassword, newPassword, confirmPassword });

      if (response.statusCode === HttpStatusCode.Ok) {
        onClose();
        setAlert({ open: true, message: 'Đổi mật khẩu thành công', severity: 'success' });
      } else if (response.statusCode === HttpStatusCode.Unauthorized) {
        logout();
      } else if (response.statusCode === HttpStatusCode.UnprocessableEntity) {
        setAlert({ open: true, message: response.data || response.message, severity: 'error' });
      } else if (response.statusCode === HttpStatusCode.BadRequest) {
        const errors = Object.entries(response.data);
        if (errors.length > 0) {
          const firstError = errors[0][1];
          setAlert({ open: true, message: firstError as string, severity: 'error' });
        }
      } else {
        setAlert({ open: true, message: 'Lỗi không xác định', severity: 'error' });
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <Dialog open={open} onClose={onClose} aria-labelledby="change-password-dialog-title" maxWidth="sm" fullWidth>
        <DialogTitle id="change-password-dialog-title">Đổi mật khẩu</DialogTitle>

        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Mật khẩu hiện tại"
            type={showOldPassword ? 'text' : 'password'}
            fullWidth
            variant="outlined"
            value={oldPassword}
            onChange={(e) => {
              setOldPassword(e.target.value);
              if (oldPasswordError) setOldPasswordError('');
            }}
            error={!!oldPasswordError}
            helperText={oldPasswordError}
            sx={{ mt: 1 }}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton onClick={() => setShowOldPassword(!showOldPassword)} edge="end" size="small">
                    {showOldPassword ? <Eye size={18} /> : <EyeSlash size={18} />}
                  </IconButton>
                </InputAdornment>
              )
            }}
          />

          <TextField
            margin="dense"
            label="Mật khẩu mới"
            type={showNewPassword ? 'text' : 'password'}
            fullWidth
            variant="outlined"
            value={newPassword}
            onChange={(e) => {
              setNewPassword(e.target.value);
              if (newPasswordError) setNewPasswordError('');
            }}
            error={!!newPasswordError}
            helperText={newPasswordError}
            sx={{ mt: 1 }}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton onClick={() => setShowNewPassword(!showNewPassword)} edge="end" size="small">
                    {showNewPassword ? <Eye size={18} /> : <EyeSlash size={18} />}
                  </IconButton>
                </InputAdornment>
              )
            }}
          />

          <TextField
            margin="dense"
            label="Xác nhận mật khẩu mới"
            type={showConfirmPassword ? 'text' : 'password'}
            fullWidth
            variant="outlined"
            value={confirmPassword}
            onChange={(e) => {
              setConfirmPassword(e.target.value);
              if (confirmPasswordError) setConfirmPasswordError('');
            }}
            error={!!confirmPasswordError}
            helperText={confirmPasswordError}
            sx={{ mt: 1 }}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton onClick={() => setShowConfirmPassword(!showConfirmPassword)} edge="end" size="small">
                    {showConfirmPassword ? <Eye size={18} /> : <EyeSlash size={18} />}
                  </IconButton>
                </InputAdornment>
              )
            }}
          />
        </DialogContent>

        <DialogActions>
          <Button variant="contained" color="primary" onClick={onClose}>
            Huỷ
          </Button>

          <Button variant="contained" color="success" onClick={handleSubmit} disabled={submitting}>
            Xác nhận
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={alert.open}
        autoHideDuration={3000}
        onClose={() => setAlert({ ...alert, open: false })}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Alert
          severity={alert.severity}
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
          {alert.message}
        </Alert>
      </Snackbar>
    </>
  );
}
