# API: Gửi file cho sinh viên

## Endpoints

| Method | URL | Mô tả |
|--------|-----|-------|
| `POST` | `/api/class/{id}/send-file` | Gửi file đến toàn bộ sinh viên trong lớp |
| `POST` | `/api/student/{id}/send-file` | Gửi file đến 1 sinh viên duy nhất |

- **Yêu cầu**: Role `TEACHER`, kèm JWT token trong header
- **`{id}`**: ID của lớp học hoặc ID của sinh viên (student entity ID)

---

## Request

### Headers

```
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

### Form Data

| Field  | Type | Bắt buộc | Mô tả               |
|--------|------|----------|---------------------|
| `file` | File | Có       | File bất kỳ cần gửi |

---

## Ví dụ — Gửi cho cả lớp

```js
const sendFileToClass = async (classId, file) => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await axiosInstance.post(
    `/api/class/${classId}/send-file`,
    formData,
    // KHÔNG set Content-Type thủ công — browser tự điền boundary cho multipart
  );

  return response.data;
};
```

---

## Ví dụ — Gửi cho 1 sinh viên

```js
const sendFileToStudent = async (studentId, file) => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await axiosInstance.post(
    `/api/student/${studentId}/send-file`,
    formData,
  );

  return response.data;
};
```

---

## Ví dụ — Component React với input file

```tsx
const FileSendButton = ({ classId }: { classId: number }) => {
  const handleChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    await sendFileToClass(classId, file);
    alert('Đã gửi file thành công!');
  };

  return <input type="file" onChange={handleChange} />;
};
```

---

## Response

### Thành công — `200 OK`

```json
{
  "statusCode": 200,
  "data": null
}
```

### Thất bại — `400 / 403 / 500`

```json
{
  "statusCode": 400,
  "message": "Không tìm thấy sinh viên có id: ..."
}
```

---

## Luồng hoạt động

```
Teacher chọn file trên web
  → POST /api/class/{id}/send-file       (gửi cả lớp)
  → POST /api/student/{id}/send-file     (gửi 1 sinh viên)
    → Backend encode file thành Base64 (in-memory, không lưu disk)
    → Gửi STOMP message tới /topic/user/{userId}/file
      → Desktop nhận, decode Base64, lưu vào ~/Downloads
      → Desktop hiển thị TrayIcon notification
```

> File **không được lưu trên server** — encode thẳng từ bộ nhớ và push qua WebSocket.  
> Giới hạn kích thước file: **50MB**.