import type { PageRequest } from '../type/paging';
import axiosServices from '../utils/axios';

const getListByStudentId = async (id: number, pageRequest: PageRequest) => {
  try {
    const response = await axiosServices.get(`/api/student-log/v1/student/${id}`, { params: pageRequest });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

export { getListByStudentId };
