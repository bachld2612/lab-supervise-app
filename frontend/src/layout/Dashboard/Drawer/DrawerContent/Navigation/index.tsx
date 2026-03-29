import { Fragment, useState } from 'react';

// material-ui
import useMediaQuery from '@mui/material/useMediaQuery';
import Divider from '@mui/material/Divider';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';

// project-imports
import NavGroup from './NavGroup';
import NavItem from './NavItem';
import { useGetMenuMaster } from 'api/menu';
import { MenuOrientation, HORIZONTAL_MAX_ITEM } from 'config';
import useConfig from 'hooks/useConfig';
import useAuth from 'hooks/useAuth';
import { adminMenu, teacherMenu, itCenterMenu, samplePage } from 'menu-items';

// types
import { NavItemType } from 'types/menu';

// ==============================|| DRAWER CONTENT - NAVIGATION ||============================== //

export default function Navigation() {
  const { user } = useAuth();
  const downLG = useMediaQuery((theme) => theme.breakpoints.down('lg'));

  const { menuOrientation } = useConfig();
  const { menuMaster } = useGetMenuMaster();
  const drawerOpen = menuMaster.isDashboardDrawerOpened;

  // Filter menu based on role
  let selectedMenu: { items: NavItemType[] } = { items: [samplePage] };

  if (user) {
    switch (user.roleId) {
      case 1:
        selectedMenu = { items: [adminMenu] };
        break;
      case 2:
        selectedMenu = { items: [teacherMenu] };
        break;
      case 4:
        selectedMenu = { items: [itCenterMenu] };
        break;
      default:
        selectedMenu = { items: [samplePage] };
        break;
    }
  }

  const [selectedID, setSelectedID] = useState<string | undefined>('');
  const [selectedItems, setSelectedItems] = useState<string | undefined>('');
  const [selectedLevel, setSelectedLevel] = useState<number>(0);

  const isHorizontal = menuOrientation === MenuOrientation.HORIZONTAL && !downLG;

  const lastItem = isHorizontal ? HORIZONTAL_MAX_ITEM : null;
  let lastItemIndex = selectedMenu.items.length - 1;
  let remItems: NavItemType[] = [];
  let lastItemId: string;

  if (lastItem && lastItem < selectedMenu.items.length) {
    lastItemId = selectedMenu.items[lastItem - 1].id!;
    lastItemIndex = lastItem - 1;
    remItems = selectedMenu.items.slice(lastItem - 1, selectedMenu.items.length).map((item) => ({
      title: item.title,
      elements: item.children,
      icon: item.icon,
      ...(item.url && {
        url: item.url
      })
    }));
  }

  const navGroups = selectedMenu.items.slice(0, lastItemIndex + 1).map((item) => {
    switch (item.type) {
      case 'group':
        if (item.url && item.id !== lastItemId) {
          return (
            <Fragment key={item.id}>
              {menuOrientation !== MenuOrientation.HORIZONTAL && <Divider sx={{ my: 0.5 }} />}
              <NavItem item={item} level={1} isParents setSelectedID={setSelectedID} />
            </Fragment>
          );
        }
        return (
          <NavGroup
            key={item.id}
            selectedID={selectedID}
            setSelectedID={setSelectedID}
            setSelectedItems={setSelectedItems}
            setSelectedLevel={setSelectedLevel}
            selectedLevel={selectedLevel}
            selectedItems={selectedItems}
            lastItem={lastItem!}
            remItems={remItems}
            lastItemId={lastItemId}
            item={item}
          />
        );
      default:
        return (
          <Typography key={item.id} variant="h6" color="error" align="center">
            Fix - Navigation Group
          </Typography>
        );
    }
  });

  return (
    <Box
      sx={{
        pt: drawerOpen ? (isHorizontal ? 0 : 2) : 0,
        '& > ul:first-of-type': { mt: 0 },
        display: isHorizontal ? { xs: 'block', lg: 'flex' } : 'block',
        alignItems: 'center'
      }}
    >
      {navGroups}
    </Box>
  );
}
