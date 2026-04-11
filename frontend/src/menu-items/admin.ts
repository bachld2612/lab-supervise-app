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
      title: 'Danh sách nhân viên',
      type: 'item',
      url: '/user',
      icon: icons.profile
    },
    {
      id: 'manage-users',
      title: 'Danh sách giảng viên',
      type: 'item',
      url: '/teacher',
      icon: icons.profile
    },
    {
      id: 'manage-subjects',
      title: 'Danh sách môn học',
      type: 'item',
      url: '/subject',
      icon: icons.table
    },
    {
      id: 'manage-classes',
      title: 'Danh sách lớp quản lý',
      type: 'item',
      url: '/manage-class',
      icon: icons.table
    },
    {
      id: 'department-management',
      title: 'Danh sách thông tin trường',
      type: 'collapse',
      icon: icons.box,
      children: [
        {
          id: 'manage-departments',
          title: 'Danh sách khoa',
          type: 'item',
          url: '/department',
          icon: icons.box
        },
        {
          id: 'manage-sections',
          title: 'Danh sách bộ môn',
          type: 'item',
          url: '/section',
          icon: icons.box
        },
        {
          id: 'manage-majors',
          title: 'Danh sách ngành',
          type: 'item',
          url: '/major',
          icon: icons.box
        },
        {
          id: 'manage-semesters',
          title: 'Danh sách học kỳ',
          type: 'item',
          url: '/semester',
          icon: icons.box
        },
        {
          id: 'manage-schedules',
          title: 'Danh sách ca học',
          type: 'item',
          url: '/schedule',
          icon: icons.box
        }
      ]
    }
  ]
};

export default adminMenu;
