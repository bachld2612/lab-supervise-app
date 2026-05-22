export interface ExamRoom {
  id: number;
  code: string;
  roomId: number;
  roomName: string;
  teacher1Id: number;
  teacher1Name: string;
  teacher2Id: number;
  teacher2Name: string;
  subjectId: number;
  subjectName: string;
  semesterId: number;
  semesterName: string;
  maxStudent: number;
  currentStudent: number;
  examDate: string;
  startTime: string;
  endTime: string;
  status: number;
  trackingEnabled?: boolean;
  studyStatus?: number;
  wifiSsid?: string;
}