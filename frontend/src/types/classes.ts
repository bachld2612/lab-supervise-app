export interface Classes {
  id: number;
  name: string;
  currentStudent: number;
  maxStudent: number;
  sessionNumber?: number;
  status: number;
  subjectId: number;
  subjectName: string;
  teacherId: number;
  teacherName: string;
  scheduleId: number;
  scheduleName: string;
  startDate: string;
  endDate: string;
  semesterId: number;
  semesterName: string;
  studyStatus?: number;
  roomId?: number;
  roomName?: string;
  wifiSsid?: string;
}
