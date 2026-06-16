// assets
import { Buildings, Danger, Cpu } from 'iconsax-reactjs';

// types
import { NavItemType } from 'types/menu';

// icons
const icons = {
  room: Buildings,
  incident: Danger,
  cpu: Cpu
};

// ==============================|| MENU ITEMS - IT CENTER ||============================== //

const itCenterMenu: NavItemType = {
  id: 'group-it-center',
  title: 'Trung tâm tin học',
  type: 'group',
  children: [
    {
      id: 'manage-rooms',
      title: 'Quản lý phòng học',
      type: 'item',
      url: '/room',
      icon: icons.room
    },
    {
      id: 'incident-report',
      title: 'Quản lý báo cáo sự cố',
      type: 'item',
      url: '/incident-report',
      icon: icons.incident
    },
    {
      id: 'pc-management',
      title: 'Quản lý máy tính sinh viên',
      type: 'item',
      url: '/pc-management',
      icon: icons.cpu
    }
  ]
};

export default itCenterMenu;