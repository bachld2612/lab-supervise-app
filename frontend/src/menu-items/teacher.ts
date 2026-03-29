// assets
import { CalendarTick, ElementPlus, Profile, Teacher, Danger } from 'iconsax-reactjs';

// types
import { NavItemType } from 'types/menu';

// icons
const icons = {
  calendar: CalendarTick,
  add: ElementPlus,
  profile: Profile,
  teacher: Teacher,
  danger: Danger
};

// ==============================|| MENU ITEMS - TEACHER ||============================== //

const teacherMenu: NavItemType = {
  id: 'group-teacher',
  title: 'Giảng viên',
  type: 'group',
  children: [
    {
      id: 'teacher-schedule',
      title: 'Lịch dạy giảng viên',
      type: 'item',
      url: '/teacher/schedule',
      icon: icons.calendar
    },
    {
      id: 'request-lab',
      title: 'Đăng ký mượn phòng',
      type: 'item',
      url: '/teacher/request',
      icon: icons.add
    },
    {
      id: 'my-classes',
      title: 'Danh sách lớp học',
      type: 'item',
      url: '/teacher/classes',
      icon: icons.teacher
    },
    {
      id: 'report-issue',
      title: 'Báo cáo sự cố',
      type: 'item',
      url: '/teacher/report',
      icon: icons.danger
    }
  ]
};

export default teacherMenu;
