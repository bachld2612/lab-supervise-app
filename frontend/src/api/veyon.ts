import axiosServices from 'utils/axios';

export const getVeyonPublicKey = async (): Promise<{ statusCode: number; data: string }> => {
  const response = await axiosServices.get('/api/v1/teacher/keys/public-key');
  return response.data;
};

export const importVeyonKey = async (
  classId: number,
  keyName: string,
  encryptedKeyData: string
): Promise<{ statusCode: number; data: null }> => {
  const response = await axiosServices.post('/api/v1/teacher/keys/import', { classId, keyName, encryptedKeyData });
  return response.data;
};

export const lockScreen = async (classId: number, studentUserId: number, active: boolean): Promise<{ statusCode: number; data: null }> => {
  const response = await axiosServices.post('/api/class/lock-screen', { classId, studentUserId, active });
  return response.data;
};

export const getScreenshot = async (classId: number, studentUserId: number): Promise<{ statusCode: number; data: string }> => {
  const response = await axiosServices.get('/api/class/screenshot', { params: { classId, studentUserId } });
  return response.data;
};
