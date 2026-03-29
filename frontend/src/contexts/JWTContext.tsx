import React, { createContext, useCallback, useEffect, useReducer } from 'react';

// third-party
import { Chance } from 'chance';
import { jwtDecode } from 'jwt-decode';

// reducer - state management
import { LOGIN, LOGOUT } from 'contexts/auth-reducer/actions';
import authReducer from 'contexts/auth-reducer/auth';

// project-imports
import Loader from 'components/Loader';
import axios from 'utils/axios';
import { openSnackbar } from 'api/snackbar';

// types
import { AuthProps, JWTContextType } from 'types/auth';
import { KeyedObject } from 'types/root';

const chance = new Chance();

// constant
const initialState: AuthProps = {
  isLoggedIn: false,
  isInitialized: false,
  user: null
};

const verifyToken: (st: string) => boolean = (token) => {
  if (!token) {
    return false;
  }
  const decoded: KeyedObject = jwtDecode(token);
  /**
   * Property 'exp' does not exist on type '<T = unknown>(token: string, options?: JwtDecodeOptions | undefined) => T'.
   */
  return decoded.exp > Date.now() / 1000;
};

const setSession = (token?: string | null) => {
  if (token) {
    localStorage.setItem('token', token);
    axios.defaults.headers.common.Authorization = `Bearer ${token}`;
  } else {
    localStorage.removeItem('token');
    delete axios.defaults.headers.common.Authorization;
  }
};

// ==============================|| JWT CONTEXT & PROVIDER ||============================== //

const JWTContext = createContext<JWTContextType | null>(null);

export const JWTProvider = ({ children }: { children: React.ReactElement }) => {
  const [state, dispatch] = useReducer(authReducer, initialState);

  useEffect(() => {
    const init = async () => {
      try {
        const token = window.localStorage.getItem('token');
        if (token && verifyToken(token)) {
          setSession(token);
          const response = await axios.get('/api/account/me');
          const user = response.data.data?.user || response.data.user;
          dispatch({
            type: LOGIN,
            payload: {
              isLoggedIn: true,
              user
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
    const response = await axios.post('/api/auth/v1/login', { email, password });
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

  const logout = useCallback(() => {
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
