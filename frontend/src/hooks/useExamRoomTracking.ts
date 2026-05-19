import { useEffect, useRef, useState } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getTracking, getConnectedStudents } from 'api/exam-room';
import { AllowedApplication } from 'types/allowed-application';
import { HttpStatusCode } from 'axios';

const WS_URL = `${import.meta.env.VITE_APP_API_URL || 'http://localhost:8080/'}ws`;

export interface AppUsageEntry {
  applicationName: string;
  createdAt: string;
  banApplication: boolean;
  connectionType?: string;
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
  type?: 'CONNECT' | 'DISCONNECT' | 'WHITELIST_UPDATE' | 'EXAM' | null;
  allowedApplications?: AllowedApplication[];
  examRoomId?: number;
}

interface ExamRoomTrackingResponse {
  studentId: number;
  userId: number;
  fullName: string;
  code: string;
  email: string;
  phone: string;
  manageClassId: number;
  manageClassName: string;
  applicationsToday: Array<{
    applicationName: string;
    createdAt: string;
    banApplication: boolean;
    connectionType?: string;
  }>;
}

export function useExamRoomTracking(
  examRoomId: number | null,
  onViolationDetected?: (message: string) => void,
  reload?: boolean,
  onStudentConnect?: (studentName: string, studentCode: string) => void,
  onStudentDisconnect?: (studentName: string, studentCode: string) => void,
  onWhitelistUpdate?: (apps: AllowedApplication[]) => void
) {
  const [students, setStudents] = useState<StudentTrackingState[]>([]);
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(false);
  const [connectedStudentIds, setConnectedStudentIds] = useState<Set<number>>(new Set());
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!examRoomId) return;
    setLoading(true);
    Promise.all([getTracking(examRoomId), getConnectedStudents(examRoomId)])
      .then(([trackingRes, connectedRes]) => {
        if (trackingRes.statusCode === HttpStatusCode.Ok) {
          const data: ExamRoomTrackingResponse[] = trackingRes.data ?? [];
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
              appHistory: [...s.applicationsToday].reverse().map((e) => ({
                applicationName: e.applicationName,
                createdAt: e.createdAt,
                banApplication: e.banApplication,
                connectionType: e.connectionType
              }))
            }))
          );
        }
        if (connectedRes.statusCode === HttpStatusCode.Ok) {
          const ids: number[] = connectedRes.data ?? [];
          setConnectedStudentIds(new Set(ids));
        }
      })
      .finally(() => setLoading(false));
  }, [examRoomId, reload]);

  useEffect(() => {
    if (!examRoomId) return;

    const token = window.localStorage.getItem('token');
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/exam/${examRoomId}`, (message: IMessage) => {
          try {
            const data: StudentClassInfoResponse = JSON.parse(message.body);

            // Whitelist update broadcast from backend
            if (data.type === 'WHITELIST_UPDATE' && data.allowedApplications) {
              onWhitelistUpdate?.(data.allowedApplications);
              return;
            }

            if (data.type === 'CONNECT') {
              setConnectedStudentIds((prev) => new Set(prev).add(data.studentId));
              setStudents((prev) =>
                prev.map((s) =>
                  s.studentId === data.studentId
                    ? {
                        ...s,
                        appHistory: [
                          { applicationName: '', createdAt: data.createdAt ?? new Date().toISOString(), banApplication: false, connectionType: 'CONNECT' },
                          ...s.appHistory
                        ]
                      }
                    : s
                )
              );
              onStudentConnect?.(data.studentName, data.studentCode);
              return;
            }

            if (data.type === 'DISCONNECT') {
              setConnectedStudentIds((prev) => {
                const next = new Set(prev);
                next.delete(data.studentId);
                return next;
              });
              setStudents((prev) =>
                prev.map((s) =>
                  s.studentId === data.studentId
                    ? {
                        ...s,
                        appHistory: [
                          { applicationName: '', createdAt: data.createdAt ?? new Date().toISOString(), banApplication: false, connectionType: 'DISCONNECT' },
                          ...s.appHistory
                        ]
                      }
                    : s
                )
              );
              onStudentDisconnect?.(data.studentName, data.studentCode);
              return;
            }

            setConnectedStudentIds((prev) => {
              if (prev.has(data.studentId)) return prev;
              return new Set(prev).add(data.studentId);
            });
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
              onViolationDetected?.(
                `Sinh viên ${data.studentName} mã ${data.studentCode} vừa mở ứng dụng không được phép: ${data.applicationName}`
              );
            }
          } catch {
            console.error('[WS] Failed to parse exam room message');
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
  }, [examRoomId]);

  return { students, connected, loading, connectedStudentIds };
}