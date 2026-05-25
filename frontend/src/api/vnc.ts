import axiosServices from 'utils/axios';

export interface VncSession {
  token: string;
  password: string;
}

export const createVncSession = (classId: number, studentUserId: number) =>
  axiosServices
    .post<{ statusCode: number; data: VncSession }>(`/api/vnc/v1/session/${classId}/${studentUserId}`)
    .then((r) => r.data);
