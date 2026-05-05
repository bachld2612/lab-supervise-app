# Theo dõi sinh viên theo lớp học phần (REST + WebSocket)

## Tổng quan kiến trúc

Giao diện giám sát hiển thị **danh sách sinh viên** trong một lớp học phần, kèm theo **ứng dụng đang dùng trên laptop** cập nhật real-time. Hai nguồn dữ liệu tách biệt rõ ràng:

- **REST** → lấy danh sách sinh viên (thông tin cố định)
- **WebSocket** → nhận app đang dùng của từng sinh viên (real-time, thay đổi liên tục)

```
[Desktop app trên từng máy sinh viên]
    → STOMP SEND /app/pc-info  { applicationName }
    → Backend lưu DB, tìm lớp đang học
    → Broadcast tới /topic/class/{classId}

[Teacher Frontend]
    → Bước 1: REST GET /api/class/v1/{classId}/tracking   (danh sách sinh viên, không có app)
    → Bước 2: STOMP SUBSCRIBE /topic/class/{classId}       (nhận app real-time)
    → Merge: khi nhận WS message → tìm studentId, cập nhật applicationName trong state
```

---

## Bước 1 — REST: Lấy danh sách sinh viên

### Endpoint

```
GET /api/class/v1/{classId}/tracking
Authorization: Bearer <teacher_token>
Role: TEACHER
```

Trả về toàn bộ sinh viên trong lớp học phần (không phân trang). Không bao gồm thông tin app — app sẽ được cập nhật qua WebSocket.

### Response

```json
{
  "code": 200,
  "data": [
    {
      "studentId": 5,
      "fullName": "Nguyễn Văn A",
      "code": "2051060001",
      "email": "nguyenvana@tlu.edu.vn",
      "phone": "0901234567",
      "manageClassId": 3,
      "manageClassName": "CNTT-K65-01"
    },
    {
      "studentId": 8,
      "fullName": "Trần Thị B",
      "code": "2051060002",
      "email": "tranthib@tlu.edu.vn",
      "phone": "0912345678",
      "manageClassId": 3,
      "manageClassName": "CNTT-K65-01"
    }
  ]
}
```

### TypeScript types

```typescript
// Response từ REST — không có applicationName
export interface ClassStudentTrackingResponse {
  studentId: number;
  fullName: string;
  code: string;
  email: string;
  phone: string;
  manageClassId: number;
  manageClassName: string;
}

// State trong FE — FE tự gán applicationName từ WebSocket
export interface StudentTrackingState extends ClassStudentTrackingResponse {
  applicationName: string | null;
}

export const getClassStudents = async (classId: number): Promise<ClassStudentTrackingResponse[]> => {
  const res = await axios.get(`/api/class/v1/${classId}/tracking`);
  return res.data.data;
};
```

---

## Bước 2 — WebSocket: Nhận app real-time

### Thông tin kết nối

| Thông số           | Giá trị                                                         |
| ------------------ | --------------------------------------------------------------- |
| WebSocket endpoint | `http://localhost:8080/ws` (hoặc `VITE_APP_API_URL + "ws"`)     |
| Application prefix | `/app`                                                          |
| Broadcast prefix   | `/topic`                                                        |
| Auth               | JWT trong STOMP CONNECT header: `Authorization: Bearer <token>` |
| Topic theo dõi lớp | `/topic/class/{classId}`                                        |

### Thư viện cần cài

```bash
yarn add @stomp/stompjs sockjs-client
yarn add -D @types/sockjs-client
```

### Message nhận được qua WebSocket

Mỗi khi sinh viên chuyển sang app khác, FE nhận:

```typescript
export interface StudentClassInfoResponse {
  classId: number;
  studentId: number; // key để tìm và cập nhật trong state
  studentName: string;
  applicationName: string; // tên app đang mở trên máy sinh viên
  createdAt: string; // ISO 8601: "2026-04-27T10:30:00"
}
```

---

## Bước 3 — Hook kết hợp

Tạo file `src/hooks/useClassTracking.ts`:

```typescript
import { useEffect, useRef, useState } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from 'src/utils/axios';

const WS_URL = `${import.meta.env.VITE_APP_API_URL || 'http://localhost:8080/'}ws`;

export interface ClassStudentTrackingResponse {
  studentId: number;
  fullName: string;
  code: string;
  email: string;
  phone: string;
  manageClassId: number;
  manageClassName: string;
}

export interface StudentTrackingState extends ClassStudentTrackingResponse {
  applicationName: string | null;
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

  // Bước 1: Load danh sách sinh viên từ REST (không có app)
  useEffect(() => {
    if (!classId) return;
    setLoading(true);
    axios
      .get(`/api/class/v1/${classId}/tracking`)
      .then((res) => {
        const data: ClassStudentTrackingResponse[] = res.data.data ?? [];
        // Khởi tạo applicationName = null, sẽ được điền bởi WebSocket
        setStudents(data.map((s) => ({ ...s, applicationName: null })));
      })
      .finally(() => setLoading(false));
  }, [classId]);

  // Bước 2: Subscribe WebSocket để nhận app real-time
  useEffect(() => {
    if (!classId) return;

    const token = localStorage.getItem('token');
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);

        client.subscribe(`/topic/class/${classId}`, (message: IMessage) => {
          try {
            const data: StudentClassInfoResponse = JSON.parse(message.body);

            // Bước 3: Tìm sinh viên theo studentId, cập nhật applicationName
            setStudents((prev) => prev.map((s) => (s.studentId === data.studentId ? { ...s, applicationName: data.applicationName } : s)));
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
```

---

## Sử dụng trong component

```tsx
import { useClassTracking } from 'src/hooks/useClassTracking';

export default function ClassMonitorPage({ classId }: { classId: number }) {
  const { students, connected, loading } = useClassTracking(classId);

  if (loading) return <p>Đang tải...</p>;

  return (
    <div>
      <p>WebSocket: {connected ? 'Đang kết nối' : 'Mất kết nối'}</p>
      <table>
        <thead>
          <tr>
            <th>Mã SV</th>
            <th>Họ tên</th>
            <th>Lớp quản lý</th>
            <th>Ứng dụng đang dùng</th>
          </tr>
        </thead>
        <tbody>
          {students.map((s) => (
            <tr key={s.studentId}>
              <td>{s.code}</td>
              <td>{s.fullName}</td>
              <td>{s.manageClassName}</td>
              <td>{s.applicationName ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
```

---

## Authorization

| Role                  | Quyền                                                                                                          |
| --------------------- | -------------------------------------------------------------------------------------------------------------- |
| **TEACHER**           | Subscribe được `/topic/class/{classId}` nếu là giảng viên của class đó. Gọi REST `/tracking` để lấy danh sách. |
| **Student / Desktop** | Gửi tới `/app/pc-info`, không subscribe topic.                                                                 |

Subscribe vào class không phải của mình → STOMP `ERROR` frame:

```
"You are not the lecturer of this class"
```

---

## Lưu ý

- **Danh sách sinh viên** (REST) chỉ load 1 lần khi mở trang. `applicationName` bắt đầu là `null` cho mọi sinh viên.
- **App** chỉ xuất hiện khi sinh viên gửi dữ liệu qua desktop app. Backend chỉ broadcast khi ngày + giờ hiện tại khớp lịch học của lớp đó.
- **Mỗi WS message** chỉ cập nhật đúng 1 sinh viên theo `studentId` — không reload toàn bộ danh sách.
- Token teacher hết hạn sau **15 phút**. `@stomp/stompjs` tự reconnect nhưng sẽ fail nếu token hết hạn — FE cần xử lý redirect về login.
