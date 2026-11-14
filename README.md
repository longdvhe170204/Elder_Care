# Elder_Care

Hệ thống quản lý viện dưỡng lão (đồ án nhóm).

## Cấu trúc

| Thư mục | Nội dung |
| --- | --- |
| `backend/` | Spring Boot 4.1, Java 25, MySQL 8.0; RESTful API `/api/v1`; gói `com.cambae.backend` chia `common` và module `m01` … `m15` |
| `docs/business/` | Nghiệp vụ (`nghiep-vu.md`) và luồng nghiệp vụ (`luong-nghiep-vu.md`) |
| `specs/` | Spec, plan, tasks của từng feature 000 → 019 (Spec Kit); hợp đồng API ở `specs/*/contracts/` |
| `.specify/` | Constitution, template, script của Spec Kit |

## Ba kho của hệ thống

| Kho | Nội dung |
| --- | --- |
| Elder_Care (kho này) | Backend, đặc tả, hợp đồng API, tài liệu nghiệp vụ và quy ước chung (CLAUDE.md, AGENTS.md, DESIGN.md) |
| [Elder_Care_Frontend](https://github.com/longdvhe170204/Elder_Care_Frontend) | Web React 19, Vite, JavaScript, Tailwind CSS v4, shadcn/ui |
| [Elder_Care_Mobile](https://github.com/longdvhe170204/Elder_Care_Mobile) | Một app Flutter chung cho nhân viên, người thân và người cao tuổi |

Web và app chỉ nói chuyện với backend qua API `/api/v1`. Tách thành ba kho ngày 2026-11-14 (trước đó là thư mục `frontend/`, `mobile/` của kho này); hai kho mới giữ lịch sử commit của thư mục tương ứng.

Clone ba kho cạnh nhau trong cùng thư mục cha, giữ tên thư mục như tên kho: `docker-compose.yml` của kho này build web từ `../Elder_Care_Frontend`, và `npm run gen:api` của web đọc hợp đồng từ `../Elder_Care/specs`.

## Chạy

Quy ước làm việc, lệnh build và test: xem [CLAUDE.md](CLAUDE.md).

```powershell
# Backend
cd "D:\FU learning\Fall26\Elder_Care\backend"
.\mvnw.cmd spring-boot:run

# Web
cd "D:\FU learning\Fall26\Elder_Care_Frontend"
npm run dev

# App
cd "D:\FU learning\Fall26\Elder_Care_Mobile"
flutter run -d chrome
```