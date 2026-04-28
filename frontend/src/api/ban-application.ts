import { isBanApplication } from 'types/ban-application';
import { PageRequest } from 'types/paging';
import axiosServices from 'utils/axios';

const getList = async (pageRequest: PageRequest) => {
  try {
    const response = await axiosServices.get('/api/ban-application/v1', { params: pageRequest });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const create = async (data: isBanApplication) => {
  try {
    const response = await axiosServices.post('/api/ban-application/v1', data);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const update = async (id: number, data: isBanApplication) => {
  try {
    const response = await axiosServices.put(`/api/ban-application/v1/${id}`, data);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const deleteById = async (id: number) => {
  try {
    const response = await axiosServices.delete(`/api/ban-application/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

export { getList, create, update, deleteById };
