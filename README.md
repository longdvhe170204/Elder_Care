# Elder_Care

Hệ thống quản lý viện dưỡng lão (đồ án nhóm).

## Cấu trúc

| Thư mục | Nội dung |
| --- | --- |
| `backend/` | Spring Boot 4.1, Java 25, MySQL 8.0; RESTful API `/api/v1`; gói `com.cambae.backend` chia `common` và module `m01` … `m15` |
| `frontend/` | Web React 19, Vite, JavaScript, Tailwind CSS v4, shadcn/ui |
| `mobile/` | Một app Flutter chung cho nhân viên và người thân |
| `docs/business/` | Nghiệp vụ (`nghiep-vu.md`) và luồng nghiệp vụ (`luong-nghiep-vu.md`) |
| `specs/` | Spec, plan, tasks của từng feature 000 → 019 (Spec Kit) |
| `.specify/` | Constitution, template, script của Spec Kit |

Quy ước làm việc, lệnh build và test: xem [CLAUDE.md](CLAUDE.md).
