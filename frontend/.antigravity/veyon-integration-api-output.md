# Tài liệu API Tích hợp Veyon — Dành cho Frontend

## Tổng quan

Backend đã triển khai 4 endpoint phục vụ tích hợp Veyon. Tất cả đều yêu cầu role **TEACHER** và gửi kèm JWT token trong header.

```
Authorization: Bearer <jwt_token>
```

Tất cả response đều có cấu trúc chung:
```
{
  "statusCode": 200,
  "data": <object | string | null tùy endpoint>
}
```

---

## API 1 — Lấy Public Key của Server

Mỗi lần server khởi động sẽ tạo (hoặc load) một cặp RSA-2048. FE phải gọi API này để lấy public key **trước khi** import khóa Veyon.

**Endpoint:**
```
GET /api/v1/teacher/keys/public-key
```

**Response `data`:** Chuỗi Base64 chứa RSA Public Key ở định dạng X.509/SubjectPublicKeyInfo (DER).

```json
{
  "statusCode": 200,
  "data": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA..."
}
```

> **Lưu ý:** Public key thay đổi nếu server restart mà chưa cấu hình `veyon.rsa-private-key` trong `application.properties`. Nên gọi API này ngay trước khi import, không nên cache lại lâu.

---

## API 2 — Import Khóa Veyon

Teacher upload file `.pem` (Veyon private key) cho một lớp học. Vì lý do bảo mật, nội dung file `.pem` **phải được mã hóa bằng RSA** trước khi gửi lên — không được gửi plain text.

**Endpoint:**
```
POST /api/v1/teacher/keys/import
Content-Type: application/json
```

**Request Body:**
```json
{
  "classId": 5,
  "keyName": "teacher-lab-key",
  "encryptedKeyData": "<chuỗi mã hóa RSA — xem hướng dẫn bên dưới>"
}
```

| Field | Type | Bắt buộc | Mô tả |
|---|---|---|---|
| `classId` | Integer | Có | ID lớp học cần gán khóa |
| `keyName` | String | Có | Tên khóa (đặt tùy ý, ví dụ: `"lab101-key"`) |
| `encryptedKeyData` | String | Có | Nội dung file `.pem` đã mã hóa RSA (xem bên dưới) |

**Response:** `"data": null` nếu thành công.

---

### Cách mã hóa file `.pem` phía FE (JavaScript)

Thuật toán: **RSA-OAEP với SHA-256**.

Vì RSA-2048 chỉ mã hóa được tối đa ~190 bytes mỗi lần, cần chia nội dung file thành từng chunk 190 bytes, mã hóa từng chunk, rồi nối lại bằng ký tự `|`.

**Cài thư viện:**
```bash
# Sử dụng Web Crypto API có sẵn trong browser — không cần cài thêm
```

**Ví dụ code đầy đủ (TypeScript/JavaScript):**

```typescript
/**
 * Chuyển chuỗi Base64 (SubjectPublicKeyInfo) sang CryptoKey
 */
async function importPublicKey(base64Key: string): Promise<CryptoKey> {
  const binaryDer = Uint8Array.from(atob(base64Key), c => c.charCodeAt(0));
  return await crypto.subtle.importKey(
    "spki",                // định dạng SubjectPublicKeyInfo — khớp với server
    binaryDer.buffer,
    { name: "RSA-OAEP", hash: "SHA-256" },
    false,
    ["encrypt"]
  );
}

/**
 * Mã hóa chuỗi lớn bằng RSA-OAEP, tự động chia chunk 190 bytes
 * Trả về: các chunk đã mã hóa nối nhau bằng "|"
 */
async function rsaEncryptLargeString(plainText: string, publicKey: CryptoKey): Promise<string> {
  const encoder = new TextEncoder();
  const bytes = encoder.encode(plainText);
  const CHUNK_SIZE = 190;

  const encryptedChunks: string[] = [];

  for (let i = 0; i < bytes.length; i += CHUNK_SIZE) {
    const chunk = bytes.slice(i, i + CHUNK_SIZE);
    const encryptedBuffer = await crypto.subtle.encrypt(
      { name: "RSA-OAEP" },
      publicKey,
      chunk
    );
    const base64Chunk = btoa(String.fromCharCode(...new Uint8Array(encryptedBuffer)));
    encryptedChunks.push(base64Chunk);
  }

  return encryptedChunks.join("|");
}

/**
 * Toàn bộ flow import khóa Veyon
 */
async function importVeyonKey(classId: number, keyName: string, pemFileContent: string) {
  // Bước 1: Lấy public key từ server
  const publicKeyRes = await fetch("/api/v1/teacher/keys/public-key", {
    headers: { Authorization: `Bearer ${token}` }
  });
  const { data: publicKeyBase64 } = await publicKeyRes.json();

  // Bước 2: Import public key vào Web Crypto
  const publicKey = await importPublicKey(publicKeyBase64);

  // Bước 3: Mã hóa nội dung file .pem
  const encryptedKeyData = await rsaEncryptLargeString(pemFileContent, publicKey);

  // Bước 4: Gửi lên server
  await fetch("/api/v1/teacher/keys/import", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ classId, keyName, encryptedKeyData })
  });
}
```

**Cách đọc nội dung file `.pem` từ input:**
```typescript
const fileInput = document.getElementById("pem-file") as HTMLInputElement;
fileInput.addEventListener("change", async () => {
  const file = fileInput.files?.[0];
  if (!file) return;

  const pemContent = await file.text(); // đọc toàn bộ file dưới dạng string
  await importVeyonKey(classId, keyName, pemContent);
});
```

> **Lưu ý:** Sau khi import thành công, khóa được server lưu vào DB ở dạng mã hóa AES-256. FE không bao giờ thấy nội dung khóa gốc sau bước này.

---

## API 3 — Khóa / Mở màn hình

Khóa hoặc mở màn hình của toàn bộ PC trong một phiên học (Veyon chạy ở `localhost`).

**Endpoint:**
```
POST /api/class/lock-screen
Content-Type: application/json
```

**Request Body:**
```json
{
  "classId": 5,
  "studentUserId": 12,
  "active": true
}
```

| Field | Type | Mô tả |
|---|---|---|
| `classId` | Integer | ID lớp học (để lấy khóa Veyon tương ứng) |
| `studentUserId` | Integer | `userId` của sinh viên (để lấy IP máy tính) |
| `active` | Boolean | `true` = khóa màn hình, `false` = mở khóa |

**Response:** `"data": null` nếu thành công.

**Luồng xử lý bên server:**
1. Lấy `veyon_key_name` và `veyon_key` (giải mã AES) từ bảng `classes` theo `classId`
2. Gọi `POST localhost:11080/api/v1/authentication/localhost` → nhận `connection-uid`
3. Gọi `PUT localhost:11080/api/v1/feature/ccb535a2-...` với `connection-uid` và `active`

---

## API 4 — Chụp ảnh màn hình

Chụp màn hình của PC đang chạy Veyon (`localhost`).

**Endpoint:**
```
GET /api/class/screenshot?classId=5
```

| Query Param | Type | Mô tả |
|---|---|---|
| `classId` | Integer | ID lớp học (để lấy khóa Veyon) |

**Response `data`:** Chuỗi Base64 của ảnh PNG.

```json
{
  "statusCode": 200,
  "data": "iVBORw0KGgoAAAANSUhEUgAA..."
}
```

**Cách hiển thị ảnh trên FE:**
```typescript
const res = await fetch(`/api/class/screenshot?classId=${classId}`, {
  headers: { Authorization: `Bearer ${token}` }
});
const { data: base64Image } = await res.json();

// Gán vào thẻ <img>
imgElement.src = `data:image/png;base64,${base64Image}`;
```

**Lưu ý:** API này mất khoảng 2–5 giây để trả về do Veyon cần render frame. Nên hiển thị loading spinner khi đang chờ.

---

## Tóm tắt các Endpoint

| # | Method | URL | Role | Mô tả |
|---|---|---|---|---|
| 1 | GET | `/api/v1/teacher/keys/public-key` | TEACHER | Lấy RSA public key để mã hóa file .pem |
| 2 | POST | `/api/v1/teacher/keys/import` | TEACHER | Import khóa Veyon cho một lớp học |
| 3 | POST | `/api/class/lock-screen` | TEACHER | Khóa/mở màn hình |
| 4 | GET | `/api/class/screenshot` | TEACHER | Chụp ảnh màn hình (trả về Base64 PNG) |

---

## Các lỗi thường gặp

| HTTP Status | Message | Nguyên nhân |
|---|---|---|
| 400 | `Không thể giải mã dữ liệu khóa` | FE mã hóa sai thuật toán hoặc dùng public key cũ (server đã restart) |
| 400 | `Lớp học chưa được cấu hình khóa Veyon` | Chưa import khóa cho lớp này, cần gọi API 2 trước |
| 500 | `Không thể xác thực với Veyon` | Veyon không chạy ở localhost hoặc khóa `.pem` không khớp |
| 500 | `Không nhận được dữ liệu ảnh từ Veyon` | Veyon timeout khi render screenshot |
| 403 | Forbidden | Token không có role TEACHER |
