export interface ChangePassword {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface User {
  id: number;
  email: string;
  fullName: string;
  phone: string;
  hometown: string;
  birthday: string;
  rawPassword?: string;
  roleId?: number;
  roleName?: string;
  roleColor?: string;
  status?: number;
}
