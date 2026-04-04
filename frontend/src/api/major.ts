import { PageRequest } from 'types/paging';
import axiosServices from 'utils/axios';
import { Major } from 'types/major';

const getList = async (pageRequest: PageRequest) => {
  try {
    const response = await axiosServices.get('/api/major/v1', { params: pageRequest });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getById = async (id: number) => {
  try {
    const response = await axiosServices.get(`/api/major/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const create = async (major: Major) => {
  try {
    const response = await axiosServices.post('/api/major/v1', major);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const update = async (major: Major, id: number) => {
  try {
    const response = await axiosServices.put(`/api/major/v1/${id}`, major);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const deleteById = async (id: number) => {
  try {
    const response = await axiosServices.delete(`/api/major/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

export { getList, getById, create, update, deleteById };
