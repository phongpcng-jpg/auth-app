# Backend — Auth App (Spring Boot)

REST API cho hệ thống đăng nhập/đăng ký. Kiến trúc 3 lớp: `controller` → `service` (interface + `impl`) → `repository` (JPA). Bảo mật bằng JWT lưu trong cookie httpOnly (`security/`).

## Chạy local

1. Khởi động Postgres (từ thư mục gốc project):
   ```bash
   docker compose up -d postgres
   ```
2. Tạo file env:
   ```bash
   cp .env.example .env
   ```
   Điền `GOOGLE_CLIENT_ID` (xem hướng dẫn lấy Client ID ở README gốc của project). Các giá trị còn lại trong `.env.example` đã khớp với `docker-compose.yml`.
3. Chạy ứng dụng (cần cài Maven 3.9+ và JDK 21 trên máy):
   ```bash
   mvn spring-boot:run
   ```
   > Dự án chưa kèm Maven Wrapper (`mvnw`) vì môi trường soạn code không tải được file wrapper. Nếu muốn dùng `./mvnw` cho tiện, chạy `mvn -N io.takari:maven:wrapper` một lần để tự sinh.
   Backend chạy tại `http://localhost:8080`. Flyway sẽ tự tạo schema khi khởi động lần đầu.

## Build Docker image (giống môi trường Render)

```bash
docker build -t authapp-backend .
docker run --env-file .env -p 8080:8080 authapp-backend
```

## Deploy lên Render

Hướng dẫn đầy đủ, đã chốt (gói Free, domain mặc định `*.onrender.com`): xem
**[`../DEPLOY.md`](../DEPLOY.md)** ở gốc project. Tóm tắt riêng phần backend:

- Tạo **PostgreSQL** instance trên Render (gói Free), lấy connection string (Internal Database URL).
- Tạo **Web Service** (gói Free), trỏ root directory vào `backend/`, Render sẽ tự build bằng `Dockerfile`.
- Đặt **Health Check Path** = `/actuator/health` trong Advanced settings.
- Khai báo biến môi trường trong dashboard Render: `DB_URL`, `DB_USER`, `DB_PASS`, `JWT_SECRET`, `FRONTEND_URL` (URL frontend đã deploy), `COOKIE_SECURE=true`, `GOOGLE_CLIENT_ID`, `PASSKEY_RP_ID`, `PASSKEY_ORIGIN`. **Không** đặt `PORT` hay `SERVER_PORT` — Render tự inject `PORT` và app đã tự đọc biến này.
- Lưu ý: `DB_URL` cho Render Postgres có dạng `jdbc:postgresql://<host>/<db>` (Render cung cấp dạng `postgres://...`, cần đổi tiền tố thành `jdbc:postgresql://...`).
- Nhớ thêm URL frontend Render vào "Authorized JavaScript origins" của Google OAuth Client ID.

## Endpoints (Bước 3)

| Method | Path                        | Auth | Mô tả |
|--------|-----------------------------|------|-------|
| POST   | /api/auth/register          | No   | Tạo tài khoản (cuối luồng đăng ký), **nay set cookie JWT luôn** (tự động đăng nhập) |
| POST   | /api/auth/login              | No   | Đăng nhập bằng email/mật khẩu, set cookie JWT |
| POST   | /api/auth/logout             | No   | Xoá cookie |
| POST   | /api/auth/oauth2/verify-email | No  | Xác thực Google ID token, trả về email tương ứng (dùng ở bước email của wizard đăng ký) |
| POST   | /api/auth/oauth2/google       | No  | Đăng nhập bằng Google ID token; set cookie JWT |
| POST   | /api/auth/passkey/login/options | No | **Mới.** Bắt đầu ceremony đăng nhập Passkey (usernameless) — trả challenge + `requestId` |
| POST   | /api/auth/passkey/login/verify  | No | **Mới.** Xác thực kết quả từ `navigator.credentials.get()`, set cookie JWT nếu hợp lệ |
| GET    | /api/users/me                | Yes  | Thông tin người dùng hiện tại |
| PUT    | /api/users/me/password        | Yes  | Đổi mật khẩu |
| POST   | /api/passkey/register/options | Yes | **Mới.** Bắt đầu ceremony thiết lập Passkey cho user hiện tại — 409 nếu đã có passkey |
| POST   | /api/passkey/register/verify  | Yes | **Mới.** Xác thực kết quả từ `navigator.credentials.create()`, lưu credential |
| DELETE | /api/passkey                  | Yes | **Mới.** Xoá passkey hiện tại của user |

### Chi tiết cơ chế Passkey (WebAuthn)

- Thư viện: `com.yubico:webauthn-server-core` — xử lý toàn bộ sinh challenge, xác thực attestation (đăng ký) và assertion (đăng nhập), kiểm tra `origin`/`rpId`, chống replay qua signature counter.
- `security/webauthn/JpaCredentialRepository` implement interface `CredentialRepository` của thư viện, dựa trên `UserRepository` + `PasskeyCredentialRepository` có sẵn — không thêm bảng mới ngoài 2 cột nhỏ ở migration `V2`.
- "User handle" (định danh WebAuthn nội bộ, tách biệt với email) được suy ra trực tiếp từ `User.id` (UUID → 16 byte), không cần cột riêng.
- Challenge tạm thời (giữa bước "start" và "finish" của mỗi ceremony) lưu trong cache bộ nhớ (`Caffeine`, TTL 5 phút, key là `requestId` ngẫu nhiên trả về cho client) — API vẫn hoàn toàn stateless (JWT-in-cookie), đây là mảnh state ngắn hạn duy nhất và giả định 1 instance backend (đúng với Render gói Free/Starter — 1 instance; nếu sau này scale nhiều instance, thay bằng Redis).
- Quyết định sản phẩm đã chốt với người dùng: **tối đa 1 passkey/tài khoản** (ràng buộc unique ở DB), **đăng nhập kiểu usernameless/discoverable** (không cần gõ email trước).

## Ghi chú quan trọng

Sandbox soạn thảo code này **không có quyền truy cập Maven Central** (npm registry thì có — đã `npm install && ng build` frontend thành công thật, xem README gốc), nên phần backend chưa tự `mvn compile` được ở đây. Toàn bộ API của `com.yubico:webauthn-server-core` dùng trong Bước 3 đã được tra cứu trực tiếp từ Javadoc + mã nguồn chính thức (bản 2.9.0) thay vì suy đoán, nhưng bạn vẫn nên là người đầu tiên chạy `mvn spring-boot:run` thật — nếu gặp lỗi biên dịch/runtime, gửi lại log để mình sửa ngay.
