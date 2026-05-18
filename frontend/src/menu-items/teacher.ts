// assets
import { CalendarTick, ElementPlus, Profile, Teacher, Danger, Monitor, Stop, Cpu } from 'iconsax-reactjs';

// types
import { NavItemType } from 'types/menu';

// icons
const icons = {
  calendar: CalendarTick,
  add: ElementPlus,
  profile: Profile,
  teacher: Teacher,
  danger: Danger,
  monitor: Monitor,
  stop: Stop,
  cpu: Cpu
};

// ==============================|| MENU ITEMS - TEACHER ||============================== //

const teacherMenu: NavItemType = {
  id: 'group-teacher',
  title: 'Giảng viên',
  type: 'group',
  children: [
    {
      id: 'teacher-dashboard',
      title: 'Trang chủ',
      type: 'item',
      url: 'dashboard/teacher',
      icon: icons.monitor
    },
    {
      id: 'ban-application',
      title: 'Quản lý ứng dụng cấm',
      type: 'item',
      url: 'ban-application',
      icon: icons.stop
    },
    {
      id: 'personal-computer',
      title: 'Quản lý máy tính cá nhân',
      type: 'item',
      url: 'teacher/personal-computer',
      icon: icons.cpu
    },
    {
      id: 'teacher-incident-report',
      title: 'Báo cáo sự cố',
      type: 'item',
      url: 'incident-report/teacher',
      icon: icons.danger
    }
  ]
};

export default teacherMenu;
