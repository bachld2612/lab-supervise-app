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
      title: 'Tổng quan',
      type: 'item',
      url: '/admin/dashboard',
      icon: icons.monitor
    },
    {
      id: 'manage-labs',
      title: 'Quản lý phòng TN',
      type: 'item',
      url: '/admin/labs',
      icon: icons.box
    },
    {
      id: 'manage-schedules',
      title: 'Quản lý lịch mượn',
      type: 'item',
      url: '/admin/schedules',
      icon: icons.calendar
    },
    {
      id: 'manage-users',
      title: 'Quản lý giảng viên',
      type: 'item',
      url: '/admin/teachers',
      icon: icons.profile
    },
    {
      id: 'admin-reports',
      title: 'Báo cáo thống kê',
      type: 'item',
      url: '/admin/reports',
      icon: icons.table
    }
  ]
};

export default adminMenu;
