// material-ui
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

// project-imports
import AuthWrapper from 'sections/auth/AuthWrapper';
import AuthLogin from 'sections/auth/auth-forms/AuthLogin';

// assets
import tluBadge from 'assets/images/tlu/tlu-badge.png';

// ================================|| LOGIN ||================================ //

export default function Login() {
  return (
    <AuthWrapper>
      <Grid container spacing={3}>
        <Grid sx={{ textAlign: 'center' }} size={12}>
          <Box component="img" src={tluBadge} alt="TLU" sx={{ width: 120, height: 'auto' }} />
        </Grid>
        <Grid size={12}>
          <Stack direction="row" sx={{ justifyContent: 'center', alignItems: 'center', mb: { xs: -0.5, sm: 0.5 } }}>
            <Typography variant="h3">Đăng nhập</Typography>
          </Stack>
        </Grid>
        <Grid size={12}>
          <AuthLogin forgot="/auth/forgot-password" />
        </Grid>
      </Grid>
    </AuthWrapper>
  );
}
