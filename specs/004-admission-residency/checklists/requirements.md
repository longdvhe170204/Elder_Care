# Specification Quality Checklist: Tiếp nhận và lưu trú

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-09-25
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

- Vòng kiểm tra 1 (2026-09-25): mọi mục đạt, trừ 3 dấu [NEEDS CLARIFICATION]:
  - FR-029 – hợp đồng qua ngày kết thúc mà chưa có quyết định (đề xuất Q-20).
  - FR-066 – thứ tự giữa điều kiện "chi phí đã chốt" (5.6) và việc chốt chi phí khi kết thúc lưu trú (BR-M11-08) (đề xuất Q-21).
  - FR-074 – mốc bắt đầu tính phí lưu trú (đề xuất Q-22).
- Clarify (2026-09-25): 5 câu đã trả lời; 3 dấu trên đã được thay (Q-20 → FR-029, Q-21 → FR-066, Q-22 → FR-074, FR-075, FR-076); thêm Q-23 (FR-025a, duyệt điều khoản hợp đồng khác chuẩn) và Q-24 (FR-057, đếm ngày vắng khi nhập viện); tham số đề xuất mới CFG-M02-10. Không còn dấu [NEEDS CLARIFICATION].
- Tuân thủ constitution: mỗi FR có tham chiếu nguồn (BR/DBR/UC/mục); các giá trị cấu hình đều gọi bằng mã CFG kèm mặc định; dữ liệu phân theo 3 nhóm của mục 1.5 (bảng đầu mục Requirements); vòng đời thể hiện bằng bảng (hồ sơ chờ, hợp đồng); không có diagram; actor và quyền theo 4.1, 4.4.
- 14 điểm mâu thuẫn hoặc thiếu trong tài liệu nguồn được báo lại ở cuối spec; chưa sửa `docs/`.
- Items marked incomplete require spec updates before `/speckit-clarify` or `/speckit-plan`
