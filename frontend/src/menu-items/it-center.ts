// assets
import { Buildings, Danger } from 'iconsax-reactjs';

// types
import { NavItemType } from 'types/menu';

// icons
const icons = {
  room: Buildings,
  incident: Danger
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
      title: 'Báo cáo sự cố',
      type: 'item',
      url: '/incident-report',
      icon: icons.incident
    }
  ]
};

export default itCenterMenu;