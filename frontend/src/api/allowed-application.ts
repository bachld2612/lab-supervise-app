import { AllowedApplication } from 'types/allowed-application';
import { PageRequest } from 'types/paging';
import axiosServices from 'utils/axios';

const getList = async (examRoomId: number, pageRequest?: PageRequest & { keyword?: string }) => {
  try {
    const response = await axiosServices.get('/api/allowed-application/v1', {
      params: { examRoomId, ...pageRequest }
    });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const create = async (data: Partial<AllowedApplication>) => {
  try {
    const response = await axiosServices.post('/api/allowed-application/v1', data);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const update = async (id: number, data: Partial<AllowedApplication>) => {
  try {
    const response = await axiosServices.put(`/api/allowed-application/v1/${id}`, data);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const deleteById = async (id: number) => {
  try {
    const response = await axiosServices.delete(`/api/allowed-application/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

export { getList, create, update, deleteById };
