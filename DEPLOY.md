# Deploy lên Render — Bước 4

Hướng dẫn này đã **chốt và sẵn sàng thực hiện** (khác với bản trước — lúc đó
còn là kế hoạch, còn vài điểm chưa xác nhận). Giả định đã áp dụng theo lựa
chọn của bạn:

- **Gói dịch vụ: Free** cho cả backend (Web Service) và database (PostgreSQL).
- **Domain: mặc định `*.onrender.com`** (không dùng custom domain).

> ⚠️ Giới hạn của gói Free cần biết trước khi bắt đầu:
> - **Web Service Free "ngủ" sau 15 phút không có request** — request đầu
>   tiên sau khi ngủ sẽ mất thêm khoảng vài chục giây để "thức dậy" (cold
>   start). Lần đăng nhập/đăng ký đầu tiên sau một thời gian không dùng sẽ
>   thấy chậm — đây là hành vi bình thường của gói Free, không phải lỗi.
> - **PostgreSQL Free hết hạn sau 90 ngày** (Render sẽ xoá database nếu
>   không nâng cấp) — cần nhớ backup hoặc nâng cấp lên gói trả phí trước mốc
>   này nếu muốn giữ dữ liệu.
> - Có thể nâng lên gói trả phí bất cứ lúc nào sau này từ dashboard, không
>   cần đổi lại code hay các bước dưới đây.

Chỉ bắt đầu bước này sau khi Bước 3 (đăng nhập/đăng ký + Passkey) đã chạy ổn
trên local và code đã được commit lên GitHub (xem `README.md` phần "Chuẩn bị
Git").

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
                                     │ Render PostgreSQL (Free)   │
                                     └──────────────────────────┘
```

## Những gì đã được sửa trong code để deploy trơn tru (mới ở lần cập nhật này)

Trước khi làm theo các bước bên dưới, đây là các thay đổi code đã thực hiện
để chuẩn bị cho Bước 4 — không cần bạn tự sửa gì thêm, chỉ cần biết để hiểu
vì sao mọi thứ hoạt động:

1. **Cổng lắng nghe (`server.port`)**: Render tự inject biến môi trường
   `PORT` (mặc định `10000`) và bắt buộc app phải lắng nghe đúng cổng đó —
   Render **không** dùng biến `SERVER_PORT` tự đặt trong `backend/.env.example`
   trước đây. Đã sửa `application.yml`:
   ```yaml
   server:
     port: ${PORT:${SERVER_PORT:8080}}
   ```
   Ưu tiên `PORT` (Render tự set) → nếu không có thì dùng `SERVER_PORT` (local,
   mặc định `8080`). **Không tự đặt `PORT` trong Render dashboard** — để Render
   tự quản lý biến này.
2. **Health check thật**: `SecurityConfig` đã cho phép truy cập công khai
   `/actuator/health` từ trước, nhưng project chưa có dependency
   `spring-boot-starter-actuator` nên endpoint đó thực ra không tồn tại (404).
   Đã thêm dependency này vào `pom.xml` — endpoint `/actuator/health` giờ trả
   về thật, dùng làm **Health Check Path** cho Web Service ở bước 2 bên dưới.
3. **SPA rewrite rule cho frontend**: ứng dụng dùng Angular Router kiểu
   HTML5 (không có `#` trong URL) với nhiều route con (`/register/email`,
   `/profile/passkey`, ...). Render Static Site mặc định sẽ trả 404 khi truy
   cập trực tiếp/refresh vào các route con này (không có file thật ở đường
   dẫn đó). Cách xử lý trên Render là khai báo 1 rule Rewrite trong dashboard
   (không phải file `_redirects` như Netlify) — xem bước 3 bên dưới.

## Thứ tự thực hiện (quan trọng — làm đúng thứ tự để tránh phải sửa lại)

### 1. Tạo Render PostgreSQL trước
- Render Dashboard → **New → PostgreSQL**.
- Chọn gói **Free**.
- Sau khi tạo xong, lấy **Internal Database URL** (dạng
  `postgres://user:pass@host/dbname`) — dùng Internal URL (không phải
  External) vì backend sẽ chạy trong cùng hạ tầng Render, nhanh hơn và không
  tính vào outbound bandwidth.
- ⚠️ Backend cần dạng `jdbc:postgresql://host/dbname` — phải tự đổi tiền tố
  `postgres://` → `jdbc:postgresql://` khi điền vào biến `DB_URL` ở bước 2
  (Render không tự chuyển đổi giúp). User/Pass tách riêng vào `DB_USER`/`DB_PASS`.

### 2. Tạo Web Service cho backend
- **New → Web Service**, connect tới repo GitHub (monorepo).
- **Root Directory**: `backend`
- **Runtime**: Docker (Render tự nhận diện `backend/Dockerfile`).
- **Instance Type**: Free.
- **Health Check Path** (Advanced section): `/actuator/health` — giúp Render
  biết chính xác khi nào deploy thật sự sẵn sàng (thay vì chỉ kiểm tra cổng
  TCP có mở hay không).
- **Environment Variables** (Settings > Environment), theo `backend/.env.example`:

  | Biến | Giá trị trên Render |
  |---|---|
  | `DB_URL` | `jdbc:postgresql://<host-từ-bước-1>/<dbname>` |
  | `DB_USER` | user Postgres từ bước 1 |
  | `DB_PASS` | password Postgres từ bước 1 |
  | `JWT_SECRET` | chuỗi ngẫu nhiên ≥32 ký tự, **khác** giá trị mặc định trong `.env.example` |
  | `JWT_EXPIRATION_MINUTES` | `120` (hoặc tuỳ chỉnh) |
  | `FRONTEND_URL` | URL frontend Render thật (điền sau khi có ở bước 3, tạm để trống hoặc localhost trước) |
  | `GOOGLE_CLIENT_ID` | Client ID Google OAuth2 (đã tạo ở Bước 2 trước đó) |
  | `COOKIE_SECURE` | `true` (bắt buộc — Render phục vụ qua HTTPS, và frontend/backend khác domain nên cookie cần `SameSite=None; Secure`) |
  | `PASSKEY_RP_ID` | domain frontend thật, **không kèm scheme/port** (vd: `authapp-frontend.onrender.com`) — điền sau khi có ở bước 3 |
  | `PASSKEY_RP_NAME` | `Auth App` (hoặc tên tuỳ chọn) |
  | `PASSKEY_ORIGIN` | URL frontend thật đầy đủ, **có scheme** (vd: `https://authapp-frontend.onrender.com`) — điền sau khi có ở bước 3 |

  ⚠️ **Không đặt biến `PORT`** — Render tự inject, và code đã đọc biến này
  tự động (xem phần "Những gì đã được sửa" ở trên). Cũng không cần đặt
  `SERVER_PORT` trên Render.

  ⚠️ Vì `PASSKEY_RP_ID`/`PASSKEY_ORIGIN` cần domain frontend thật mà domain đó
  chỉ có sau khi tạo xong Static Site ở bước 3, quy trình thực tế sẽ là:
  tạo Web Service backend trước với 2 biến này để tạm/placeholder (hoặc để
  trống) → deploy → tạo frontend (bước 3) → quay lại backend, cập nhật 2
  biến này bằng domain frontend thật → backend tự redeploy.

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

- **Redirect/Rewrite Rule** (bắt buộc — thiếu bước này thì mọi route ngoài
  `/` sẽ bị lỗi 404 khi truy cập trực tiếp hoặc refresh trang, vd:
  `/register/email`, `/menu`, `/profile/passkey`): vào tab **Redirects/Rewrites**
  của Static Site, thêm 1 rule:

  | Source | Destination | Action |
  |---|---|---|
  | `/*` | `/index.html` | Rewrite |

- Sau khi deploy xong, copy URL frontend thật.

### 4. Quay lại backend — điền nốt domain thật
- Cập nhật `FRONTEND_URL`, `PASSKEY_RP_ID`, `PASSKEY_ORIGIN` bằng URL frontend
  thật từ bước 3 → lưu → Render tự redeploy backend.

### 5. Google Cloud Console
- Vào OAuth Client ID đã tạo ở Bước 2 → **Authorized JavaScript origins** →
  thêm URL frontend Render thật (giữ nguyên `http://localhost:4200` để vẫn
  test local được).

### 6. Kiểm thử trên production
- Mở URL frontend thật, chờ backend "thức dậy" nếu lần đầu truy cập sau một
  lúc không dùng (gói Free — xem cảnh báo đầu file).
- Đăng ký tài khoản mới (luồng đầy đủ: email → xác thực Google → mật khẩu →
  thông tin cá nhân → có/không passkey).
- Refresh trực tiếp vào 1 route con (vd `/menu` sau khi đăng nhập) để xác
  nhận rewrite rule ở bước 3 hoạt động (không bị 404).
- Đăng nhập bằng mật khẩu, bằng Google, và bằng Passkey (cần thiết bị/trình
  duyệt hỗ trợ WebAuthn thật — điện thoại hoặc máy tính có vân tay/Face ID/
  Windows Hello; không dùng Chrome DevTools Virtual Authenticator được nữa
  vì đó chỉ giả lập cho `localhost`).
- Test đăng nhập bằng passkey tạo trên điện thoại, dùng trên máy tính qua
  luồng cross-device (QR code chuẩn WebAuthn).

## Vẫn chạy được local sau khi deploy

Không cần thay đổi cách chạy local — các biến `PORT`, `Health Check Path`,
rewrite rule ở trên chỉ áp dụng khi chạy trên Render:
- `backend`: `server.port` fallback về `SERVER_PORT` (mặc định `8080`) khi
  biến `PORT` không tồn tại — đúng trường hợp chạy local.
- `frontend`: `ng serve` (qua `npm start`) tự xử lý client-side routing khi
  chạy dev server, không cần rewrite rule nào.
- Tiếp tục dùng `docker compose up -d postgres` cho database local như cũ.

## Nếu sau này muốn nâng cấp

- **Đổi sang custom domain**: tạo domain trong Render dashboard cho Static
  Site → **bắt buộc** cập nhật lại `PASSKEY_RP_ID`/`PASSKEY_ORIGIN` bên
  backend sang domain mới (và mọi passkey cũ sẽ ngừng dùng được — xem cảnh
  báo ở bước 2). Cũng cần thêm domain mới vào "Authorized JavaScript origins"
  của Google OAuth Client ID.
- **Nâng lên gói trả phí**: chỉ cần đổi Instance Type trong dashboard, không
  cần sửa code.
- **Scale nhiều instance backend**: `PasskeyChallengeStore` hiện lưu challenge
  tạm thời trong bộ nhớ (Caffeine, TTL 5 phút) trong 1 instance — đủ cho gói
  Free/Starter (1 instance). Nếu sau này scale nhiều instance, cần thay bằng
  cache dùng chung (vd: Redis) để mọi instance thấy cùng 1 challenge.
