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
      title: 'Ứng dụng cấm',
      type: 'item',
      url: 'ban-application',
      icon: icons.stop
    },
    {
      id: 'personal-computer',
      title: 'Máy tính cá nhân',
      type: 'item',
      url: 'teacher/personal-computer',
      icon: icons.cpu
    }
  ]
};

export default teacherMenu;
