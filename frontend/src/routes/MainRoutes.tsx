import { lazy } from 'react';

// project-imports
import Loadable from 'components/Loadable';
import { SimpleLayoutType } from 'config';
import DashboardLayout from 'layout/Dashboard';
import PagesLayout from 'layout/Pages';
import SimpleLayout from 'layout/Simple';

// pages routing
const MaintenanceError = Loadable(lazy(() => import('pages/maintenance/error/404')));
const MaintenanceError500 = Loadable(lazy(() => import('pages/maintenance/error/500')));

// render - sample page
const ContactUS = Loadable(lazy(() => import('pages/contact-us')));
const UserPage = Loadable(lazy(() => import('pages/user/user-page')));
const AddUser = Loadable(lazy(() => import('sections/extra-pages/user/add')));
const EditUser = Loadable(lazy(() => import('sections/extra-pages/user/edit')));
const DetailUser = Loadable(lazy(() => import('sections/extra-pages/user/detail')));
const AdminDashboardPage = Loadable(lazy(() => import('pages/dashboard/admin-dashboard-page')));
const TeacherDashboardPage = Loadable(lazy(() => import('pages/dashboard/teacher-dashboard-page')));
const ItCenterDashboardPage = Loadable(lazy(() => import('pages/dashboard/it-center-dashboard-page')));

// ==============================|| MAIN ROUTES ||============================== //

const MainRoutes = {
  path: '/',
  children: [
    {
      path: '/',
      element: <DashboardLayout />,
      children: [
        {
          path: 'dashboard/admin',
          element: <AdminDashboardPage />
        },
        {
          path: 'dashboard/teacher',
          element: <TeacherDashboardPage />
        },
        {
          path: 'dashboard/it-center',
          element: <ItCenterDashboardPage />
        },
        {
          path: 'user',
          element: <UserPage />
        },
        {
          path: 'user/add',
          element: <AddUser />
        },
        {
          path: 'user/edit/:id',
          element: <EditUser />
        },
        {
          path: 'user/detail/:id',
          element: <DetailUser />
        }
      ]
    },
    {
      path: '/',
      element: <SimpleLayout layout={SimpleLayoutType.SIMPLE} />,
      children: [
        {
          path: 'contact-us',
          element: <ContactUS />
        }
      ]
    },
    {
      path: '/maintenance',
      element: <PagesLayout />,
      children: [
        {
          path: '404',
          element: <MaintenanceError />
        },
        {
          path: '500',
          element: <MaintenanceError500 />
        }
      ]
    },
    { path: '*', element: <MaintenanceError /> }
  ]
};

export default MainRoutes;
