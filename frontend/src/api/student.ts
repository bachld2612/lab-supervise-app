import axiosServices from "../utils/axios";

const getById = async (id: number) => {
  try {
    const response = await axiosServices.get(`/api/student/v1/${id}`);
    return response.data;
  } catch (error: Error | any) {
    return error;
  }
};

export { getById };
