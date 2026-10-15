# Specification Quality Checklist: Lịch ca xoay vòng, phủ ca tối thiểu, đổi ca và nghỉ đột xuất

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-09-27
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain — Q-177 (A), Q-178 (A), Q-179 (B) đã chốt ngày 2026-09-27
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

- [x] I. Mỗi FR có nguồn BR/UC/CFG/Q; mã giữ nguyên
- [x] II. Không nêu công nghệ, màn hình, bảng
- [x] III. Dữ liệu phân nhóm 1.5; nhóm 2 chỉ qua lệnh; nhóm 3 chỉ ghi thêm
- [x] IV. Bảng trạng thái cho yêu cầu đổi ca, nghỉ đột xuất, cảnh báo thiếu phủ, lời mời; mỗi BR-M09-09 → 11 có kịch bản Given/When/Then
- [x] V. Mốc thời gian, ngưỡng gọi bằng CFG kèm mặc định (CFG-M09-02, 06, 07, 10, 11)
- [x] VI. Quyền khớp 4.4 (dòng "Lịch ca, phân công", "Yêu cầu đổi ca"); khoảng trống ghi ở "Điểm cần báo lại"
- [x] VII. Điểm chưa rõ đã chốt thành Q-177 → Q-179, ghi ở mục Clarifications
- [x] VIII. Có tiêu chí thành công đo được
- [x] IX. Không có plan, tasks, mã nguồn

## Notes

- Lượt kiểm 1 (2026-09-27): mọi mục đạt trừ 3 điểm [NEEDS CLARIFICATION] (Q-177 → Q-179), đang chờ người dùng chọn phương án.
- Lượt kiểm 2 (2026-09-27): sau khi áp Q-177 → Q-179, mọi mục đạt. Ngoại lệ quyền Q-177 (Quản lý viện duyệt ca toàn viện) được ghi ở "Điểm cần báo lại" điểm 1 để phản ánh vào Q-15 và 19.3.
- Lượt kiểm 3 (2026-09-27): sau clarify Q-180 → Q-184 và sửa theo checklist consistency (Q-185 → Q-188), mọi mục vẫn đạt.
- Lượt kiểm 4 (2026-09-27): sau checklist business-rules (Q-189 → Q-191), mọi mục vẫn đạt.
- Items marked incomplete require spec updates before `/speckit-clarify` or `/speckit-plan`
