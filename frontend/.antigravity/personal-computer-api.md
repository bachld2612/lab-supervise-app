# Personal Computer API

## Tổng quan

API này cho phép người dùng (Teacher hoặc Student) **đăng ký / cập nhật IP máy tính cá nhân** của mình và **xem IP hiện tại** đang được lưu. Dữ liệu này được Veyon dùng để biết địa chỉ máy cần điều khiển.

> Yêu cầu xác thực: tất cả endpoint cần header `Authorization: Bearer <token>`. Role hợp lệ: `TEACHER`, `STUDENT`.

---

## Endpoints

### 1. Lấy thông tin máy tính của user đang đăng nhập

```
GET /api/personal-computer/v1/me
```

**Headers:**
```
Authorization: Bearer <token>
```

**Response thành công (`200`):**
```json
{
  "code": 200,
  "data": {
    "ipAddress": "192.168.1.101",
    "userId": 42
  }
}
```

**Trường hợp chưa đăng ký máy:**
```json
{
  "code": 200,
  "data": null
}
```

---

### 2. Đăng ký / cập nhật IP máy tính

```
POST /api/personal-computer/v1/update
```

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request body:**
```json
{
  "ipAddress": "192.168.1.101"
}
```

**Validation:**
- `ipAddress`: bắt buộc, không được để trống

**Response thành công (`200`):**
```json
{
  "code": 200,
  "data": null
}
```

**Response lỗi IP đã tồn tại ở thiết bị khác (`400`):**
```json
{
  "message": "Địa chỉ IP đã tồn tại ở thiết bị khác"
}
```

---

## Logic hoạt động

```
POST /v1/update
  ├── Lấy userId từ JWT (user đang đăng nhập)
  ├── Kiểm tra IP có bị trùng với máy người khác không
  ├── Tìm bản ghi PersonalComputer theo userId
  │     ├── Đã có → cập nhật ipAddress
  │     └── Chưa có → tạo mới
  └── Lưu DB
```

Endpoint này hoạt động theo kiểu **upsert**: tạo mới nếu chưa có, cập nhật nếu đã có. FE không cần phân biệt hai trường hợp.

---

## Gợi ý xây dựng giao diện

> *(Sẽ bổ sung sau khi có ảnh mẫu)*

### Flow cơ bản

1. Khi user vào trang cài đặt máy tính, FE gọi `GET /v1/me` để lấy IP hiện tại và hiển thị vào ô input.
2. User nhập / sửa IP rồi bấm lưu → FE gọi `POST /v1/update` với IP mới.
3. Hiển thị toast thành công hoặc thông báo lỗi tương ứng.

### Trạng thái cần xử lý

| Trạng thái | Hiển thị |
|---|---|
| `data: null` từ GET /me | Ô input trống, hiển thị placeholder "Chưa đăng ký" |
| `data.ipAddress` có giá trị | Điền sẵn IP vào ô input |
| POST thành công | Toast "Cập nhật thành công" |
| `"Địa chỉ IP đã tồn tại ở thiết bị khác"` | Hiển thị lỗi dưới ô input |
| `"Địa chỉ IP không được phép bỏ trống"` | Validate ngay ở FE trước khi gọi API |