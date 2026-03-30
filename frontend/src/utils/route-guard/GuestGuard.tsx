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
    const roleName: string = roleId === 1 ? 'admin' : roleId === 2 ? 'teacher' : roleId === 4 ? 'it-center' : '';

    if (isLoggedIn && user) {
      const rolePath = `/dashboard/${roleName}`;
      navigate(location?.state?.from ? location?.state?.from : rolePath, {
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
