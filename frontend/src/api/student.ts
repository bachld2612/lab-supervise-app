import { PageRequest } from 'types/paging';
import { Student } from 'types/student';
import axiosServices from 'utils/axios';

const getList = async (pageRequest: PageRequest) => {
  try {
    const response = await axiosServices.get('/api/student/v1', { params: pageRequest });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getById = async (id: number) => {
  try {
    const response = await axiosServices.get(`/api/student/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const create = async (student: Student) => {
  try {
    const response = await axiosServices.post('/api/student/v1', student);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const update = async (student: Student, id: number) => {
  try {
    const response = await axiosServices.put(`/api/student/v1/${id}`, student);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const deleteById = async (id: number) => {
  try {
    const response = await axiosServices.delete(`/api/student/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const resetPassword = async (userId: number) => {
  try {
    const response = await axiosServices.post(`/api/user/v1/${userId}/reset-password`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const downloadStudentImportTemplate = async () => {
  try {
    const response = await axiosServices.get('/api/student/v1/template/download', { responseType: 'blob' });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const importStudent = async (formData: FormData) => {
  try {
    const response = await axiosServices.post(`/api/student/v1/import`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });

    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

export { getList, getById, create, update, deleteById, resetPassword, downloadStudentImportTemplate, importStudent };
