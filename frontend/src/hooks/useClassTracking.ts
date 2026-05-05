import { useEffect, useRef, useState } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getClassStudentTracking } from 'api/class';
import { HttpStatusCode } from 'axios';

const WS_URL = `${import.meta.env.VITE_APP_API_URL || 'http://localhost:8080/'}ws`;

export interface AppUsageEntry {
  applicationName: string;
  createdAt: string;
  banApplication: boolean;
}

export interface StudentTrackingState {
  studentId: number;
  userId: number;
  fullName: string;
  code: string;
  email: string;
  phone: string;
  manageClassId: number;
  manageClassName: string;
  appHistory: AppUsageEntry[];
}

interface StudentClassInfoResponse {
  classId: number;
  studentId: number;
  studentName: string;
  studentCode: string;
  applicationName: string;
  createdAt: string;
  banApplication: boolean;
}

interface ClassStudentTrackingResponse {
  studentId: number;
  userId: number;
  fullName: string;
  code: string;
  email: string;
  phone: string;
  manageClassId: number;
  manageClassName: string;
  applicationsToday: AppUsageEntry[];
}

export function useClassTracking(classId: number | null, onBanDetected?: (message: string) => void, reload?: boolean) {
  const [students, setStudents] = useState<StudentTrackingState[]>([]);
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(false);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!classId) return;
    setLoading(true);
    getClassStudentTracking(classId)
      .then((res) => {
        if (res.statusCode === HttpStatusCode.Ok) {
          const data: ClassStudentTrackingResponse[] = res.data ?? [];
          setStudents(
            data.map((s) => ({
              studentId: s.studentId,
              userId: s.userId,
              fullName: s.fullName,
              code: s.code,
              email: s.email,
              phone: s.phone,
              manageClassId: s.manageClassId,
              manageClassName: s.manageClassName,
              appHistory: [...s.applicationsToday].reverse()
            }))
          );
        }
      })
      .finally(() => setLoading(false));
  }, [classId, reload]);

  useEffect(() => {
    if (!classId) return;

    const token = window.localStorage.getItem('token');
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/class/${classId}`, (message: IMessage) => {
          try {
            const data: StudentClassInfoResponse = JSON.parse(message.body);
            setStudents((prev) =>
              prev.map((s) =>
                s.studentId === data.studentId
                  ? {
                      ...s,
                      appHistory: [
                        { applicationName: data.applicationName, createdAt: data.createdAt, banApplication: data.banApplication },
                        ...s.appHistory
                      ]
                    }
                  : s
              )
            );
            if (data.banApplication) {
              onBanDetected?.(
                `Sinh viên ${data.studentName} mã sinh viên ${data.studentCode} vừa truy cập ứng dụng ${data.applicationName} bị cấm`
              );
            }
          } catch {
            console.error('[WS] Failed to parse message');
          }
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: (frame) => console.error('[WS STOMP Error]', frame.headers['message']),
      onWebSocketError: () => console.error('[WS] Connection error')
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
  }, [classId]);

  return { students, connected, loading };
}
