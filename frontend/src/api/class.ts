import { Classes } from 'types/classes';
import { PageRequest } from 'types/paging';
import axiosServices from 'utils/axios';

const getList = async (pageRequest: PageRequest) => {
  try {
    const response = await axiosServices.get('/api/class/v1', { params: pageRequest });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getById = async (id: number) => {
  try {
    const response = await axiosServices.get(`/api/class/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const create = async (classes: Classes) => {
  try {
    const response = await axiosServices.post('/api/class/v1', classes);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const update = async (classes: Classes, id: number) => {
  try {
    const response = await axiosServices.put(`/api/class/v1/${id}`, classes);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const deleteById = async (id: number) => {
  try {
    const response = await axiosServices.delete(`/api/class/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getTeacherClasses = async () => {
  try {
    const response = await axiosServices.get('/api/class/v1/teacher');
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getStudentsByClassId = async (classId: number, pageRequest: PageRequest) => {
  try {
    const response = await axiosServices.get(`/api/class/v1/${classId}/student`, { params: pageRequest });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getStudentsNotInClass = async (classId: number, pageRequest: PageRequest) => {
  try {
    const response = await axiosServices.get(`/api/class/v1/${classId}/student/available`, { params: pageRequest });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const addStudentsToClass = async (classId: number, studentIds: number[]) => {
  try {
    const response = await axiosServices.post(`/api/class/v1/${classId}/student`, { studentIds });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const removeStudentsFromClass = async (classId: number, studentIds: number[]) => {
  try {
    const response = await axiosServices.delete(`/api/class/v1/${classId}/student`, { data: { studentIds } });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getClassStudentTracking = async (classId: number) => {
  try {
    const response = await axiosServices.get(`/api/class/v1/${classId}/tracking`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const downloadClassStudentImportTemplate = async () => {
  try {
    const response = await axiosServices.get('/api/class/v1/template/download', { responseType: 'blob' });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const importStudentIntoClass = async (classId: number, formData: FormData) => {
  try {
    const response = await axiosServices.post(`/api/class/v1/${classId}/student/import`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const sendFileToClass = async (classId: number, formData: FormData) => {
  try {
    const response = await axiosServices.post(`/api/class/${classId}/send-file`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getClassStudyStatus = async (classId: number) => {
  try {
    const response = await axiosServices.get(`/api/class/v1/${classId}/study-status`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getConnectedStudents = async (classId: number) => {
  try {
    const response = await axiosServices.get(`/api/class/v1/${classId}/connected-students`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const sendFileToStudent = async (studentId: number, formData: FormData) => {
  try {
    const response = await axiosServices.post(`/api/student/${studentId}/send-file`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const updateWifiSsid = async (classId: number, wifiSsid: string) => {
  try {
    const response = await axiosServices.put(`/api/class/v1/${classId}/wifi-ssid`, { wifiSsid });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const generateWifiSsid = async (classId: number) => {
  try {
    const response = await axiosServices.post(`/api/class/v1/${classId}/wifi-ssid/generate`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const setTrackingEnabled = async (classId: number, enabled: boolean) => {
  try {
    const response = await axiosServices.put(`/api/class/v1/${classId}/tracking-enabled`, null, {
      params: { enabled }
    });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};
export {
  getList,
  getById,
  getClassStudyStatus,
  create,
  update,
  deleteById,
  getTeacherClasses,
  getStudentsByClassId,
  getStudentsNotInClass,
  addStudentsToClass,
  removeStudentsFromClass,
  getClassStudentTracking,
  getConnectedStudents,
  downloadClassStudentImportTemplate,
  importStudentIntoClass,
  sendFileToClass,
  sendFileToStudent,
  updateWifiSsid,
  generateWifiSsid,
  setTrackingEnabled
};
