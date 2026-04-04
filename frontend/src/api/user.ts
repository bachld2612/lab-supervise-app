import axiosServices from 'utils/axios';
import { PageRequest } from 'types/paging';
import { ChangePassword, User } from 'types/user';

const getList = async (pageRequest: PageRequest) => {
  try {
    const response = await axiosServices.get('/api/user/v1', { params: pageRequest });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getById = async (id: number) => {
  try {
    const response = await axiosServices.get(`/api/user/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const create = async (user: User) => {
  try {
    const response = await axiosServices.post('/api/user/v1', user);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const update = async (id: number, user: User) => {
  try {
    const response = await axiosServices.put(`/api/user/v1/${id}`, user);
    return response.data;
  } catch (error: any) {
    return error;
  }
};

const changePassword = async (changePassword: ChangePassword) => {
  try {
    const response = await axiosServices.put('/api/user/v1/change-password', changePassword);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const resetPassword = async (id: number) => {
  try {
    const response = await axiosServices.post(`/api/user/v1/${id}/reset-password`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const deleteById = async (id: number) => {
  try {
    const response = await axiosServices.delete(`/api/user/v1/${id}`);
    return response.data;
  } catch (error: any) {
    return error;
  }
};

export { getList, getById, create, update, changePassword, resetPassword, deleteById };
