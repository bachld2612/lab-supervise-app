// hooks/useStudentDetail.ts
import { useState, useEffect, useCallback } from 'react';
import { getById } from '../api/student';
import type { StudentInfo } from '../type/student';
import { getListByStudentId } from '../api/studentLog';
import type { AppLog } from '../type/student';

const BASE_URL = 'http://localhost:8080/';

export const useStudentDetail = (studentId: number) => {
  const [student, setStudent] = useState<StudentInfo | null>(null);
  const [logs, setLogs] = useState<AppLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [reloadTrigger, setReloadTrigger] = useState(false);

  // 1. Fetch Student Info
  useEffect(() => {
    const fetchInfo = async () => {
      const res = await getById(studentId);
      if (res?.statusCode === 200) {
        setStudent(res.data);
      }
    };
    fetchInfo();
  }, [studentId]);

  // 2. Fetch Logs (Support Pagination logic here if needed)
  const fetchLogs = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getListByStudentId(studentId, { page: 0, size: 30 });
      if (res?.statusCode === 200) {
        setLogs(res.data.content);
      }
    } finally {
      setLoading(false);
    }
  }, [studentId]);

  // Trigger fetch khi reloadTrigger thay đổi (do SSE)
  useEffect(() => {
    fetchLogs();
  }, [fetchLogs, reloadTrigger]);

  // 3. SSE Connection
  useEffect(() => {
    const url = `${BASE_URL}api/sse/subscribe?userId=${studentId}`;
    const eventSource = new EventSource(url);

    // Giả sử event name là 'message' hoặc tên cụ thể server gửi
    eventSource.onmessage = (event) => {
      // Log logic xử lý event
      console.log('SSE Received:', event.data);
      // Trigger reload log list
      setReloadTrigger((prev) => !prev);
    };

    // Nếu server gửi event có tên cụ thể (named event) như ví dụ cũ của bạn
    eventSource.addEventListener('new-notification', () => {
      setReloadTrigger((prev) => !prev);
    });

    eventSource.onerror = () => {
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
  }, [studentId]);

  return { student, logs, loading };
};
