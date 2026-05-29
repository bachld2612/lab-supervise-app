import axiosServices from 'utils/axios';

export type ScreenshotContextType = 'CLASS' | 'EXAM_ROOM';

export interface ScreenshotContextOption {
  id: number;
  label: string;
}

export interface ScreenshotStudentOption {
  studentId: number;
  fullName: string;
  code: string;
}

export interface ScreenshotHistoryItem {
  id: number;
  createdAt: string;
  studentId: number;
  studentName: string;
  studentCode: string;
  contextType: ScreenshotContextType;
  contextId: number;
  contextName: string;
  applicationName: string | null;
  imageUrl: string;
}

export interface ScreenshotHistoryPage {
  content: ScreenshotHistoryItem[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ScreenshotCaptureResponse {
  id: number;
  imageUrl: string | null;
}

export const requestClassScreenshot = async (
  classId: number,
  studentUserId: number
): Promise<{ statusCode: number; data: ScreenshotCaptureResponse }> => {
  const response = await axiosServices.post(`/api/screenshots/v1/class/${classId}/students/${studentUserId}/request`);
  return response.data;
};

export const requestExamRoomScreenshot = async (
  examRoomId: number,
  studentUserId: number
): Promise<{ statusCode: number; data: ScreenshotCaptureResponse }> => {
  const response = await axiosServices.post(`/api/screenshots/v1/exam-room/${examRoomId}/students/${studentUserId}/request`);
  return response.data;
};

export const getScreenshotContexts = async (
  contextType: ScreenshotContextType
): Promise<{ statusCode: number; data: ScreenshotContextOption[] }> => {
  const response = await axiosServices.get('/api/screenshots/v1/history/contexts', { params: { contextType } });
  return response.data;
};

export const getScreenshotStudents = async (
  contextType: ScreenshotContextType,
  contextId: number
): Promise<{ statusCode: number; data: ScreenshotStudentOption[] }> => {
  const response = await axiosServices.get('/api/screenshots/v1/history/students', { params: { contextType, contextId } });
  return response.data;
};

export const getScreenshotHistory = async (params: {
  contextType: ScreenshotContextType;
  contextId?: number;
  studentId?: number;
  date?: string;
  page: number;
  size: number;
}): Promise<{ statusCode: number; data: ScreenshotHistoryPage }> => {
  const response = await axiosServices.get('/api/screenshots/v1/history', { params });
  return response.data;
};
