import { PageRequest } from 'types/paging';
import axiosServices from 'utils/axios';
import { Subject } from 'types/subject';

const getList = async (pageRequest: PageRequest) => {
  try {
    const response = await axiosServices.get('/api/subject/v1', { params: pageRequest });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getById = async (id: number) => {
  try {
    const response = await axiosServices.get(`/api/subject/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const create = async (subject: Subject) => {
  try {
    const response = await axiosServices.post('/api/subject/v1', subject);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const update = async (subject: Subject, id: number) => {
  try {
    const response = await axiosServices.put(`/api/subject/v1/${id}`, subject);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const deleteById = async (id: number) => {
  try {
    const response = await axiosServices.delete(`/api/subject/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

export { getList, getById, create, update, deleteById };
