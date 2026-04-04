import { PageRequest } from 'types/paging';
import { Teacher } from 'types/teacher';
import axiosServices from 'utils/axios';

const getList = async (pageRequest: PageRequest) => {
  try {
    const response = await axiosServices.get('/api/teacher/v1', { params: pageRequest });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getById = async (id: number) => {
  try {
    const response = await axiosServices.get(`/api/teacher/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const create = async (teacher: Teacher) => {
  try {
    const response = await axiosServices.post('/api/teacher/v1', teacher);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const update = async (teacher: Teacher, id: number) => {
  try {
    const response = await axiosServices.put(`/api/teacher/v1/${id}`, teacher);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const deleteById = async (id: number) => {
  try {
    const response = await axiosServices.delete(`/api/teacher/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const resetPassword = async (userId: number) => {
  try {
    const response = await axiosServices.post(`/api/user/v1/${userId}/reset-password`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

export { getList, getById, create, update, deleteById, resetPassword };
