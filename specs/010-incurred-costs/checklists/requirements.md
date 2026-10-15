# Specification Quality Checklist: Chi phí phát sinh

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

## Constitution (I–IX)

- [x] I. Mỗi FR có nguồn (BR, DBR, UC, CFG, Q hoặc "suy ra từ"); mã được giữ nguyên
- [x] II. Không nêu công nghệ, bảng hay màn hình; "Excel/CSV" là định dạng nghiệp vụ đã ghi ở mục 23 và Q-05
- [x] III. Dữ liệu được phân nhóm theo 1.5; nhóm 2 chỉ đổi qua lệnh; nhóm 3 chỉ ghi thêm và điều chỉnh
- [x] IV. Vòng đời bảng chi phí, khoản chi phí, đề nghị mua hộ thể hiện bằng bảng; mỗi quy tắc BR-M11-01 → 09 có kịch bản Given/When/Then
- [x] V. Ngưỡng, hạn mức dùng mã CFG kèm mặc định (CFG-M11-01/02/03, CFG-M02-05/09, CFG-M15-05/06)
- [x] VI. Quyền khớp dòng "Chi phí, khoản điều chỉnh" và "Chốt kỳ, xuất kế toán" của 4.4; phần vượt ma trận (người đại diện đồng ý mua hộ) được báo lại
- [x] VII. Điểm chưa rõ đánh dấu [NEEDS CLARIFICATION] kèm mã Q đề xuất
- [x] VIII. Tiêu chí thành công đo được
- [x] IX. Chỉ có spec, không có plan, tasks hay mã nguồn

## Notes

- Lần kiểm tra 2 (2026-09-26): 3 dấu [NEEDS CLARIFICATION] đã được giải quyết theo trả lời của người dùng (Q1: A → Q-131, Q2: A → Q-133, Q3: A → Q-132), ghi ở mục Clarifications; FR-001, FR-002, FR-019 được viết lại; thêm kịch bản US3 #8, #9 và US7 #6. Mọi mục đạt.
- Lần kiểm tra 1: FR-020 bị đặt sai mục (dưới "nhập tay và mua hộ") và bảng trạng thái khoản dẫn sai FR-023. Đã sửa: chuyển FR-020 về cuối mục D, sửa dẫn chiếu thành FR-021.
- 12 điểm báo lại về tài liệu nguồn nằm ở cuối spec; chưa sửa `docs/` theo CLAUDE.md.
