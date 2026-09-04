# Kế hoạch Deploy lên Render — Bước 4

Đây là **kế hoạch**, chưa thực hiện. Chỉ bắt đầu bước này sau khi Bước 3 (đăng
nhập/đăng ký + Passkey) đã chạy ổn trên local và code đã được commit lên
GitHub (xem `README.md` phần "Chuẩn bị Git").

Repo là **monorepo** (1 repo, chứa cả `backend/` và `frontend/`) — Render hỗ
trợ trỏ "Root Directory" riêng cho từng service trỏ vào 1 repo GitHub duy
nhất, nên không cần tách repo.

## Tổng quan kiến trúc sau khi deploy

```
┌─────────────────────────┐         ┌──────────────────────────┐
│ Render Static Site       │  HTTPS  │ Render Web Service         │
│ (frontend/, Angular)     │ ──────▶ │ (backend/, Spring Boot,     │
│ https://xxx.onrender.com │  REST   │  Dockerfile)                │
└─────────────────────────┘  API    │ https://yyy.onrender.com    │
                                     └──────────────┬───────────────┘
                                                     │ JDBC
                                                     ▼
                                     ┌──────────────────────────┐
                                     │ Render PostgreSQL          │
                                     └──────────────────────────┘
```

## Thứ tự thực hiện (quan trọng — làm đúng thứ tự để tránh phải sửa lại)

### 1. Tạo Render PostgreSQL trước
- Render Dashboard → **New → PostgreSQL**.
- Sau khi tạo xong, lấy **Internal Database URL** (dạng `postgres://user:pass@host/dbname`).
- ⚠️ Backend cần dạng `jdbc:postgresql://host/dbname` — phải tự đổi tiền tố
  `postgres://` → `jdbc:postgresql://` khi điền vào biến `DB_URL` ở bước 2
  (Render không tự chuyển đổi giúp). User/Pass tách riêng vào `DB_USER`/`DB_PASS`.

### 2. Tạo Web Service cho backend
- **New → Web Service**, connect tới repo GitHub (monorepo).
- **Root Directory**: `backend`
- **Runtime**: Docker (Render tự nhận diện `backend/Dockerfile`).
- **Environment Variables** (Settings > Environment), theo `backend/.env.example`:

  | Biến | Giá trị trên Render |
  |---|---|
  | `SERVER_PORT` | `8080` (hoặc để trống, Render tự set `PORT` — cân nhắc đổi `application.yml` sang `${PORT:8080}` nếu Render yêu cầu, xem ghi chú bên dưới) |
  | `DB_URL` | `jdbc:postgresql://<host-từ-bước-1>/<dbname>` |
  | `DB_USER` | user Postgres từ bước 1 |
  | `DB_PASS` | password Postgres từ bước 1 |
  | `JWT_SECRET` | chuỗi ngẫu nhiên ≥32 ký tự, **khác** giá trị mặc định trong `.env.example` |
  | `JWT_EXPIRATION_MINUTES` | `120` (hoặc tuỳ chỉnh) |
  | `FRONTEND_URL` | URL frontend Render thật (điền sau khi có ở bước 3, tạm để trống hoặc localhost trước) |
  | `GOOGLE_CLIENT_ID` | Client ID Google OAuth2 (đã tạo ở Bước 2) |
  | `COOKIE_SECURE` | `true` (bắt buộc — Render phục vụ qua HTTPS, và frontend/backend khác domain nên cookie cần `SameSite=None; Secure`) |
  | `PASSKEY_RP_ID` | domain frontend thật, **không kèm scheme/port** (vd: `authapp-frontend.onrender.com`) — điền sau khi có ở bước 3 |
  | `PASSKEY_RP_NAME` | `Auth App` (hoặc tên tuỳ chọn) |
  | `PASSKEY_ORIGIN` | URL frontend thật đầy đủ, **có scheme** (vd: `https://authapp-frontend.onrender.com`) — điền sau khi có ở bước 3 |

  ⚠️ Vì `PASSKEY_RP_ID`/`PASSKEY_ORIGIN` cần domain frontend thật mà domain đó
  chỉ có sau khi tạo xong Static Site ở bước 3, quy trình thực tế sẽ là:
  tạo Web Service backend trước với 2 biến này để tạm/placeholder → deploy →
  tạo frontend (bước 3) → quay lại backend, cập nhật 2 biến này bằng domain
  frontend thật → backend tự redeploy.

  ⚠️ **Ghi chú về passkey và việc đổi RP ID sau khi có domain thật**: bất kỳ
  passkey nào được tạo thử trong lúc `PASSKEY_RP_ID` còn là giá trị tạm sẽ
  **không dùng lại được** sau khi đổi sang domain thật (đây là giới hạn của
  chuẩn WebAuthn, không phải lỗi ứng dụng). Không cần lo vì lúc này chưa có
  user thật nào — chỉ cần biết trước để không bối rối khi thấy passkey "biến mất".

- Render tự build bằng `backend/Dockerfile` (multi-stage, không cần cấu hình build command riêng).

### 3. Tạo Static Site cho frontend
- **New → Static Site**, connect cùng repo.
- **Root Directory**: `frontend`
- **Build Command**: `npm install && npm run build`
- **Publish Directory**: `dist/frontend`
- **Environment Variables**:

  | Biến | Giá trị |
  |---|---|
  | `API_BASE_URL` | URL backend từ bước 2 + `/api` (vd: `https://authapp-backend.onrender.com/api`) |
  | `GOOGLE_CLIENT_ID` | **cùng giá trị** với `GOOGLE_CLIENT_ID` bên backend |

- Sau khi deploy xong, copy URL frontend thật.

### 4. Quay lại backend — điền nốt domain thật
- Cập nhật `FRONTEND_URL`, `PASSKEY_RP_ID`, `PASSKEY_ORIGIN` bằng URL frontend
  thật từ bước 3 → lưu → Render tự redeploy backend.

### 5. Google Cloud Console
- Vào OAuth Client ID đã tạo ở Bước 2 → **Authorized JavaScript origins** →
  thêm URL frontend Render thật (giữ nguyên `http://localhost:4200` để vẫn
  test local được).

### 6. Kiểm thử trên production
- Đăng ký tài khoản mới (luồng đầy đủ: email → xác thực Google → mật khẩu →
  thông tin cá nhân → có/không passkey).
- Đăng nhập bằng mật khẩu, bằng Google, và bằng Passkey (cần thiết bị/trình
  duyệt hỗ trợ WebAuthn thật — điện thoại hoặc máy tính có vân tay/Face ID/
  Windows Hello; không dùng Chrome DevTools Virtual Authenticator được nữa
  vì đó chỉ giả lập cho `localhost`).
- Test đăng nhập bằng passkey tạo trên điện thoại, dùng trên máy tính qua
  luồng cross-device (QR code chuẩn WebAuthn).

## Điểm cần xác nhận thêm trước khi thực hiện Bước 4 thật

Những điểm dưới đây **chưa chốt**, cần bạn xác nhận khi bắt đầu Bước 4 (không
tự quyết định):

1. **`SERVER_PORT` vs biến `PORT` của Render**: Render tự inject biến môi
   trường `PORT` và mong ứng dụng lắng nghe đúng cổng đó (khác `8080` mặc
   định). `application.yml` hiện đọc `${SERVER_PORT:8080}`. Cần xác nhận: đổi
   thành đọc cả `PORT` (Render) lẫn `SERVER_PORT` (local), hay đơn giản hơn là
   set biến `SERVER_PORT=${PORT}` thủ công trong Render dashboard? (Cách 2
   không cần sửa code nhưng Render Web Service Docker thường tự expose đúng
   cổng nếu app lắng nghe theo biến `PORT` — cần kiểm chứng thực tế lúc deploy.)
2. **Gói dịch vụ Render (Free vs Paid)**: gói Free có giới hạn (backend "ngủ"
   sau 15 phút không hoạt động, PostgreSQL Free hết hạn sau 90 ngày). Cần biết
   bạn dự định dùng gói nào để lên kế hoạch phù hợp (vd: nếu Free, cân nhắc
   thêm cảnh báo về cold-start làm chậm lần request đầu, ảnh hưởng UX passkey).
3. **Tên miền tuỳ chỉnh (custom domain)**: dùng domain `*.onrender.com` mặc
   định, hay bạn đã có domain riêng muốn trỏ vào? Ảnh hưởng trực tiếp tới giá
   trị `PASSKEY_RP_ID`/`PASSKEY_ORIGIN` — nên biết trước để tránh phải đổi lại
   (và mất passkey đã đăng ký thử) sau này.
