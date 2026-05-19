import { ExamRoom } from 'types/exam-room';
import { PageRequest } from 'types/paging';
import axiosServices from 'utils/axios';

const getList = async (pageRequest: PageRequest & { semesterId?: number }) => {
  try {
    const response = await axiosServices.get('/api/exam-room/v1', { params: pageRequest });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getById = async (id: number) => {
  try {
    const response = await axiosServices.get(`/api/exam-room/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const create = async (examRoom: Partial<ExamRoom>) => {
  try {
    const response = await axiosServices.post('/api/exam-room/v1', examRoom);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const update = async (examRoom: Partial<ExamRoom>, id: number) => {
  try {
    const response = await axiosServices.put(`/api/exam-room/v1/${id}`, examRoom);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const deleteById = async (id: number) => {
  try {
    const response = await axiosServices.delete(`/api/exam-room/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const importStudents = async (examRoomId: number, formData: FormData) => {
  try {
    const response = await axiosServices.post(`/api/exam-room/v1/${examRoomId}/student/import`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getTracking = async (examRoomId: number) => {
  try {
    const response = await axiosServices.get(`/api/exam-room/v1/${examRoomId}/tracking`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const importVeyonKey = async (examRoomId: number, keyName: string, encryptedKeyData: string) => {
  try {
    const response = await axiosServices.post(`/api/exam-room/v1/${examRoomId}/veyon/import-key`, {
      examRoomId,
      keyName,
      encryptedKeyData
    });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getTeacherExamRooms = async () => {
  try {
    const response = await axiosServices.get('/api/exam-room/v1/teacher');
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getConnectedStudents = async (examRoomId: number) => {
  try {
    const response = await axiosServices.get(`/api/exam-room/v1/${examRoomId}/connected-students`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const getStudyStatus = async (examRoomId: number) => {
  try {
    const response = await axiosServices.get(`/api/exam-room/v1/${examRoomId}/study-status`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

const setTrackingEnabled = async (examRoomId: number, enabled: boolean) => {
  try {
    const response = await axiosServices.put(`/api/exam-room/v1/${examRoomId}/tracking-enabled`, null, {
      params: { enabled }
    });
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

export { getList, getById, create, update, deleteById, importStudents, getTracking, importVeyonKey, getTeacherExamRooms, getConnectedStudents, getStudyStatus, setTrackingEnabled };