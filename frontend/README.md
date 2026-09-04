# Frontend — Auth App

Angular 18 (standalone components), giao tiếp với `backend/` qua REST API.
Toàn bộ màu sắc/theme nằm trong 1 file duy nhất: `src/app/shared/styles/theme.css`.

## Cấu hình môi trường (.env)

Angular không tự đọc file `.env` — cấu hình cuối cùng nằm ở
`src/environments/environment.ts` (dev) và `environment.prod.ts` (prod), được
biên dịch cứng vào bundle lúc build. Để vẫn dùng `.env` như backend, project
này có script `scripts/generate-env.js`: đọc biến môi trường rồi tự sinh 2
file trên **trước mỗi lần** `npm start` / `npm run build` (đã gắn sẵn vào
`package.json`, không cần chạy tay).

Thứ tự ưu tiên đọc biến:
1. Biến môi trường thật của hệ thống (trên Render: đặt trong dashboard, xem
   phần Deploy bên dưới) — luôn được ưu tiên nếu tồn tại.
2. File `.env` trong thư mục `frontend/` (chỉ dùng cho local — không commit).
3. Giá trị placeholder mặc định, kèm cảnh báo ra console nếu thiếu.

### Chạy local
```bash
cp .env.example .env
# mở .env, điền GOOGLE_CLIENT_ID (API_BASE_URL để mặc định nếu backend chạy localhost:8080)
npm install
npm start   # tự sinh environment.ts từ .env rồi chạy ng serve tại http://localhost:4200
```

`src/environments/environment.ts` / `environment.prod.ts` **vẫn được commit**
vào git (chứa giá trị placeholder an toàn) để clone lần đầu vẫn biên dịch
được trước khi ai đó chạy `npm start`/`npm run build` — 2 file này sẽ tự bị
ghi đè mỗi lần chạy 2 lệnh đó, không cần sửa tay và sửa tay cũng sẽ bị mất.

## Các lệnh khác

| Lệnh | Mô tả |
|---|---|
| `npm start` | Sinh `environment.ts` từ `.env`, chạy dev server tại `:4200` |
| `npm run build` | Sinh `environment.prod.ts`, build production vào `dist/frontend` |
| `npm run build:dev` | Build development (không minify) |
| `npm test` | Chạy unit test (Karma) |
| `npm run env:dev` / `npm run env:prod` | Chỉ sinh file environment tương ứng, không build |

## Deploy lên Render (Bước 4)

- Tạo **Static Site** (hoặc Web Service nếu muốn tự SSR sau này) trên Render,
  trỏ **Root Directory** vào `frontend/` (repo monorepo — xem README gốc).
- Build Command: `npm install && npm run build`
- Publish Directory: `dist/frontend`
- Khai báo Environment Variables trong dashboard Render (Settings > Environment):
  - `API_BASE_URL` = URL backend đã deploy + `/api` (vd: `https://authapp-backend.onrender.com/api`)
  - `GOOGLE_CLIENT_ID` = cùng giá trị với `GOOGLE_CLIENT_ID` bên backend
- Không cần file `.env` nào trên Render — build command tự đọc 2 biến trên
  từ dashboard nhờ `scripts/generate-env.js`.
- Sau khi có URL frontend thật, quay lại cập nhật:
  - `FRONTEND_URL`, `COOKIE_SECURE=true` bên backend (CORS + cookie).
  - `PASSKEY_RP_ID`, `PASSKEY_ORIGIN` bên backend (đổi sang domain frontend thật — bắt buộc cho passkey).
  - "Authorized JavaScript origins" của Google OAuth Client ID (thêm domain frontend Render).

## Cấu trúc thư mục
```
src/app/
├── core/            # services, guards, models, utils dùng chung
├── features/
│   ├── login/
│   ├── register/steps/   # 5 bước: email -> password -> profile -> passkey-prompt -> passkey-setup
│   ├── menu/
│   └── profile/     # profile-view, change-password, passkey-manage
└── shared/styles/    # theme.css (màu), shared.css (class dùng chung)
```
