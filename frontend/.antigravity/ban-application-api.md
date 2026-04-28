# API: Quản lý ứng dụng cấm (Ban Application)

## Tổng quan

Teacher quản lý danh sách ứng dụng bị cấm trong lớp học của mình. Mỗi teacher chỉ thấy và thao tác được với ban list của chính mình.

**Base URL:** `/api/ban-application`  
**Role yêu cầu:** `TEACHER` (tất cả endpoint)

---

## TypeScript Types

```typescript
export interface isBanApplicationResponse {
  id: number;
  teacherId: number;
  applicationName: string;
  imageUrl: string | null;
  status: number; // 1 = active
}

export interface isBanApplicationCreateRequest {
  applicationName: string; // required
  imageUrl?: string; // optional
}

export interface isBanApplicationUpdateRequest {
  applicationName?: string;
  imageUrl?: string;
}
```

---

## API call functions

```typescript
import axios from 'src/utils/axios';

export const getisBanApplicationList = async (page = 0, size = 20, keyword?: string): Promise<PageResponse<isBanApplicationResponse>> => {
  const res = await axios.get('/api/ban-application/v1', {
    params: { page, size, keyword }
  });
  return res.data.data;
};

export const getisBanApplicationById = async (id: number): Promise<isBanApplicationResponse> => {
  const res = await axios.get(`/api/ban-application/v1/${id}`);
  return res.data.data;
};

export const createisBanApplication = async (data: isBanApplicationCreateRequest): Promise<void> => {
  await axios.post('/api/ban-application/v1', data);
};

export const updateisBanApplication = async (id: number, data: isBanApplicationUpdateRequest): Promise<void> => {
  await axios.put(`/api/ban-application/v1/${id}`, data);
};

export const deleteisBanApplication = async (id: number): Promise<void> => {
  await axios.delete(`/api/ban-application/v1/${id}`);
};
```

> `PageResponse<T>` — xem type chung ở cuối file.

---

## Endpoints chi tiết

---

### GET /api/ban-application/v1 — Danh sách

**Query params:**

| Param     | Type     | Required | Mô tả                                                   |
| --------- | -------- | -------- | ------------------------------------------------------- |
| `keyword` | `string` | ❌       | Tìm theo `applicationName` (không phân biệt hoa thường) |
| `page`    | `number` | ❌       | Số trang, mặc định `0`                                  |
| `size`    | `number` | ❌       | Số bản ghi mỗi trang, mặc định `20`                     |

**Response 200:**

```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "teacherId": 3,
        "applicationName": "Facebook",
        "imageUrl": "https://example.com/facebook.png",
        "status": 1
      },
      {
        "id": 2,
        "teacherId": 3,
        "applicationName": "Zalo",
        "imageUrl": null,
        "status": 1
      }
    ],
    "totalElements": 2,
    "totalPages": 1,
    "size": 20,
    "number": 0
  }
}
```

> Chỉ trả về ứng dụng của teacher đang đăng nhập, sắp xếp theo `applicationName` A-Z.

---

### GET /api/ban-application/v1/{id} — Chi tiết

**Response 200:**

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "teacherId": 3,
    "applicationName": "Facebook",
    "imageUrl": "https://example.com/facebook.png",
    "status": 1
  }
}
```

> Trả về lỗi nếu `id` không thuộc về teacher hiện tại.

---

### POST /api/ban-application/v1 — Tạo mới

**Request body:**

```json
{
  "applicationName": "TikTok",
  "imageUrl": "https://example.com/tiktok.png"
}
```

| Field             | Type     | Required | Mô tả                         |
| ----------------- | -------- | -------- | ----------------------------- |
| `applicationName` | `string` | ✅       | Tên ứng dụng cần cấm          |
| `imageUrl`        | `string` | ❌       | URL ảnh đại diện của ứng dụng |

**Response 200:**

```json
{
  "code": 200,
  "data": null
}
```

---

### PUT /api/ban-application/v1/{id} — Cập nhật

**Request body** (tất cả field đều optional — chỉ gửi field cần sửa):

```json
{
  "applicationName": "TikTok Updated",
  "imageUrl": "https://example.com/new-icon.png"
}
```

**Response 200:**

```json
{
  "code": 200,
  "data": null
}
```

> Trả về `403 Forbidden` nếu cố sửa ứng dụng không thuộc về mình.

---

### DELETE /api/ban-application/v1/{id} — Xoá

Soft delete — chỉ đánh dấu `status = 0`, không xoá khỏi DB.

**Response 200:**

```json
{
  "code": 200,
  "data": null
}
```

> Trả về `403 Forbidden` nếu cố xoá ứng dụng không thuộc về mình.

---

## Type chung

```typescript
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // current page index (0-based)
}
```

---

## Lưu ý

- `teacherId` không cần truyền từ FE — backend tự lấy từ JWT token của người đang đăng nhập.
- Teacher chỉ xem/sửa/xoá được ban list của chính mình. Cố tình thao tác trên record của người khác → `403`.
- `imageUrl` có thể `null` — nên dùng ảnh placeholder khi hiển thị.
- Kết quả list luôn là `status = 1` (active). Record bị xoá không xuất hiện trong danh sách.
