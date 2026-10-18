# Elder_Care web (`frontend/`)

Giao diện web của hệ thống quản lý viện dưỡng lão: React 19, Vite, JavaScript, Tailwind CSS v4, shadcn/ui (Base UI), lucide-react, font Be Vietnam Pro.

- Quy định thiết kế (màu, chữ, trạng thái, khi nào dùng lưới thẻ hay bảng, component): [DESIGN.md](../DESIGN.md) ở gốc kho, chung cho web và mobile. Đọc trước khi thiết kế hay sửa màn hình.
- Quy ước chung của dự án, giai đoạn hiện tại: [CLAUDE.md](../CLAUDE.md).

## Lệnh

```bash
npm ci          # cài đúng phiên bản trong package-lock.json
npm run dev     # chạy thử ở http://localhost:5173
npm run lint
npm run build
```

Thêm component shadcn: `npx shadcn@latest add <tên>` (file sinh vào `src/components/ui/`).

## Cấu trúc `src/`

| Thư mục | Nội dung |
| --- | --- |
| `api/` | Client sinh từ `specs/*/contracts/api-*.yaml` (task 000 T010), không sửa tay. Chưa có |
| `common/` | Khung ứng dụng, menu theo vai trò, thành phần dùng chung, dữ liệu minh họa (`demoData.js`) |
| `features/` | Màn hình theo chức năng |
| `components/ui/` | Component shadcn; đổi giao diện bằng biến thể `cva` theo DESIGN.md |
| `lib/` | Tiện ích (`cn`) |

Alias `@/` trỏ `src/`.

Hiện là bản mẫu của giai đoạn thiết kế: các màn dùng dữ liệu minh họa, chưa gọi API.
