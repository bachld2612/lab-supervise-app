import { PageRequest } from 'types/paging';
import { Schedule } from 'types/schedule';
import axiosServices from 'utils/axios';

const getList = async (pageRequest: PageRequest) => {
  try {
    const response = await axiosServices.get('/api/schedule/v1', { params: pageRequest });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getById = async (id: number) => {
  try {
    const response = await axiosServices.get(`/api/schedule/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const create = async (schedule: Schedule) => {
  try {
    const response = await axiosServices.post('/api/schedule/v1', schedule);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const update = async (schedule: Schedule, id: number) => {
  try {
    const response = await axiosServices.put(`/api/schedule/v1/${id}`, schedule);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const deleteById = async (id: number) => {
  try {
    const response = await axiosServices.delete(`/api/schedule/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

export { getList, getById, create, update, deleteById };
