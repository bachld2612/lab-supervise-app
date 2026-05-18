import { Room } from 'types/room';
import { PageRequest } from 'types/paging';
import axiosServices from 'utils/axios';

const getList = async (pageRequest: PageRequest) => {
  try {
    const response = await axiosServices.get('/api/room/v1', { params: pageRequest });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getById = async (id: number) => {
  try {
    const response = await axiosServices.get(`/api/room/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const create = async (room: Partial<Room>) => {
  try {
    const response = await axiosServices.post('/api/room/v1', room);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const update = async (room: Partial<Room>, id: number) => {
  try {
    const response = await axiosServices.put(`/api/room/v1/${id}`, room);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const deleteById = async (id: number) => {
  try {
    const response = await axiosServices.delete(`/api/room/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

export { getList, getById, create, update, deleteById };