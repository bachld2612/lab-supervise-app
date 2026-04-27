# API: Danh sách lớp học của Teacher

## Endpoint

```
GET /api/class/v1/teacher
Authorization: Bearer <teacher_token>
```

**Role yêu cầu:** `TEACHER`

---

## Logic xử lý

1. Lấy `userId` từ JWT token của current user.
2. Tìm tất cả `Classes` thỏa mãn:
   - `teacher.userId = currentUserId`
   - `today BETWEEN c.startDate AND c.endDate` (lớp đang trong học kỳ)
3. Với **mỗi lớp**, kiểm tra lịch học (`Schedule`) để set `studyStatus`:
   - `studyStatus = 1` — lớp đang diễn ra ngay lúc này (đúng ngày trong tuần + giờ hiện tại nằm trong `startTime..endTime`)
   - `studyStatus = 0` — lớp không đang diễn ra
4. Sắp xếp: lớp đang diễn ra (`studyStatus = 1`) lên đầu.

> **Khác với `/v1/student`**: Student dùng `break` sau khi tìm được 1 lớp đang học (vì student chỉ học 1 lớp tại một thời điểm). Teacher kiểm tra từng lớp độc lập vì teacher có thể quản lý nhiều lớp với các lịch khác nhau.

---

## Response

**HTTP 200 OK**

```json
{
  "code": 200,
  "data": [
    {
      "id": 12,
      "name": "CNTT-01",
      "currentStudent": 35,
      "maxStudent": 40,
      "sessionNumber": 30,
      "status": 1,
      "subjectId": 3,
      "subjectName": "Lập trình Web",
      "teacherId": 7,
      "teacherName": "Nguyễn Văn A",
      "scheduleId": 2,
      "scheduleName": "Thứ 3 - Ca 1 (7:00–9:30)",
      "startDate": "2026-02-10",
      "endDate": "2026-06-15",
      "semesterId": 1,
      "semesterName": "HK2 2025-2026",
      "studyStatus": 1
    },
    {
      "id": 15,
      "name": "CNTT-02",
      "currentStudent": 28,
      "maxStudent": 40,
      "sessionNumber": 30,
      "status": 1,
      "subjectId": 3,
      "subjectName": "Lập trình Web",
      "teacherId": 7,
      "teacherName": "Nguyễn Văn A",
      "scheduleId": 5,
      "scheduleName": "Thứ 5 - Ca 3 (13:00–15:30)",
      "startDate": "2026-02-10",
      "endDate": "2026-06-15",
      "semesterId": 1,
      "semesterName": "HK2 2025-2026",
      "studyStatus": 0
    }
  ]
}
```

---

## Response fields

| Field | Type | Mô tả |
|---|---|---|
| `id` | `number` | ID của lớp học |
| `name` | `string` | Tên lớp học phần |
| `currentStudent` | `number` | Số sinh viên hiện tại trong lớp |
| `maxStudent` | `number` | Sĩ số tối đa |
| `sessionNumber` | `number` | Số buổi học trong học kỳ |
| `status` | `number` | Trạng thái lớp (1 = active) |
| `subjectId` | `number` | ID môn học |
| `subjectName` | `string` | Tên môn học |
| `teacherId` | `number` | ID giảng viên (Teacher entity) |
| `teacherName` | `string` | Họ tên giảng viên |
| `scheduleId` | `number` | ID lịch học |
| `scheduleName` | `string` | Tên lịch học |
| `startDate` | `string` | Ngày bắt đầu học kỳ (yyyy-MM-dd) |
| `endDate` | `string` | Ngày kết thúc học kỳ (yyyy-MM-dd) |
| `semesterId` | `number` | ID học kỳ |
| `semesterName` | `string` | Tên học kỳ |
| `studyStatus` | `number` | **1** = đang diễn ra, **0** = không diễn ra |

---

## Sử dụng trong React (Frontend)

### API call (`src/api/classApi.ts`)

```typescript
import axios from 'src/utils/axios';

export interface ClassItem {
  id: number;
  name: string;
  currentStudent: number;
  maxStudent: number;
  sessionNumber: number;
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
  studyStatus: number; // 1 = đang học, 0 = không học
}

export const getTeacherClasses = async (): Promise<ClassItem[]> => {
  const res = await axios.get('/api/class/v1/teacher');
  return res.data.data;
};
```

### Component ví dụ

```tsx
import { useEffect, useState } from 'react';
import { getTeacherClasses, ClassItem } from 'src/api/classApi';

export default function TeacherClassList() {
  const [classes, setClasses] = useState<ClassItem[]>([]);

  useEffect(() => {
    getTeacherClasses().then(setClasses);
  }, []);

  return (
    <div>
      {classes.map((cls) => (
        <div key={cls.id} style={{ border: cls.studyStatus === 1 ? '2px solid green' : '1px solid gray' }}>
          <h3>{cls.name}</h3>
          <p>{cls.subjectName} — {cls.scheduleName}</p>
          <p>{cls.currentStudent}/{cls.maxStudent} sinh viên</p>
          {cls.studyStatus === 1 && <span>Đang diễn ra</span>}
        </div>
      ))}
    </div>
  );
}
```

---

## Lưu ý

- Lớp trả về chỉ bao gồm các lớp **trong học kỳ hiện tại** (today nằm trong `startDate..endDate`). Lớp đã kết thúc hoặc chưa bắt đầu sẽ không xuất hiện.
- `studyStatus = 1` phụ thuộc vào thời gian thực của server. Nếu cần realtime, kết hợp với WebSocket (`/topic/class/{classId}`) sau khi biết được `classId` nào đang active.
- Token teacher hết hạn sau **15 phút** — axios interceptor sẽ trả về 401, cần handle refresh hoặc redirect về login.

---

# API: Danh sách sinh viên trong lớp học phần

## Endpoint

```
GET /api/class/v1/{classId}/student
Authorization: Bearer <teacher_token>
```

**Role yêu cầu:** `TEACHER`

---

## Query Parameters

| Param | Type | Required | Mô tả |
|---|---|---|---|
| `classId` | `number` (path) | ✅ | ID của lớp học phần (entity `Classes`) |
| `keyword` | `string` | ❌ | Tìm kiếm theo `fullName`, `email`, hoặc `phone` (không phân biệt hoa thường) |
| `page` | `number` | ❌ | Số trang, mặc định `0` |
| `size` | `number` | ❌ | Số bản ghi mỗi trang, mặc định `20` |
| `sort` | `string` | ❌ | Ví dụ: `fullName,asc` |

---

## Logic xử lý

1. Query join: `StudentClass → Student → User → ManageClass` theo `classId`.
2. Lọc keyword trên `fullName`, `email`, `phone` (LIKE, lowercase).
3. Kết quả sắp xếp theo `code` sinh viên tăng dần (mặc định), hỗ trợ override qua param `sort`.
4. Trả về Page (phân trang).

---

## Response

**HTTP 200 OK**

```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 5,
        "email": "nguyenvana@tlu.edu.vn",
        "phone": "0901234567",
        "fullName": "Nguyễn Văn A",
        "code": "2051060001",
        "manageClassId": 3,
        "manageClassName": "CNTT-K65-01",
        "hometown": "Hà Nội",
        "birthday": "2002-05-15",
        "rawPassword": "123456",
        "status": 1
      }
    ],
    "totalElements": 35,
    "totalPages": 2,
    "size": 20,
    "number": 0
  }
}
```

---

## Response fields (`content[]`)

| Field | Type | Mô tả |
|---|---|---|
| `id` | `number` | ID của sinh viên (Student entity) |
| `email` | `string` | Email sinh viên |
| `phone` | `string` | Số điện thoại |
| `fullName` | `string` | Họ và tên |
| `code` | `string` | Mã sinh viên |
| `manageClassId` | `number` | ID lớp quản lý (homeroom) |
| `manageClassName` | `string` | Tên lớp quản lý |
| `hometown` | `string` | Quê quán |
| `birthday` | `string` | Ngày sinh (yyyy-MM-dd) |
| `rawPassword` | `string` | Mật khẩu ban đầu (plain text) |
| `status` | `number` | 1 = active, 0 = inactive |

---

## Sử dụng trong React (Frontend)

### API call

```typescript
import axios from 'src/utils/axios';
import { StudentResponse } from 'src/types/student';

export interface StudentPageResponse {
  content: StudentResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const getStudentsByClassId = async (
  classId: number,
  page = 0,
  size = 20,
  keyword?: string
): Promise<StudentPageResponse> => {
  const res = await axios.get(`/api/class/v1/${classId}/student`, {
    params: { page, size, keyword },
  });
  return res.data.data;
};
```

### Component ví dụ

```tsx
import { useEffect, useState } from 'react';
import { getStudentsByClassId, StudentPageResponse } from 'src/api/classApi';

export default function ClassStudentList({ classId }: { classId: number }) {
  const [data, setData] = useState<StudentPageResponse | null>(null);
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);

  useEffect(() => {
    getStudentsByClassId(classId, page, 20, keyword || undefined).then(setData);
  }, [classId, page, keyword]);

  return (
    <div>
      <input value={keyword} onChange={(e) => { setKeyword(e.target.value); setPage(0); }} placeholder="Tìm theo tên, email, SĐT..." />
      <p>Tổng: {data?.totalElements ?? 0} sinh viên</p>
      {data?.content.map((s) => (
        <div key={s.id}>
          <strong>{s.fullName}</strong> — {s.code} — {s.manageClassName}
        </div>
      ))}
    </div>
  );
}
```

---

## Lưu ý

- `classId` là ID của **lớp học phần** (`Classes` entity), không phải lớp quản lý (`ManageClass`).
- Không filter theo `status` của sinh viên — nếu cần, thêm param `status` và mở rộng query tương ứng.
- Kết quả mặc định sắp xếp theo `code` sinh viên tăng dần.