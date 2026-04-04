import { Department } from 'types/department';
import { PageRequest } from 'types/paging';
import axiosServices from 'utils/axios';

const getList = async (pageRequest: PageRequest) => {
  try {
    const response = await axiosServices.get('/api/department/v1', { params: pageRequest });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getById = async (id: number) => {
  try {
    const response = await axiosServices.get(`/api/department/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const create = async (department: Department) => {
  try {
    const response = await axiosServices.post('/api/department/v1', department);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const update = async (department: Department, id: number) => {
  try {
    const response = await axiosServices.put(`/api/department/v1/${id}`, department);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const deleteById = async (id: number) => {
  try {
    const response = await axiosServices.delete(`/api/department/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

export { getList, getById, create, update, deleteById };
