# Specification Quality Checklist: Chỉ số sức khỏe, cảnh báo và sự cố

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-09-26
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

## Notes

- Vòng kiểm tra 1 (2026-09-26): mọi mục đạt, trừ 3 dấu [NEEDS CLARIFICATION] (đề xuất mã mới, chưa có trong mục 24):
  - FR-047a – cảnh báo mức Khẩn cấp có tự tạo sự cố Khẩn cấp và kích hoạt quy trình khẩn cấp (thông báo người liên hệ chính) không (Q-67).
  - FR-060, FR-061 (và User Story 8 kịch bản 3) – ai xác nhận danh sách tiếp xúc: BR-M05-10 nói Điều dưỡng, Permission Matrix 4.4 cho Điều dưỡng quyền X (Q-68).
  - FR-063 (và Key Entities "Khoanh vùng") – đơn vị "khu" khi khoanh vùng: khu vực, tầng, hay tập phòng (Q-69; feature 003 đã báo lại).
- Sửa trong vòng 1: đánh số lại nhóm quy tắc xu hướng (FR-039 → FR-039c); sửa tham chiếu FR-016 → FR-019 (User Story 10), feature 005 FR-012 → FR-020 (công việc hủy khi vắng); bỏ việc dùng CFG-M05-01 làm khoảng thời gian nhận biết sự cố Khẩn cấp trùng (FR-041).
- Tuân thủ constitution: mỗi FR có tham chiếu nguồn (BR/DBR/UC/mục); giá trị cấu hình gọi bằng mã CFG kèm mặc định (CFG-M05-01 → 08, CFG-M06-01 → 03, CFG-M13-01, CFG-M04-08) và một tham số mới đề xuất CFG-M06-04; dữ liệu phân theo 3 nhóm của mục 1.5 (bảng đầu mục Requirements); vòng đời thể hiện bằng bảng (lịch đo, ngưỡng, cảnh báo, sự cố, danh sách tiếp xúc, khoanh vùng); không có diagram; actor và quyền theo 4.1, 4.4 (FR-036, FR-041, FR-080).
- DBR-18 (FR-028, FR-030; User Story 3 kịch bản 4, 9) và DBR-19 (FR-046, FR-048a; User Story 5 kịch bản 3, 8) có kịch bản chấp nhận.
- Ngoại lệ ghi sự cố ngoài phạm vi (feature 002 FR-044a), việc feature 002 báo lại cần phản ánh ở spec này, đã có ở FR-041, FR-048.
- 12 điểm mâu thuẫn hoặc thiếu trong tài liệu nguồn được báo lại ở cuối spec; chưa sửa `docs/`.
- Items marked incomplete require spec updates before `/speckit-clarify` or `/speckit-plan`
