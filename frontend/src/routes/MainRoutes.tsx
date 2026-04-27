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

const TeacherPage = Loadable(lazy(() => import('pages/teacher/teacher-page')));
const AddTeacher = Loadable(lazy(() => import('sections/extra-pages/teacher/add')));
const EditTeacher = Loadable(lazy(() => import('sections/extra-pages/teacher/edit')));
const DetailTeacher = Loadable(lazy(() => import('sections/extra-pages/teacher/detail')));

const DepartmentPage = Loadable(lazy(() => import('pages/tlu-information/department-page')));
const SectionPage = Loadable(lazy(() => import('pages/tlu-information/section-page')));
const MajorPage = Loadable(lazy(() => import('pages/tlu-information/major-page')));

const SemesterPage = Loadable(lazy(() => import('pages/tlu-information/semester-page')));
const AddSemester = Loadable(lazy(() => import('sections/extra-pages/semester/add')));
const EditSemester = Loadable(lazy(() => import('sections/extra-pages/semester/edit')));
const DetailSemester = Loadable(lazy(() => import('sections/extra-pages/semester/detail')));

const SchedulePage = Loadable(lazy(() => import('pages/tlu-information/schedule-page')));
const AddSchedule = Loadable(lazy(() => import('sections/extra-pages/schedule/add')));
const EditSchedule = Loadable(lazy(() => import('sections/extra-pages/schedule/edit')));
const DetailSchedule = Loadable(lazy(() => import('sections/extra-pages/schedule/detail')));

const SubjectPage = Loadable(lazy(() => import('pages/subject/subject-page')));
const AddSubject = Loadable(lazy(() => import('sections/extra-pages/subject/add')));
const EditSubject = Loadable(lazy(() => import('sections/extra-pages/subject/edit')));
const DetailSubject = Loadable(lazy(() => import('sections/extra-pages/subject/detail')));

const ManageClassPage = Loadable(lazy(() => import('pages/manage-class/manage-class-page')));
const AddManageClass = Loadable(lazy(() => import('sections/extra-pages/manage-class/add')));
const EditManageClass = Loadable(lazy(() => import('sections/extra-pages/manage-class/edit')));
const DetailManageClass = Loadable(lazy(() => import('sections/extra-pages/manage-class/detail')));

const StudentPage = Loadable(lazy(() => import('pages/student/student-page')));
const AddStudent = Loadable(lazy(() => import('sections/extra-pages/student/add')));
const EditStudent = Loadable(lazy(() => import('sections/extra-pages/student/edit')));
const DetailStudent = Loadable(lazy(() => import('sections/extra-pages/student/detail')));

const ClassPage = Loadable(lazy(() => import('pages/class/class-page')));
const AddClass = Loadable(lazy(() => import('sections/extra-pages/class/add')));
const EditClass = Loadable(lazy(() => import('sections/extra-pages/class/edit')));
const DetailClass = Loadable(lazy(() => import('sections/extra-pages/class/detail')));

const AdminDashboardPage = Loadable(lazy(() => import('pages/dashboard/admin-dashboard-page')));
const TeacherDashboardPage = Loadable(lazy(() => import('pages/dashboard/teacher-dashboard-page')));
const ItCenterDashboardPage = Loadable(lazy(() => import('pages/dashboard/it-center-dashboard-page')));
const TeacherClassTrackingPage = Loadable(lazy(() => import('pages/teacher/teacher-class-tracking-page')));
const BanApplicationPage = Loadable(lazy(() => import('pages/teacher/ban-application-page')));

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
        },
        {
          path: 'teacher',
          element: <TeacherPage />
        },
        {
          path: 'teacher/add',
          element: <AddTeacher />
        },
        {
          path: 'teacher/edit/:id',
          element: <EditTeacher />
        },
        {
          path: 'teacher/detail/:id',
          element: <DetailTeacher />
        },
        {
          path: 'department',
          element: <DepartmentPage />
        },
        {
          path: 'section',
          element: <SectionPage />
        },
        {
          path: 'major',
          element: <MajorPage />
        },
        {
          path: 'semester',
          element: <SemesterPage />
        },
        {
          path: 'semester/add',
          element: <AddSemester />
        },
        {
          path: 'semester/edit/:id',
          element: <EditSemester />
        },
        {
          path: 'semester/detail/:id',
          element: <DetailSemester />
        },
        {
          path: 'subject',
          element: <SubjectPage />
        },
        {
          path: 'subject/add',
          element: <AddSubject />
        },
        {
          path: 'subject/edit/:id',
          element: <EditSubject />
        },
        {
          path: 'subject/detail/:id',
          element: <DetailSubject />
        },
        {
          path: 'manage-class',
          element: <ManageClassPage />
        },
        {
          path: 'manage-class/add',
          element: <AddManageClass />
        },
        {
          path: 'manage-class/edit/:id',
          element: <EditManageClass />
        },
        {
          path: 'manage-class/detail/:id',
          element: <DetailManageClass />
        },
        {
          path: 'schedule',
          element: <SchedulePage />
        },
        {
          path: 'schedule/add',
          element: <AddSchedule />
        },
        {
          path: 'schedule/edit/:id',
          element: <EditSchedule />
        },
        {
          path: 'schedule/detail/:id',
          element: <DetailSchedule />
        },
        {
          path: 'student',
          element: <StudentPage />
        },
        {
          path: 'student/add',
          element: <AddStudent />
        },
        {
          path: 'student/edit/:id',
          element: <EditStudent />
        },
        {
          path: 'student/detail/:id',
          element: <DetailStudent />
        },
        {
          path: 'class',
          element: <ClassPage />
        },
        {
          path: 'class/add',
          element: <AddClass />
        },
        {
          path: 'class/edit/:id',
          element: <EditClass />
        },
        {
          path: 'class/detail/:id',
          element: <DetailClass />
        },
        {
          path: 'teacher/class/:id/tracking',
          element: <TeacherClassTrackingPage />
        },
        {
          path: 'ban-application',
          element: <BanApplicationPage />
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
