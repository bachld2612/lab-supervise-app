import { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

// project-imports
import useAuth from 'hooks/useAuth';
import Loader from 'components/Loader';

// types
import { GuardProps } from 'types/auth';

// ==============================|| GUEST GUARD ||============================== //

export default function GuestGuard({ children }: GuardProps) {
  const { isLoggedIn, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const roleId = user?.roleId;
    // landing page = first sidebar item per role (dashboard chưa xây cho admin/it-center)
    const rolePath: string = roleId === 1 ? '/user' : roleId === 2 ? '/dashboard/teacher' : roleId === 4 ? '/room' : '/';

    if (isLoggedIn && user) {
      // Always land on the role's home page. Do NOT honor location.state.from:
      // after logout it holds the previous page, which would let a different
      // account jump into a page it has no permission for.
      navigate(rolePath, {
        state: { from: '' },
        replace: true
      });
    }
  }, [isLoggedIn, user, navigate, location]);

  if (isLoggedIn) {
    return <Loader />;
  }

  return children;
}
