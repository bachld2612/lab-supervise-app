# Cập nhật API Veyon — Backend

## Tổng quan

Backend đã thay đổi cách gọi Veyon API: thay vì dùng `localhost` cố định, hệ thống giờ tự động lấy **IP máy giáo viên** và **IP máy sinh viên** từ bảng `personal_computer` trong DB theo từng request.

Thay đổi này **trong suốt** với FE ở hầu hết các endpoint, **ngoại trừ một thay đổi breaking** ở endpoint chụp màn hình.

---

## Breaking Change

### `GET /api/class/screenshot`

Endpoint này **bắt buộc thêm query param `studentUserId`**.

**Trước:**
```
GET /api/class/screenshot?classId=1
```

**Sau:**
```
GET /api/class/screenshot?classId=1&studentUserId=42
```

| Param | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `classId` | `Integer` | Có | ID lớp học |
| `studentUserId` | `Integer` | Có | `userId` của sinh viên cần chụp màn hình |

> **Lưu ý:** `studentUserId` là `user_id` trong bảng `users`, không phải `student_id` trong bảng `students`.

---

## Không thay đổi

### `POST /api/class/lock-screen`

Request body **giữ nguyên**, không cần sửa gì ở FE.

```json
{
  "classId": 1,
  "studentUserId": 42,
  "active": true
}
```

### `POST /api/v1/teacher/keys/import`
### `GET /api/v1/teacher/keys/public-key`

Hai endpoint này không thay đổi gì.

---

## Điều kiện tiên quyết để Veyon hoạt động

Để backend có thể gọi Veyon thành công, hệ thống cần đảm bảo:

1. **Giáo viên phụ trách lớp** phải có bản ghi trong bảng `personal_computer` (đã đăng ký máy tính cá nhân với IP hợp lệ).
2. **Sinh viên được truyền vào** (`studentUserId`) cũng phải có bản ghi tương ứng trong `personal_computer`.

Nếu một trong hai điều kiện trên không thỏa, backend trả về `400 Bad Request` với message mô tả cụ thể.

---

## Gợi ý xử lý lỗi ở FE

| Message từ backend | Nguyên nhân | Hiển thị cho người dùng |
|---|---|---|
| `Giáo viên chưa đăng ký máy tính cá nhân` | Teacher chưa có PC trong DB | Thông báo yêu cầu giáo viên đăng ký máy |
| `Không tìm thấy máy tính của sinh viên có userId: X` | Student chưa có PC trong DB | Thông báo sinh viên chưa được gán máy |
| `Lớp học chưa được cấu hình khóa Veyon` | Chưa import Veyon key cho lớp | Nhắc giáo viên import key trước |