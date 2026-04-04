import { PageRequest } from 'types/paging';
import { Semester } from 'types/semester';
import axiosServices from 'utils/axios';

const getList = async (pageRequest: PageRequest) => {
  try {
    const response = await axiosServices.get('/api/semester/v1', { params: pageRequest });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getById = async (id: number) => {
  try {
    const response = await axiosServices.get(`/api/semester/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const create = async (semester: Semester) => {
  try {
    const response = await axiosServices.post('/api/semester/v1', semester);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const update = async (semester: Semester, id: number) => {
  try {
    const response = await axiosServices.put(`/api/semester/v1/${id}`, semester);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const deleteById = async (id: number) => {
  try {
    const response = await axiosServices.delete(`/api/semester/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

export { getList, getById, create, update, deleteById };
