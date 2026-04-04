import { PageRequest } from 'types/paging';
import { Section } from 'types/section';
import axiosServices from 'utils/axios';

const getList = async (pageRequest: PageRequest) => {
  try {
    const response = await axiosServices.get('/api/section/v1', { params: pageRequest });
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

const create = async (section: Section) => {
  try {
    const response = await axiosServices.post('/api/section/v1', section);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const update = async (section: Section, id: number) => {
  try {
    const response = await axiosServices.put(`/api/section/v1/${id}`, section);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const deleteById = async (id: number) => {
  try {
    const response = await axiosServices.delete(`/api/section/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

export { getList, getById, create, update, deleteById };
