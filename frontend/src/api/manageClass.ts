import { ManageClass } from 'types/manageClass';
import { PageRequest } from 'types/paging';
import axiosServices from 'utils/axios';

const getList = async (pageRequest: PageRequest) => {
  try {
    const response = await axiosServices.get('/api/manage-class/v1', { params: pageRequest });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getById = async (id: number) => {
  try {
    const response = await axiosServices.get(`/api/manage-class/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const create = async (manageClass: ManageClass) => {
  try {
    const response = await axiosServices.post('/api/manage-class/v1', manageClass);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const update = async (manageClass: ManageClass, id: number) => {
  try {
    const response = await axiosServices.put(`/api/manage-class/v1/${id}`, manageClass);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const deleteById = async (id: number) => {
  try {
    const response = await axiosServices.delete(`/api/manage-class/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

export { getList, getById, create, update, deleteById };
