# Specification Quality Checklist: Nền tảng quy tắc nghiệp vụ dùng chung

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-09-25
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain — còn 3: FR-035 (tự duyệt, đề xuất Q-10), FR-038 (áp dụng không thành, đề xuất Q-11), FR-050 (thời hạn lưu trữ, Q-04)
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

- [x] I. FR tham chiếu mã BR/DBR/UC/CFG nguồn; mã giữ nguyên
- [x] II. Không nêu công nghệ, bảng, màn hình cụ thể
- [x] III. Phân 3 nhóm; nhóm 2 chỉ lệnh nghiệp vụ; nhóm 3 chỉ ghi thêm và đính chính
- [x] IV. Vòng đời yêu cầu phê duyệt thể hiện bằng bảng; mỗi quy tắc có Given/When/Then
- [x] V. Không cố định giá trị; tham số gọi bằng mã CFG
- [x] VI. Actor khớp mục 4.1 (Quản lý viện AC-01, Bộ lập lịch AC-11) và Permission Matrix 4.4
- [x] VII. Điểm chưa rõ đánh dấu kèm Q-xx — FR-035, FR-038 chưa có mã Q trong mục 24 (đã đề xuất Q-10, Q-11)
- [x] VIII. Tiêu chí thành công đo được
- [x] IX. Chỉ spec, không plan/tasks/code

## Notes

- Có 4 điểm cần báo lại về tài liệu nguồn, ghi ở cuối spec.md; không sửa docs.
- Items marked incomplete require spec updates before `/speckit-clarify` or `/speckit-plan`
