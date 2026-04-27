import { useEffect, useRef, useState } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getClassStudentTracking } from 'api/class';
import { HttpStatusCode } from 'axios';

const WS_URL = `${import.meta.env.VITE_APP_API_URL || 'http://localhost:8080/'}ws`;

export interface AppUsageEntry {
  applicationName: string;
  createdAt: string;
}

export interface StudentTrackingState {
  studentId: number;
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
  applicationName: string;
  createdAt: string;
}

export function useClassTracking(classId: number | null) {
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
          const data = res.data ?? [];
          setStudents(data.map((s: Omit<StudentTrackingState, 'appHistory'>) => ({ ...s, appHistory: [] })));
        }
      })
      .finally(() => setLoading(false));
  }, [classId]);

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
                  ? { ...s, appHistory: [{ applicationName: data.applicationName, createdAt: data.createdAt }, ...s.appHistory] }
                  : s
              )
            );
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
