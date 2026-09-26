# Specification Quality Checklist: Phòng, giường và phân bổ giường

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-09-25
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain — còn 3: FR-010 (chuyển người trong/ra khỏi phòng cách ly, đề xuất Q-17), FR-024 (phân bổ khi giữ giường cho người vắng, đề xuất Q-18), FR-034 (đổi mức chăm sóc khi phòng hiện tại không cho phép, đề xuất Q-19)
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
- [x] II. Không nêu công nghệ, bảng, màn hình cụ thể; thuật ngữ theo 2.4 (lệnh nghiệp vụ, đính chính, người phụ trách ca…)
- [x] III. Bảng phân nhóm dữ liệu ở đầu mục Requirements; trạng thái giường, cách ly, phân bổ chỉ qua lệnh; bản ghi phân bổ đã đóng bất biến, chỉ đính chính
- [x] IV. Vòng đời trạng thái giường thể hiện bằng bảng (chuyển, lệnh/sự kiện, người, điều kiện, tác động); mỗi BR-M03-01 → 07 có ít nhất một Given/When/Then
- [x] V. Thời hạn, sức chứa gọi bằng CFG-M02-02, CFG-M02-05, CFG-M03-01 kèm mặc định
- [x] VI. Actor và quyền khớp mục 4.1 và Permission Matrix 4.4 (điểm lệch về cách ly phòng, bảo trì giường, phê duyệt chuyển giường đã báo lại, điểm 5, 6, 8)
- [x] VII. Điểm chưa rõ đánh dấu kèm đề xuất Q-17, Q-18, Q-19 — chờ người dùng trả lời
- [x] VIII. Tiêu chí thành công đo được
- [x] IX. Chỉ spec, không plan/tasks/code

## Notes

- Bao phủ BR-M03-01 → BR-M03-07, DBR-09, DBR-10, UC-19 → UC-21; dùng BR-M02-01, 02, 04, 06, 07 và BR-M05-10 → 12 ở ranh giới với feature 004 và 007.
- Có 10 điểm cần báo lại về tài liệu nguồn, ghi ở cuối spec.md; không sửa docs.
- Vòng kiểm tra 1: sửa một tham chiếu CFG không tồn tại (US5 kịch bản 3) và gom 6 marker trùng thành 3.
- Items marked incomplete require spec updates before `/speckit-clarify` or `/speckit-plan`
