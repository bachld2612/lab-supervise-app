export interface Teacher {
  id: number;
  fullName: string;
  email: string;
  phone: string;
  hometown: string;
  birthday: string;
  code: string;
  sectionName?: string;
  sectionId?: number;
  rawPassword?: string;
  userId?: number;
  roleId?: number;
  roleName?: string;
  roleColor?: string;
  status?: number;
}
