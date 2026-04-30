# Ban Application Tracking — Cập nhật Backend & Hướng dẫn Frontend

## Tổng quan thay đổi

Hệ thống tracking được bổ sung khả năng phát hiện ứng dụng bị cấm theo thời gian thực. Mỗi khi desktop app gửi thông tin ứng dụng đang mở qua WebSocket, server sẽ tự động so sánh với danh sách ứng dụng bị cấm của giáo viên phụ trách lớp và đính kèm cờ `isBanApplication` vào response.

---

## Backend — Những gì đã thay đổi

### 1. Entity `StudentClassInfo` (bảng `student_class_info`)

Thêm cột mới:

```java
@Column(name = "is_ban_application", columnDefinition = "BOOLEAN DEFAULT FALSE")
boolean isBanApplication;
```

Cột này lưu trạng thái ứng dụng bị cấm tại thời điểm record được tạo. Default `false`, không cần set thủ công khi không bị cấm.

---

### 2. Repository `isBanApplicationRepository`

Thêm query JOIN thẳng từ `classId`:

```java
@Query("""
    SELECT b.applicationName FROM isBanApplication b
    JOIN Classes c ON c.teacherId = b.teacherId
    WHERE c.id = :classId AND b.status = 1
""")
List<String> findActiveAppNamesByClassId(@Param("classId") Integer classId);
```

Không cần gọi thêm bước lấy `teacherId` rồi query lại — một lần JOIN duy nhất từ `classId`.

---

### 3. Service `TrackingService.processTracking()`

Sau khi xác định được `activeClass`, service thực hiện thêm:

```java
List<String> bannedApps = isBanApplicationRepository.findActiveAppNamesByClassId(activeClass.getId());
String appName = request.getApplicationName();
boolean isBanApplication = bannedApps.stream()
    .anyMatch(banned -> appName.toLowerCase().contains(banned.toLowerCase()));
```

Kiểm tra bằng `contains` (không phân biệt hoa thường) — nếu tên ứng dụng **chứa** tên bất kỳ ứng dụng bị cấm thì `isBanApplication = true`.

Giá trị này được lưu vào `StudentClassInfo` và trả về trong response.

---

### 4. WebSocket Response `StudentClassInfoResponse`

Hai field mới được thêm:

| Field              | Type      | Mô tả                                                         |
| ------------------ | --------- | ------------------------------------------------------------- |
| `studentCode`      | `string`  | Mã sinh viên, dùng cho thông báo bị cấm                       |
| `isBanApplication` | `boolean` | `true` nếu ứng dụng nằm trong danh sách cấm của giáo viên lớp |

**Response đầy đủ (WebSocket topic `/topic/class/{classId}`):**

```json
{
  "classId": 12,
  "studentId": 5,
  "studentName": "Nguyễn Văn A",
  "studentCode": "2051060001",
  "applicationName": "Facebook",
  "createdAt": "2026-04-28T08:45:12",
  "isBanApplication": true
}
```

---

### 5. REST Tracking Response `AppUsageItem`

Field `isBanApplication` cũng được thêm vào `AppUsageItem` trả từ endpoint `GET /api/class/v1/{classId}/tracking`:

```json
{
  "applicationName": "Facebook",
  "createdAt": "2026-04-28T08:45:12",
  "isBanApplication": true
}
```

---

## Frontend — TypeScript Types

### Cập nhật `src/types/tracking.ts`

```typescript
export interface AppUsageItem {
  applicationName: string;
  createdAt: string; // ISO 8601
  isBanApplication: boolean;
}

export interface ClassStudentTrackingResponse {
  studentId: number;
  fullName: string;
  code: string;
  email: string;
  phone: string;
  manageClassId: number;
  manageClassName: string;
  applicationsToday: AppUsageItem[];
}

// WebSocket message shape
export interface StudentClassInfoResponse {
  classId: number;
  studentId: number;
  studentName: string;
  studentCode: string;
  applicationName: string;
  createdAt: string; // ISO 8601
  isBanApplication: boolean;
}
```

---

## Frontend — Hiển thị danh sách ứng dụng (REST)

Với mỗi entry trong `applicationsToday`, nếu `isBanApplication = true` thì hiển thị tên ứng dụng bằng **màu đỏ + in đậm**.

```tsx
// Ví dụ component hiển thị lịch sử ứng dụng của một sinh viên
function AppUsageList({ apps }: { apps: AppUsageItem[] }) {
  return (
    <ul>
      {apps.map((app, idx) => (
        <li key={idx}>
          <span style={app.isBanApplication ? { color: 'red', fontWeight: 'bold' } : undefined}>{app.applicationName}</span> —{' '}
          {new Date(app.createdAt).toLocaleTimeString('vi-VN')}
        </li>
      ))}
    </ul>
  );
}
```

---

## Frontend — WebSocket: Thông báo ứng dụng bị cấm

Khi nhận message từ WebSocket topic `/topic/class/{classId}`, nếu `isBanApplication = true` thì hiển thị **thông báo (toast/alert)** ở phía trên giao diện.

**Nội dung thông báo:**

> Sinh viên **{studentName}** mã sinh viên **{studentCode}** vừa truy cập ứng dụng **{applicationName}** bị cấm

### Ví dụ với `react-toastify`

```tsx
import { useEffect } from 'react';
import { Client } from '@stomp/stompjs';
import { toast } from 'react-toastify';
import { StudentClassInfoResponse } from 'src/types/tracking';

function useClassTracking(classId: number, token: string) {
  useEffect(() => {
    const client = new Client({
      brokerURL: `${import.meta.env.VITE_APP_WS_URL}/ws`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      onConnect: () => {
        client.subscribe(`/topic/class/${classId}`, (message) => {
          const data: StudentClassInfoResponse = JSON.parse(message.body);

          if (data.isBanApplication) {
            toast.error(
              `Sinh viên ${data.studentName} mã sinh viên ${data.studentCode} vừa truy cập ứng dụng ${data.applicationName} bị cấm`,
              { autoClose: 8000 }
            );
          }

          // Cập nhật state danh sách tracking nếu cần
        });
      }
    });

    client.activate();
    return () => {
      client.deactivate();
    };
  }, [classId, token]);
}
```

### Ví dụ nếu dùng alert thủ công (không có toast library)

```tsx
client.subscribe(`/topic/class/${classId}`, (message) => {
  const data: StudentClassInfoResponse = JSON.parse(message.body);

  if (data.isBanApplication) {
    // Đẩy vào state để render banner ở đầu trang
    setBanAlerts((prev) => [
      {
        id: Date.now(),
        message: `Sinh viên ${data.studentName} mã sinh viên ${data.studentCode} vừa truy cập ứng dụng ${data.applicationName} bị cấm`
      },
      ...prev
    ]);
  }
});

// Render banner
{
  banAlerts.map((alert) => (
    <div
      key={alert.id}
      style={{
        backgroundColor: '#ffe0e0',
        border: '1px solid red',
        color: 'red',
        fontWeight: 'bold',
        padding: '10px 16px',
        marginBottom: 4,
        borderRadius: 4
      }}
    >
      {alert.message}
    </div>
  ));
}
```

---

## Lưu ý

- **So sánh `contains` (không phân biệt hoa thường):** Nếu tên bị cấm là `"Facebook"`, thì `"Facebook Messenger"` cũng bị coi là vi phạm. Phù hợp để bắt các biến thể của cùng một app.
- **`isBanApplication` được tính tại thời điểm WebSocket nhận message** — nếu giáo viên thêm/xóa ứng dụng bị cấm sau đó, các record cũ không bị ảnh hưởng.
- **Danh sách ứng dụng bị cấm** được quản lý tại `GET/POST/PUT/DELETE /api/ban-application/v1` (xem `ban-application-api.md`).
- Thông báo từ WebSocket là **realtime** — nên ưu tiên dùng toast để không làm gián đoạn giao diện giáo viên.
