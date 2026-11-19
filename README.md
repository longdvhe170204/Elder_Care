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
| Elder_Care (kho này) | Backend, đặc tả, hợp đồng API, tài liệu nghiệp vụ và quy ước chung (CLAUDE.md, AGENTS.md) |
| [Elder_Care_Frontend](https://github.com/longdvhe170204/Elder_Care_Frontend) | Web React 19, Vite, JavaScript, Tailwind CSS v4, shadcn/ui; quy tắc giao diện web và app DESIGN.md (từ 2026-11-20) |
| [Elder_Care_Mobile](https://github.com/longdvhe170204/Elder_Care_Mobile) | Một app Flutter chung cho nhân viên, người thân và người cao tuổi |

Web và app chỉ nói chuyện với backend qua API `/api/v1`. Tách thành ba kho ngày 2026-11-14 (trước đó là thư mục `frontend/`, `mobile/` của kho này); hai kho mới giữ lịch sử commit của thư mục tương ứng.

Khi phát triển, clone ba kho cạnh nhau trong cùng thư mục cha, giữ tên thư mục như tên kho: `npm run gen:api` của web đọc hợp đồng từ `../Elder_Care/specs`, và `docker-compose.build.yml` build web từ `../Elder_Care_Frontend`.

## Chạy bản demo bằng Docker

Chỉ cần Docker, `docker-compose.yml` và file `.env` (chép từ `.env.docker.example`, điền ba mật khẩu); không cần clone kho web hay cài JDK, Node, MySQL. Backend và web dùng image dựng sẵn trên GHCR, được đẩy lên mỗi khi CI xanh trên `develop`.

```powershell
docker compose pull
docker compose up -d
```

Web ở `http://localhost:8081`. Build từ mã trên máy thay cho image dựng sẵn: `docker compose -f docker-compose.yml -f docker-compose.build.yml up -d --build`.

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