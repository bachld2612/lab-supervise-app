import axiosServices from 'utils/axios';
import { PersonalComputerData } from 'types/personal-computer';

export const getMyPC = async (): Promise<{ code: number; data: PersonalComputerData | null }> => {
  const response = await axiosServices.get('/api/personal-computer/v1/me');
  return response.data;
};

export const updateMyPC = async (ipAddress: string): Promise<{ code: number; data: null }> => {
  const response = await axiosServices.post('/api/personal-computer/v1/update', { ipAddress });
  return response.data;
};
