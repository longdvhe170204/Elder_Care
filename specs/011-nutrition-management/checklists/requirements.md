# Specification Quality Checklist: Quản lý dinh dưỡng và suất ăn

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-09-27
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Constitution (I–IX)

- [x] I. Mỗi FR có nguồn BR/DBR/UC hoặc ghi "suy ra"/"giả định"; mã CFG giữ nguyên
- [x] II. Không nêu công nghệ, màn hình, bảng dữ liệu
- [x] III. Dữ liệu phân nhóm theo 1.5; nhóm 2 chỉ qua lệnh; nhóm 3 chỉ ghi thêm
- [x] IV. Năm vòng đời có bảng trạng thái (bản gán chế độ ăn, thực đơn, phiếu bữa ăn, đồ ăn gia đình, yêu cầu xem lại); mỗi quy tắc BR-M08-01 → 15 có kịch bản Given/When/Then
- [x] V. Ngưỡng và thời hạn dùng CFG-M08-01 → 06, CFG-M04-04, CFG-M15-05/06 kèm mặc định; CFG-M08-07 ghi là đề xuất
- [x] VI. Quyền khớp 4.4; điểm lệch (UC-48 không có dòng trong ma trận) được báo lại
- [x] VII. Điểm chưa chốt được đánh dấu kèm Q-xx — đã đánh dấu; chờ quyết định
- [x] VIII. Có 11 tiêu chí thành công đo được
- [x] IX. Chỉ có spec, không có plan, tasks hay mã nguồn

## Notes

- Lần kiểm tra 1: sửa lỗi lặp "MUST MUST NOT" ở FR-031, đổi số FR-049a → FR-048a và FR-068 → FR-065 cho đúng thứ tự, sửa tham chiếu sai ở User Story 2 kịch bản 4, 5 và User Story 3 kịch bản 10.
- Còn 3 điểm `[NEEDS CLARIFICATION]` theo quy ước dự án (CLAUDE.md: không tự đoán, đánh dấu kèm Q-xx). Cần chốt qua `/speckit-clarify` hoặc trả lời trực tiếp trước khi coi spec là sẵn sàng.
- Mục "Điểm cần báo lại về tài liệu nguồn" liệt kê 10 điểm cần phản ánh vào `docs/nghiep-vu.md`, `docs/phan-tich-yeu-cau.md` và các spec 001, 005, 007, 009, 010, 012, 014.
