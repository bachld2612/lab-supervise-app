export interface StudentInfo {
  id: number;
  fullName: string;
  hostname: string;
  username: string;
}

export interface AppLog {
  studentId: number;
  appLog: string;
  createdAt: string;
}
