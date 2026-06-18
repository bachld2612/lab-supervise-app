import { PageRequest } from 'types/paging';
import axiosServices from 'utils/axios';

const getListForItCenter = async (params: PageRequest & { roomId?: number | string }) => {
  try {
    const response = await axiosServices.get('/api/incident-report/v1', { params });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getListForTeacher = async (params: PageRequest & { roomId?: number | string }) => {
  try {
    const response = await axiosServices.get('/api/incident-report/v1/teacher', { params });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const createForTeacher = async (data: { title: string; roomId: number }) => {
  try {
    const response = await axiosServices.post('/api/incident-report/v1/teacher', data);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const updateForTeacher = async (id: number, data: { title: string; roomId: number }) => {
  try {
    const response = await axiosServices.put(`/api/incident-report/v1/teacher/${id}`, data);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const resolve = async (id: number) => {
  try {
    const response = await axiosServices.put(`/api/incident-report/v1/${id}/resolve`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const reject = async (id: number) => {
  try {
    const response = await axiosServices.put(`/api/incident-report/v1/${id}/reject`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

export { getListForItCenter, getListForTeacher, createForTeacher, updateForTeacher, resolve, reject };
