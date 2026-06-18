import axiosServices from 'utils/axios';

export const lockScreen = async (classId: number, studentUserId: number, active: boolean): Promise<{ statusCode: number; data: null }> => {
  const response = await axiosServices.post('/api/remote-control/v1/class/lock-screen', { classId, studentUserId, active });
  return response.data;
};

export const openWebsiteForClass = async (classId: number, websiteUrl: string): Promise<{ statusCode: number; data: null }> => {
  const response = await axiosServices.post(`/api/remote-control/v1/class/${classId}/open-website`, { websiteUrl });
  return response.data;
};

export const openWebsiteForStudent = async (
  classId: number,
  studentId: number,
  websiteUrl: string
): Promise<{ statusCode: number; data: null }> => {
  const response = await axiosServices.post(`/api/remote-control/v1/class/${classId}/student/${studentId}/open-website`, { websiteUrl });
  return response.data;
};

export const sendMessageToClass = async (classId: number, text: string): Promise<{ statusCode: number; data: null }> => {
  const response = await axiosServices.post(`/api/remote-control/v1/class/${classId}/text-message`, { text });
  return response.data;
};

export const sendMessageToStudent = async (
  classId: number,
  studentId: number,
  text: string
): Promise<{ statusCode: number; data: null }> => {
  const response = await axiosServices.post(`/api/remote-control/v1/class/${classId}/student/${studentId}/text-message`, { text });
  return response.data;
};

export const lockScreenForExamRoom = async (
  examRoomId: number,
  studentUserId: number,
  active: boolean
): Promise<{ statusCode: number; data: null }> => {
  const response = await axiosServices.post('/api/remote-control/v1/exam-room/lock-screen', { examRoomId, studentUserId, active });
  return response.data;
};

export const openWebsiteForExamRoom = async (examRoomId: number, websiteUrl: string): Promise<{ statusCode: number; data: null }> => {
  const response = await axiosServices.post(`/api/remote-control/v1/exam-room/${examRoomId}/open-website`, { websiteUrl });
  return response.data;
};

export const openWebsiteForExamRoomStudent = async (
  examRoomId: number,
  studentId: number,
  websiteUrl: string
): Promise<{ statusCode: number; data: null }> => {
  const response = await axiosServices.post(`/api/remote-control/v1/exam-room/${examRoomId}/student/${studentId}/open-website`, {
    websiteUrl
  });
  return response.data;
};

export const sendMessageToExamRoom = async (examRoomId: number, text: string): Promise<{ statusCode: number; data: null }> => {
  const response = await axiosServices.post(`/api/remote-control/v1/exam-room/${examRoomId}/text-message`, { text });
  return response.data;
};

export const sendMessageToExamRoomStudent = async (
  examRoomId: number,
  studentId: number,
  text: string
): Promise<{ statusCode: number; data: null }> => {
  const response = await axiosServices.post(`/api/remote-control/v1/exam-room/${examRoomId}/student/${studentId}/text-message`, { text });
  return response.data;
};
