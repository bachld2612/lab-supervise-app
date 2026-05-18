// material-ui
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';

// project-imports
import NavUser from './NavUser';
import Navigation from './Navigation';
import SimpleBar from 'components/third-party/SimpleBar';
import useAuth from 'hooks/useAuth';
import { useGetMenuMaster } from 'api/menu';

// assets
import { Call } from 'iconsax-reactjs';

// ==============================|| DRAWER CONTENT ||============================== //

export default function DrawerContent() {
  const { user } = useAuth();
  const { menuMaster } = useGetMenuMaster();
  const drawerOpen = menuMaster.isDashboardDrawerOpened;

  // Show hotline for teacher (roleId=2) and student (roleId=3)
  const showHotline = user?.roleId !== 1 && user?.roleId !== 4;

  return (
    <>
      <SimpleBar sx={{ '& .simplebar-content': { display: 'flex', flexDirection: 'column' } }}>
        <Navigation />
      </SimpleBar>
      {showHotline && drawerOpen && (
        <Box sx={{ px: 3, pb: 1.5, pt: 0.5 }}>
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 1.5,
              py: 1.5,
              px: 2,
              borderRadius: '12px',
              border: '1px solid',
              borderColor: 'divider',
              bgcolor: 'background.paper',
              boxShadow: '0 1px 4px rgba(0,0,0,0.04)'
            }}
          >
            <Call size={20} variant="Bold" />
            <Typography variant="subtitle2" sx={{ fontWeight: 600, whiteSpace: 'nowrap' }}>
              Hotline TTTH: 024.3563.8072
            </Typography>
          </Box>
        </Box>
      )}
      <NavUser />
    </>
  );
}
