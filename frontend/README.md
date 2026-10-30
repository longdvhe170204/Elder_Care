# Elder_Care web (`frontend/`)

Giao diện web của hệ thống quản lý viện dưỡng lão: React 19, Vite 8, JavaScript (không TypeScript), Tailwind CSS v4, shadcn/ui (Base UI), lucide-react, font Be Vietnam Pro.

Đọc trước khi viết mã: [AGENTS.md](../AGENTS.md) (luật chung cho mọi người và mọi công cụ AI), [DESIGN.md](../DESIGN.md) (giao diện), [docs/team-plan.md](../docs/team-plan.md) (ai sở hữu thư mục nào). File này là luật cấu trúc của `frontend/`; khác với mã cũ thì theo file này.

## Lệnh

```bash
npm ci              # cài đúng phiên bản trong package-lock.json
npm run dev         # chạy thử ở http://localhost:5173 (đổi vai trò ở khối người dùng cuối menu)
npm test            # Vitest; một file: npx vitest run <đường dẫn>
npm run lint
npm run build
npm run gen:api     # sinh src/api/ từ specs/*/contracts/api-*.yaml
npx prettier --write <file>   # định dạng file mình sửa (chưa áp cho mã cũ)
```

Thêm component shadcn: `npx shadcn@latest add <tên>` (sinh vào `src/components/ui/`). Thêm thư viện npm: nhờ A (người giữ `package.json`).

## 1. Cấu trúc `src/`

| Thư mục | Nhiệm vụ | Ai sửa |
| --- | --- | --- |
| `api/` | Client sinh từ hợp đồng (`npm run gen:api`). Mỗi endpoint một hàm, ví dụ `getParameters`, `postApprovalRequestsByIdApprove`; `request()` trả `{ data, etag, status }`, lỗi ném `ApiError(status, problem)` | Không ai sửa tay. Trùng khi gộp nhánh thì chạy lại `npm run gen:api` |
| `common/` | Dùng chung cho mọi feature: khung ứng dụng và menu (`layout/`), lỗi (`problem.js`, `ProblemAlert.jsx`), ngày giờ (`dateTime.js`), trạng thái màn (`PageState.jsx`), đầu trang (`PageHeader.jsx`), nhãn trạng thái (`StatusBadge.jsx`), ghi chú an toàn (`CareNotes.jsx`), ảnh đại diện (`ResidentAvatar.jsx`), danh sách và khung chi tiết (`DetailLayout.jsx`, `useWide.js`), dải "Cần làm ngay" (`NowStrip.jsx`), đính chính (`correction/`), dữ liệu minh họa (`demoData.js`) | A duyệt mọi thay đổi |
| `features/<tên>/` | Màn hình của một feature (bảng mục 2) | Chủ feature |
| `components/ui/` | Component shadcn; chỉ đổi qua biến thể `cva` theo DESIGN.md | A duyệt |
| `lib/` | Tiện ích nhỏ không phụ thuộc nghiệp vụ | A duyệt |
| `test/` | Đồ dùng chung cho test: `setup.js`, `fetchRoutes.js` (`mockRoutes`, `json`, `problem`) | A duyệt |

Alias `@/` trỏ `src/`.

## 2. Thư mục màn hình theo feature

Mỗi feature chỉ ghi vào thư mục của mình. Tên dưới đây là tên chốt; cần thêm thư mục thì ghi vào mục glossary của feature trước.

| Feature | Thư mục `features/` | Chủ |
| --- | --- | --- |
| 000 | `parameters`, `approval-requests`, `audit-log` | A |
| 001 | `residents` | B |
| 002 | `login`, `accounts`, `permissions`, `pending-review`, `admin` (màn gộp tab Tài khoản, Tham số) | A |
| 003 | `beds` | C |
| 004 | `admissions` | B |
| 005 | `care-tasks` | C |
| 006 | `medication` | D |
| 007 | `alerts`, `incidents` | D |
| 008 | `shifts`, `handover` | E |
| 009 | `notifications` | A |
| 010 | `charges` | E |
| 011 | `meals` | D |
| 012 | `family-portal`, `relatives` | E |
| 013 | `belongings` | B |
| 014 | `activities` | C |
| 015 | `shift-swaps` | E |
| 016 | `dashboard`, `reports` | A |
| 017 | `balance` | E |
| 018 | `residence-registration` | B |
| 019 | `inventory` | C |

Màn mẫu của giai đoạn thiết kế (dữ liệu minh họa) đã nằm trong một số thư mục; chủ feature thay dần bằng màn gọi API thật.

## 3. Bên trong một thư mục feature

```
features/parameters/
  ParameterList.jsx         màn chính hoặc phần chính của màn
  ParameterForm.jsx         form một lệnh
  ParameterHistory.jsx      phần phụ (tab, khung chi tiết)
  TableValueEditor.jsx      thành phần chỉ feature này dùng
  parameterValue.js         hàm thuần: nhãn, định dạng, chuyển đổi (không JSX)
  ParameterForm.test.jsx    test cạnh file
  parameterValue.test.js
```

- **Tên file**: component PascalCase `.jsx` theo tên tiếng Anh ở glossary (`ThamSoForm.jsx` → `ParameterForm.jsx`); hàm thuần camelCase `.js`; test `<tên>.test.jsx`.
- **Màn gắn vào menu**: một component xuất ra (thường `XxxPage`), đăng ký một dòng trong `PAGES` của `src/App.jsx` và một mục trong `src/common/layout/navigation.js` (ghi dòng Phụ lục 27 làm căn cứ `roles`). Chỉ thêm đúng dòng của mình; trùng khi gộp thì giữ cả hai dòng. Màn nhận prop `role` (vai trò đang xem, chỉ để hiển thị) và `onNavigate(page, params)`.
- **Hậu tố gợi ý**: `XxxPage` (cả màn), `XxxList` (danh sách, bảng), `XxxForm` (form một lệnh), `XxxSheet`/`XxxDetail` (khung trượt chi tiết), `XxxCard` (thẻ trong lưới), `xxxLabels.js`/`xxxStatus.js` (nhãn enum → tiếng Việt).

## 4. Luật bắt buộc

1. **Không import chéo feature**: `features/a/` không import từ `features/b/`. Cần dùng chung thì đề xuất đưa lên `common/` (A duyệt).
2. **Gọi API chỉ qua `@/api`**: không gọi `fetch` trực tiếp, không tự viết URL. Endpoint chưa có trong `src/api/` nghĩa là hợp đồng chưa dịch hoặc chưa sinh lại.
3. **Lỗi**: bắt `ApiError`, đổi bằng `problemOf(error)`, hiện bằng `ProblemAlert`. 422 báo tại ô (lý do: `REASON_REQUIRED`); 412 hiện giá trị hiện hành mới kèm nút tải lại; 409 `ALREADY_PROCESSED` báo rồi tải lại; 403 khi tải màn dùng `PermissionDeniedState`. Không hiện `title` tiếng Anh của backend.
4. **Lệnh sửa**: gửi `If-Match` bằng ETag lấy từ lần đọc chi tiết; lệnh có lý do thì ô "Lý do" bắt buộc, tối đa 2000 ký tự.
5. **Quyền**: nút hiện theo `allowedCommands` hoặc `/me/permissions` do backend trả; vai trò trên màn chỉ để hiển thị. Không tự suy quyền từ vai trò khi backend đã trả.
6. **Ngày giờ**: chỉ dùng `formatDate`, `formatDateTime` trong `common/dateTime.js` (dd/MM/yyyy HH:mm, giờ Việt Nam). Gửi lên backend dạng ISO-8601 có `+07:00`.
7. **Chữ trên màn**: tiếng Việt, theo DESIGN.md mục 8. Tên trong mã tiếng Anh theo glossary.
8. **Giao diện** (DESIGN.md 6.x): màu, chữ, khoảng cách chỉ qua token và lớp `type-*` của `index.css`; không viết mã màu thô, không gradient. Dùng component có sẵn trước: `PageHeader` (đầu màn không bọc thẻ), `StatusBadge` (trạng thái chữ màu), `CareNotes`, `DetailLayout` (màn danh sách có chi tiết: khung cố định bên phải từ 1280px chỉ hiện khi chọn, hẹp hơn là `Sheet`; không tự viết lại), `NowStrip`, `ToggleGroup variant="filter"` (tab từ 768px, chip dưới 768px; không truyền `flex-wrap`). Màn mẫu tham khảo: `features/care-tasks/ShiftChecklistPage.jsx`. Trạng thái mới thêm vào `StatusBadge` và DESIGN.md mục 3.5 trong cùng thay đổi.
9. **Dữ liệu minh họa** chỉ ở `common/demoData.js`, đầu file ghi rõ là minh họa, tên người hư cấu tiếng Việt (DESIGN.md mục 8); màn đã có API thì không dùng dữ liệu minh họa.
10. **Chỉ trường của spec và hợp đồng** (DESIGN.md mục 13 điều 6): màn chỉ hiện trường có trong spec của feature và `contracts/api-NNN.yaml`; thiếu trường thì báo chủ module (team-plan mục 8), không tự lấy từ module khác. Dữ liệu minh họa viết đúng hình dạng hợp đồng và đọc qua một lớp chuyển đổi trong feature (mẫu: `features/care-tasks/checklistView.js` kèm test), để khi có client sinh từ hợp đồng chỉ sửa lớp đó.
11. **Trợ năng**: 12 điểm của DESIGN.md mục 9.2 phải đạt trước khi mở PR có màn mới hoặc màn sửa.

## 5. Test

- Vitest + Testing Library, `fireEvent` (không thêm `user-event`). Test viết trước, phải lỗi trước khi cài.
- Giả API bằng `mockRoutes()` trong `src/test/fetchRoutes.js`: `api.on('GET', '/parameters', json(200, …))`, `problem(412, 'VERSION_MISMATCH', {...})`; route không khai báo thì treo (trạng thái đang tải).
- Tên test có mã quy tắc ở đầu: `US4_9_412_hienGiaTriMoi_vaNutTaiLai`, `FR_049_...`.
- Truy vấn theo vai trò và nhãn (`getByRole`, `getByLabelText`), không theo lớp CSS.
