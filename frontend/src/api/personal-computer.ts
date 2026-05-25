import axiosServices from 'utils/axios';
import { PersonalComputerData } from 'types/personal-computer';
import { StudentPcInfo } from 'types/student-pc-info';

export const getMyPC = async (): Promise<{ code: number; data: PersonalComputerData | null }> => {
  const response = await axiosServices.get('/api/personal-computer/v1/me');
  return response.data;
};

export const updateMyPC = async (ipAddress: string): Promise<{ code: number; data: null }> => {
  const response = await axiosServices.post('/api/personal-computer/v1/update', { ipAddress });
  return response.data;
};

export const getStudentsByClassId = async (classId: number): Promise<{ statusCode: number; data: StudentPcInfo[] }> => {
  const response = await axiosServices.get(`/api/personal-computer/v1/by-class/${classId}`);
  return response.data;
};

export const getStudentsByExamRoomId = async (examRoomId: number): Promise<{ statusCode: number; data: StudentPcInfo[] }> => {
  const response = await axiosServices.get(`/api/personal-computer/v1/by-exam-room/${examRoomId}`);
  return response.data;
};

export const updateStudentPcIp = async (userId: number, ipAddress: string): Promise<{ statusCode: number; data: null }> => {
  const response = await axiosServices.put(`/api/personal-computer/v1/student/${userId}`, { ipAddress });
  return response.data;
};
