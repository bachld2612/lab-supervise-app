export interface IncidentReport {
  id: number;
  title: string;
  status: number; // 0=chờ xử lý, 1=đã xử lý, 2=từ chối
  roomId: number | null;
  roomName: string | null;
  reporterId: number;
  reporterName: string;
  reporterRole: string; // "STUDENT" | "TEACHER"
  handlerId: number | null;
  handlerName: string | null;
  createdAt: string;
}
