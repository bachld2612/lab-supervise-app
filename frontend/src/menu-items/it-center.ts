// assets
import { Setting2, Monitor, SecurityUser, Setting5, Information } from 'iconsax-reactjs';

// types
import { NavItemType } from 'types/menu';

// icons
const icons = {
  setting: Setting2,
  monitor: Monitor,
  user: SecurityUser,
  config: Setting5,
  info: Information
};

// ==============================|| MENU ITEMS - IT CENTER ||============================== //

const itCenterMenu: NavItemType = {
  id: 'group-it-center',
  title: 'Trung tâm tin học',
  type: 'group',
  children: [
    {
      id: 'system-monitor',
      title: 'Giám sát hệ thống',
      type: 'item',
      url: '/it-center/monitor',
      icon: icons.monitor
    },
    {
      id: 'system-users',
      title: 'Quản lý tài khoản',
      type: 'item',
      url: '/it-center/users',
      icon: icons.user
    },
    {
      id: 'system-config',
      title: 'Cấu hình phần cứng',
      type: 'item',
      url: '/it-center/config',
      icon: icons.config
    },
    {
      id: 'system-settings',
      title: 'Cấu hình hệ thống',
      type: 'item',
      url: '/it-center/settings',
      icon: icons.setting
    },
    {
      id: 'system-about',
      title: 'Trợ giúp & Hướng dẫn',
      type: 'item',
      url: '/it-center/about',
      icon: icons.info
    }
  ]
};

export default itCenterMenu;
