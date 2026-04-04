// assets
import { Profile2User, Calendar, Box, Monitor, TableDocument } from 'iconsax-reactjs';

// types
import { NavItemType } from 'types/menu';

// icons
const icons = {
  profile: Profile2User,
  calendar: Calendar,
  box: Box,
  monitor: Monitor,
  table: TableDocument
};

// ==============================|| MENU ITEMS - ADMIN ||============================== //

const adminMenu: NavItemType = {
  id: 'group-admin',
  title: 'Văn phòng khoa',
  type: 'group',
  children: [
    {
      id: 'admin-dashboard',
      title: 'Trang chủ',
      type: 'item',
      url: '/dashboard/admin',
      icon: icons.monitor
    },
    {
      id: 'user-management',
      title: 'Quản lý nhân viên',
      type: 'item',
      url: '/user',
      icon: icons.profile
    },
    {
      id: 'manage-users',
      title: 'Quản lý giảng viên',
      type: 'item',
      url: '/teacher',
      icon: icons.profile
    },
    {
      id: 'department-management',
      title: 'Quản lý thông tin trường',
      type: 'collapse',
      icon: icons.box,
      children: [
        {
          id: 'manage-departments',
          title: 'Quản lý khoa',
          type: 'item',
          url: '/department',
          icon: icons.box
        }
      ]
    }
  ]
};

export default adminMenu;
