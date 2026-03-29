// project-imports
import samplePage from './sample-page';
import adminMenu from './admin';
import teacherMenu from './teacher';
import itCenterMenu from './it-center';

// types
import { NavItemType } from 'types/menu';

// ==============================|| MENU ITEMS ||============================== //

const menuItems: { items: NavItemType[] } = {
  items: [adminMenu, teacherMenu, itCenterMenu, samplePage]
};

export { adminMenu, teacherMenu, itCenterMenu, samplePage };
export default menuItems;
