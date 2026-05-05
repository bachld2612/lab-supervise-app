# Thông báo kết nối / ngắt kết nối sinh viên qua WebSocket

## Tổng quan

Backend bổ sung 2 loại message mới gửi qua STOMP topic `/topic/class/{classId}` khi sinh viên kết nối hoặc ngắt kết nối ứng dụng desktop trong giờ học. Giáo viên đang xem trang tracking sẽ nhận được thông báo realtime.

---

## 1. Backend thay đổi gì

### `StudentClassInfoResponse` — thêm field `type`

```java
// Các giá trị có thể có:
// null        → tracking bình thường (không đổi gì ở FE hiện tại)
// "CONNECT"   → sinh viên vừa kết nối
// "DISCONNECT" → sinh viên vừa ngắt kết nối
String type;
```

### `WebSocketEventListener`

- Lắng nghe `SessionConnectedEvent` → gửi `type: "CONNECT"`
- Lắng nghe `SessionDisconnectEvent` → gửi `type: "DISCONNECT"`
- Chỉ xử lý user có role `STUDENT`, bỏ qua teacher/admin
- Chỉ gửi notification nếu sinh viên đang trong giờ học (tìm lớp active theo lịch hiện tại)
- Gửi đến `/topic/class/{classId}` — giáo viên subscribe topic này nhận được ngay

---

## 2. Payload nhận được ở FE

```ts
// Tracking bình thường (type = null hoặc undefined)
{
  classId: 12,
  studentId: 5,
  studentName: "Nguyễn Văn A",
  studentCode: "2251172245",
  applicationName: "Chrome",
  createdAt: "2026-05-06T10:30:00",
  banApplication: false,
  type: null
}

// Sinh viên vừa kết nối
{
  classId: 12,
  studentId: 5,
  studentName: "Nguyễn Văn A",
  studentCode: "2251172245",
  applicationName: null,
  createdAt: "2026-05-06T10:30:00",
  banApplication: false,
  type: "CONNECT"
}

// Sinh viên vừa ngắt kết nối
{
  classId: 12,
  studentId: 5,
  studentName: "Nguyễn Văn A",
  studentCode: "2251172245",
  applicationName: null,
  createdAt: "2026-05-06T10:30:00",
  banApplication: false,
  type: "DISCONNECT"
}
```

---

## 3. Thay đổi cần làm ở Frontend

### 3.1 Cập nhật interface trong `useClassTracking.ts`

Thêm `type` vào `StudentClassInfoResponse`:

```ts
interface StudentClassInfoResponse {
  classId: number;
  studentId: number;
  studentName: string;
  studentCode: string;
  applicationName: string;
  createdAt: string;
  banApplication: boolean;
  type?: 'CONNECT' | 'DISCONNECT' | null;  // thêm dòng này
}
```

Thêm `connectedStudentIds` vào state để track sinh viên đang online:

```ts
const [connectedStudentIds, setConnectedStudentIds] = useState<Set<number>>(new Set());
```

Return thêm `connectedStudentIds` từ hook:

```ts
return { students, connected, loading, connectedStudentIds };
```

### 3.2 Cập nhật callback `onConnect` / `onDisconnect`

Bổ sung 2 callback tùy chọn vào hook signature:

```ts
export function useClassTracking(
  classId: number | null,
  onBanDetected?: (message: string) => void,
  reload?: boolean,
  onStudentConnect?: (studentName: string, studentCode: string) => void,    // thêm
  onStudentDisconnect?: (studentName: string, studentCode: string) => void  // thêm
)
```

### 3.3 Cập nhật handler xử lý STOMP message

Trong `client.subscribe(...)`, phân nhánh theo `data.type`:

```ts
client.subscribe(`/topic/class/${classId}`, (message: IMessage) => {
  try {
    const data: StudentClassInfoResponse = JSON.parse(message.body);

    // --- CONNECT ---
    if (data.type === 'CONNECT') {
      setConnectedStudentIds((prev) => new Set(prev).add(data.studentId));
      onStudentConnect?.(data.studentName, data.studentCode);
      return;
    }

    // --- DISCONNECT ---
    if (data.type === 'DISCONNECT') {
      setConnectedStudentIds((prev) => {
        const next = new Set(prev);
        next.delete(data.studentId);
        return next;
      });
      onStudentDisconnect?.(data.studentName, data.studentCode);
      return;
    }

    // --- TRACKING bình thường (type = null) ---
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
```

---

## 4. Áp dụng vào `teacher-class-tracking-page.tsx`

### 4.1 Gọi hook với callback mới

```tsx
const { students, loading, connectedStudentIds } = useClassTracking(
  classId,
  (message) => {
    setAlert({ open: true, message, severity: 'error' });
  },
  reload,
  // onStudentConnect
  (studentName, studentCode) => {
    setAlert({
      open: true,
      message: `Sinh viên ${studentName} (${studentCode}) vừa kết nối`,
      severity: 'success'
    });
  },
  // onStudentDisconnect
  (studentName, studentCode) => {
    setAlert({
      open: true,
      message: `Sinh viên ${studentName} (${studentCode}) vừa ngắt kết nối`,
      severity: 'warning'
    });
  }
);
```

### 4.2 Hiển thị trạng thái online trên student card (tuỳ chọn)

Dùng `connectedStudentIds` để hiển thị badge/dot trên card:

```tsx
const isOnline = connectedStudentIds.has(student.studentId);

// Trong CardContent, thêm indicator:
<Stack direction="row" alignItems="center" justifyContent="space-between">
  <Typography variant="h6" fontWeight="bold">
    {student.fullName}
  </Typography>
  <Box
    sx={{
      width: 8,
      height: 8,
      borderRadius: '50%',
      bgcolor: isOnline ? 'success.main' : 'text.disabled'
    }}
  />
</Stack>
```

---

## 5. Lưu ý

- `connectedStudentIds` ban đầu là Set rỗng. Nếu sinh viên đã kết nối trước khi giáo viên mở trang tracking, giáo viên sẽ không thấy dot "online" cho đến khi sinh viên gửi message tracking tiếp theo hoặc reconnect. Đây là giới hạn của approach event-driven — không có trạng thái persistent.
- Nếu cần biết chính xác ai đang online khi mở trang, cần thêm một REST API riêng để query danh sách session đang active (phức tạp hơn, chưa implement).
- Backend chỉ gửi CONNECT/DISCONNECT khi sinh viên đang trong giờ học (có lớp active). Nếu kết nối ngoài giờ học, sẽ không có notification.