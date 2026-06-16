import React, { createContext, useCallback, useEffect, useReducer } from 'react';

// third-party
import { Chance } from 'chance';

// reducer - state management
import { LOGIN, LOGOUT } from 'contexts/auth-reducer/actions';
import authReducer from 'contexts/auth-reducer/auth';

// project-imports
import Loader from 'components/Loader';
import axios, { refreshAccessToken } from 'utils/axios';
import { setAccessToken, clearAccessToken } from 'utils/authToken';
import { openSnackbar } from 'api/snackbar';

// types
import { AuthProps, JWTContextType } from 'types/auth';

const chance = new Chance();

// constant
const initialState: AuthProps = {
  isLoggedIn: false,
  isInitialized: false,
  user: null
};

// Access token is kept in-memory only; the refresh token lives in an HttpOnly cookie.
const setSession = (token?: string | null) => {
  if (token) {
    setAccessToken(token);
  } else {
    clearAccessToken();
  }
};

// ==============================|| JWT CONTEXT & PROVIDER ||============================== //

const JWTContext = createContext<JWTContextType | null>(null);

export const JWTProvider = ({ children }: { children: React.ReactElement }) => {
  const [state, dispatch] = useReducer(authReducer, initialState);

  useEffect(() => {
    const init = async () => {
      try {
        // No persisted access token: bootstrap the session from the refresh cookie.
        const token = await refreshAccessToken();
        if (token) {
          const response = await axios.get('/api/user/v1/profile');
          const userProfile = response.data.data || response.data;
          dispatch({
            type: LOGIN,
            payload: {
              isLoggedIn: true,
              user: {
                ...userProfile,
                name: userProfile.fullName
              }
            }
          });
        } else {
          dispatch({
            type: LOGOUT
          });
        }
      } catch (err) {
        console.error(err);
        dispatch({
          type: LOGOUT
        });
      }
    };

    init();
  }, []);

  const login = async (email: string, password: string) => {
    const response = await axios.post('/api/auth/v1/login', { email, password, device: 'web' });
    const { statusCode, data, message } = response.data;

    if (statusCode === 200) {
      const { token, user } = data;
      setSession(token);
      dispatch({
        type: LOGIN,
        payload: {
          isLoggedIn: true,
          user: {
            ...user,
            name: user.fullName
          }
        }
      });
    } else if (statusCode === 422) {
      openSnackbar({
        open: true,
        message: message || 'Tài khoản hoặc mật khẩu không chính xác',
        variant: 'alert',
        alert: {
          color: 'error',
          variant: 'filled'
        },
        close: false,
        anchorOrigin: {
          vertical: 'bottom',
          horizontal: 'right'
        },
        transition: 'Fade',
        actionButton: false,
        dense: false,
        maxStack: 3,
        iconVariant: 'usedefault',
        action: false
      });
    }
  };

  const register = async (email: string, password: string, firstName: string, lastName: string) => {
    // todo: this flow need to be recode as it not verified
    const id = chance.bb_pin();
    const response = await axios.post('/api/account/register', {
      id,
      email,
      password,
      firstName,
      lastName
    });
    let users = response.data;

    if (window.localStorage.getItem('users') !== undefined && window.localStorage.getItem('users') !== null) {
      const localUsers = window.localStorage.getItem('users');
      users = [
        ...JSON.parse(localUsers!),
        {
          id,
          email,
          password,
          name: `${firstName} ${lastName}`
        }
      ];
    }

    window.localStorage.setItem('users', JSON.stringify(users));
  };

  const logout = useCallback(async () => {
    try {
      // Revoke server-side: delete refresh token from DB + blacklist access token, clear cookie.
      await axios.post('/api/auth/v1/logout');
    } catch (err) {
      console.error(err);
    }
    setSession(null);
    dispatch({ type: LOGOUT });
  }, []);

  useEffect(() => {
    const interceptor = axios.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response && error.response.status === 401 && !window.location.href.includes('/login')) {
          logout();
        }
        return Promise.reject(error);
      }
    );

    return () => {
      axios.interceptors.response.eject(interceptor);
    };
  }, [logout]);

  const resetPassword = async (email: string) => {
    console.log('email - ', email);
  };

  const updateProfile = () => {};

  if (state.isInitialized !== undefined && !state.isInitialized) {
    return <Loader />;
  }

  return <JWTContext value={{ ...state, login, logout, register, resetPassword, updateProfile }}>{children}</JWTContext>;
};

export default JWTContext;
