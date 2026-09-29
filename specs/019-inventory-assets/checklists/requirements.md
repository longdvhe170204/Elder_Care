# Specification Quality Checklist: Kho nguyên liệu và tài sản của viện

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-09-28
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

- [x] I. Mỗi FR có nguồn (7.7, 12.7, BR-M03-15 → 18, BR-M08-17 → 20, DBR-31, DBR-32, UC-89 → UC-92, Q-220, Q-231 → Q-233)
- [x] II. Không nêu công nghệ
- [x] III. Phiếu kho là nhóm 3 (chỉ ghi thêm, phiếu đảo); tồn, trạng thái tài sản, lịch xe chỉ đổi qua lệnh
- [x] IV. Có bảng trạng thái đề nghị nhập, phiếu kiểm kê, tài sản, lịch xe; mỗi quy tắc có kịch bản
- [x] V. CFG-M03-09, CFG-M08-08, CFG-M08-09 kèm mặc định
- [x] VI. Quyền khớp Phụ lục 27 dòng "Kho nguyên liệu", "Tài sản, lịch xe" (chú thích ³³, ³⁴ đã sửa theo Q-233) và 19.3
- [x] VII. Không có điểm để ngỏ; cách áp chưa có trong nguồn ghi ở Assumptions và Điểm cần báo lại 1 → 3
- [x] VIII. Có SC-001 → SC-006 đo được
- [x] IX. Chỉ specify

## Notes

- Lần kiểm tra 1 (2026-09-28): mọi mục đạt. Đã bỏ một trường hợp biên tự thêm nhãn "ghi muộn" không có căn cứ trong nguồn.
- Lần kiểm tra 2 (2026-09-29): mục VI đạt lại sau khi đưa Clarification 2026-09-29 vào tài liệu nguồn (7.7, 12.7, BR-M03-15, BR-M08-19, 19.3, UC-92, Phụ lục 27 ³⁴, Q-231 → Q-233 ở 24.2). Điểm cần báo lại 1 → 5 đã xử lý.
- Lần kiểm tra 3 (2026-09-29, sau checklist cross-feature CHK042, CHK043, CHK047, CHK048): thêm Q-237 → Q-240 vào Clarifications, FR-011, FR-018, bảng vòng đời tài sản. Mọi mục vẫn đạt.
- Lần kiểm tra 4 (2026-09-29, sau checklist cross-feature CHK044, CHK058): Q-242, Q-243 vào Clarifications, FR-012, FR-013, FR-015. Mọi mục vẫn đạt.
