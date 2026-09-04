# Auth App — Bước 3

Hệ thống đăng nhập/đăng ký gồm 2 package độc lập:
- `backend/` — Spring Boot 3 (Java 21), REST API, JWT trong httpOnly cookie, PostgreSQL
- `frontend/` — Angular (bản hiện đại, standalone components), theme màu tách riêng ở `frontend/src/app/shared/styles/theme.css`

Phạm vi Bước 3 (đã thống nhất, bổ sung so với Bước 2):
- **Đăng nhập bằng Passkey (WebAuthn): hoạt động thật**, kiểu **usernameless/discoverable** — bấm nút "Đăng nhập với Passkey" trên `/login`, không cần gõ email, trình duyệt tự hiện danh sách passkey đã lưu cho site này.
- **Mỗi tài khoản tối đa 1 passkey** (theo quyết định đã chốt) — vẫn dùng được passkey lưu trên điện thoại để đăng nhập trên máy tính, qua cơ chế quét mã QR chuẩn của WebAuthn (cross-device / "hybrid" transport), không cần cài gì thêm.
- **Đăng ký**: sau bước hỏi "Có dùng Passkey?" → **tài khoản được tạo trước** (và tự động đăng nhập luôn, không cần quay lại `/login` như Bước 1/2 nữa) → nếu chọn "Có", chuyển sang trang thiết lập Passkey **thật** (gọi WebAuthn thật, không còn placeholder).
- **Menu**: mục "Quản lý Passkey" đã được bật. Trang `/profile/passkey`: chưa có passkey → nút "Thêm" (chuyển sang ceremony thiết lập thật) / "Không" (quay về menu, ghi console); đã có → nút "Xóa" (gọi API thật, luôn quay về menu dù thành công hay thất bại, ghi console).
- Backend dùng thư viện chuẩn **`com.yubico:webauthn-server-core`** (Yubico) để xử lý toàn bộ nghiệp vụ mật mã học của WebAuthn (sinh challenge, xác thực attestation/assertion, chống replay qua signature counter) — không tự viết lại phần mật mã.

## Cấu hình Google OAuth2 Client ID (bắt buộc để chạy được — không đổi so với Bước 2)

Xem lại phần này trong README Bước 2 nếu chưa cấu hình: [Google Cloud Console](https://console.cloud.google.com/apis/credentials) → tạo OAuth client ID (Web application) → điền Client ID vào `backend/.env` (`GOOGLE_CLIENT_ID`) và `frontend/src/environments/environment.ts` (`googleClientId`).

## Cấu hình Passkey (mới ở Bước 3)

Đã có sẵn giá trị mặc định hợp lý cho local trong `backend/.env.example`, **không cần chỉnh gì thêm để chạy local**:

```
PASSKEY_RP_ID=localhost
PASSKEY_RP_NAME=Auth App
PASSKEY_ORIGIN=http://localhost:4200
```

- `PASSKEY_RP_ID` ("Relying Party ID"): phải là domain (không kèm scheme/port) của **frontend**. `localhost` chạy được vì trình duyệt coi `http://localhost` là "secure context" — đây là lý do Bước 3 test trên máy tính local được ngay mà không cần HTTPS.
- `PASSKEY_ORIGIN`: phải khớp **chính xác** scheme+host+port mà trình duyệt gửi lên (`http://localhost:4200`).
- ⚠️ **Quan trọng cho Bước 4 (deploy Render)**: một passkey được tạo dưới một `PASSKEY_RP_ID` sẽ **không** dùng để đăng nhập được nếu đổi sang `PASSKEY_RP_ID` khác. Khi deploy, đổi 2 biến này thành domain frontend thật trên Render — làm ở Bước 4.
- Test trên **điện thoại thật** cần origin là HTTPS thật (không phải `http://localhost`), nên theo đúng phạm vi đã thống nhất, việc này dời sang sau khi deploy (Bước 4). Trên máy tính local, có thể giả lập bằng Chrome DevTools (xem phần Kiểm thử bên dưới) mà không cần thiết bị thật.

## Chạy local — theo đúng thứ tự

### 1. Database
```bash
docker compose up -d postgres
```

### 2. Backend
```bash
cd backend
cp .env.example .env
# điền GOOGLE_CLIENT_ID; các biến PASSKEY_* đã có sẵn giá trị mặc định cho local
mvn spring-boot:run
```
Chạy tại `http://localhost:8080`. Flyway sẽ tự áp thêm migration mới (`V2__passkey_single_credential.sql`) khi khởi động.

### 3. Frontend
```bash
cd frontend
npm install
# mở src/environments/environment.ts, điền googleClientId
npm start   # http://localhost:4200
```

## Kiểm thử thủ công gợi ý (Bước 3)

Passkey cần một "authenticator" (vân tay/Face ID/Windows Hello, hoặc khoá bảo mật vật lý). Trên máy tính không có sẵn cảm biến sinh trắc học, dùng **Chrome DevTools Virtual Authenticator** để giả lập:

1. Mở Chrome → DevTools (F12) → menu `⋮` → **More tools → WebAuthn**.
2. Tick **"Enable virtual authenticator environment"**.
3. Bấm **"+ Add"**, chọn:
   - Protocol: `ctap2`
   - Transport: `internal`
   - Tick **"Supports resident keys"** (bắt buộc — Bước 3 dùng passkey dạng discoverable/resident)
   - Tick **"Supports user verification"**
4. Giữ tab DevTools này mở trong lúc test — mọi lệnh `navigator.credentials.create()/get()` sẽ tự "chạm vân tay" thành công ngay lập tức, không có popup hệ điều hành thật nào hiện lên.

Sau khi bật virtual authenticator:

1. `/register/email` → xác thực Google → mật khẩu → thông tin cá nhân → "Có, thiết lập Passkey" → tài khoản được tạo (đăng nhập luôn) → trang "Thiết lập Passkey" → bấm "Thiết lập Passkey ngay" → phải thành công, tự chuyển vào `/menu`.
2. Logout → `/login` → bấm "Đăng nhập với Passkey" → **không cần gõ email** → phải đăng nhập thẳng vào `/menu` (đúng passkey vừa tạo ở bước 1).
3. Vào `/profile/passkey` → phải thấy trạng thái "đang có 1 Passkey" → bấm "Xóa Passkey" → phải quay về menu, mở Console (F12 → Console) thấy log xác nhận đã xóa.
4. Quay lại `/profile/passkey` → giờ phải thấy trạng thái "chưa có Passkey" → bấm "Thêm Passkey" → lặp lại ceremony thiết lập thật → thành công.
5. Trong DevTools WebAuthn panel, xoá virtual authenticator (hoặc tắt "Enable virtual authenticator environment") rồi thử đăng nhập bằng Passkey lại → trình duyệt phải báo không tìm thấy passkey nào khớp (đúng hành vi mong đợi khi không còn authenticator).
6. Test một lượt đăng nhập bằng mật khẩu thường và bằng Google OAuth2 (Bước 1, 2) để đảm bảo chưa bị hỏng gì.

## Đã kiểm thử trong quá trình xây dựng — và điều CHƯA kiểm thử được

Quá trình chạy thử thật của bạn đã giúp phát hiện và sửa 3 lỗi liên tiếp (không thể tự kiểm chứng hết trong sandbox vì không build được backend — xem cảnh báo bên dưới):

1. **Lỗi build backend**: `JpaCredentialRepository` chưa bắt `Base64UrlException` (checked exception) khi decode dữ liệu passkey. Đã bọc `try/catch` ném `IllegalArgumentException`, thêm `@ExceptionHandler(IllegalArgumentException.class)` vào `GlobalExceptionHandler` (theo đúng bản sửa bạn gửi).
2. **Lỗi JS `Cannot read properties of undefined (reading 'length')`**: bộ mã hoá/giải mã WebAuthn tôi tự viết tay ban đầu có rủi ro lệch định dạng. Đã thay bằng API trình duyệt gốc `PublicKeyCredential.parseCreationOptionsFromJSON()`/`parseRequestOptionsFromJSON()`/`credential.toJSON()` (chuẩn WebAuthn Level 3, Baseline từ 3/2025).
3. **Lỗi `Required member 'challenge' is undefined`** (gốc rễ thật sự): `toCredentialsCreateJson()`/`toCredentialsGetJson()` của Yubico tự trả JSON dạng `{"publicKey": {...}}` sẵn, không phải object phẳng như tôi hiểu nhầm ban đầu. `PasskeyOptionsResponse` lại bọc thêm 1 lớp `"publicKey"` nữa → dữ liệu lồng 2 lớp. Đã sửa trong `PasskeyServiceImpl.toJson()`: tự bóc lớp `"publicKey"` thừa trước khi đóng gói response. Lỗi này chỉ tìm ra được nhờ bạn gửi log response JSON thật — cảm ơn bạn.

- ✅ **Frontend: đã build thật thành công** — `npm install && ng build` chạy trong sandbox này (môi trường lần này có quyền truy cập npm registry, khác với ghi chú ở Bước 1/2). Phát hiện và sửa 1 lỗi biên dịch thật (`TS2729` — field khởi tạo trước constructor) trong `login.component.ts`.
- ⚠️ **Backend: CHƯA build/chạy thử được** — sandbox này không có quyền truy cập Maven Central nên không chạy được `mvn compile`. Toàn bộ API của thư viện `com.yubico:webauthn-server-core` (tên class, tên method, thứ tự builder...) đã được tra cứu trực tiếp từ mã nguồn và Javadoc chính thức trên GitHub/developers.yubico.com (bản 2.9.0) thay vì suy đoán từ trí nhớ, nhưng **bạn nên là người đầu tiên chạy `mvn spring-boot:run` thật** — nếu gặp lỗi biên dịch hoặc lỗi runtime (đặc biệt là các đoạn liên quan `RelyingParty`, `CredentialRepository`, `RegisteredCredential`), gửi lại log lỗi để mình sửa ngay.
- Migration mới `V2__passkey_single_credential.sql` thêm ràng buộc unique + 2 cột — Flyway sẽ tự áp khi backend khởi động, không cần thao tác tay.

## Chuẩn bị Git (xong ở lần cập nhật này)

Repo được tổ chức theo dạng **monorepo** (1 repo GitHub duy nhất chứa cả
`backend/` và `frontend/`, mỗi package deploy thành 1 Render service riêng
qua "Root Directory") — theo lựa chọn đã chốt với bạn.

- `.gitignore` riêng cho từng package (`backend/.gitignore`, `frontend/.gitignore`)
  + 1 file gốc nhỏ cho rác OS/editor ở cấp cao nhất.
- Biến môi trường của cả 2 package đều nằm trong `.env` (không commit) +
  `.env.example` (commit, chỉ chứa placeholder):
  - `backend/.env.example` — như cũ.
  - `frontend/.env.example` — **mới**: Angular tự thân không đọc `.env`, nên
    có thêm `frontend/scripts/generate-env.js` tự sinh
    `src/environments/environment.ts`/`environment.prod.ts` từ `.env` (local)
    hoặc từ biến môi trường thật do Render inject (production) mỗi khi chạy
    `npm start`/`npm run build`. Xem chi tiết ở `frontend/README.md`.
- Chưa có unit/integration test nào trong `backend/src/test` — cân nhắc bổ
  sung trước khi lên production thật, không nằm trong phạm vi Bước 3 đã chốt
  nên chưa tự thêm.

### Đưa code lên GitHub
```bash
git init
git add .
git commit -m "Step 3: password + Google OAuth2 + Passkey login, ready for Render (Step 4)"
git branch -M main
git remote add origin <URL-repo-GitHub-của-bạn>
git push -u origin main
```
(Repo GitHub cần tạo trước trên github.com — trống, không kèm README/license
để tránh xung đột lúc `push` lần đầu.)

## Deploy lên Render (Bước 4)

Hướng dẫn chi tiết, **đã chốt và sẵn sàng thực hiện**: xem file
**[`DEPLOY.md`](./DEPLOY.md)** — gói **Free** cho cả backend + database,
domain mặc định `*.onrender.com`. File đó liệt kê rõ thứ tự tạo Postgres →
backend Web Service → frontend Static Site (kèm rule rewrite bắt buộc cho
Angular Router) → vòng lại cập nhật domain thật cho
`PASSKEY_RP_ID`/`PASSKEY_ORIGIN`, cùng danh sách biến môi trường cần khai
báo trên Render dashboard cho từng service, và các thay đổi code đã thực
hiện để deploy trơn tru (cổng lắng nghe theo `PORT` của Render, health check
endpoint thật).

## Cấu trúc thư mục
```
project-root/
├── backend/           # Spring Boot — xem backend/README.md
│   ├── .env.example
│   └── .gitignore
├── frontend/          # Angular — xem frontend/README.md
│   ├── .env.example
│   ├── scripts/generate-env.js
│   └── .gitignore
├── .gitignore         # chỉ rác OS/editor ở cấp gốc
├── DEPLOY.md          # kế hoạch chi tiết Bước 4 (Render)
└── docker-compose.yml # chỉ chạy Postgres cho local dev
```
