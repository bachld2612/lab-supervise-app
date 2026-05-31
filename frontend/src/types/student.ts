export interface Student {
  id: number;
  fullName: string;
  code: string;
  email: string;
  phone?: string;
  hometown?: string;
  status: number;
  manageClassId: number;
  manageClassName: string;
  birthday?: string;
  rawPassword?: string;
}
