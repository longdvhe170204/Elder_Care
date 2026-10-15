# Specification Quality Checklist: Khai báo tạm trú, lưu trú

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

- [x] I. Mỗi FR có nguồn (6.10, BR-M02-11 → 14, DBR-33, UC-80, UC-81)
- [x] II. Không nêu công nghệ; "Cổng dịch vụ công" là kênh nghiệp vụ ở mục 23
- [x] III. Khai báo là nhóm 2, chỉ đổi qua lệnh; nội dung đã ghi sửa bằng đính chính
- [x] IV. Có bảng trạng thái khai báo (FR-005); mỗi quy tắc có kịch bản Given/When/Then
- [x] V. Ngưỡng, hạn dùng CFG-M02-09, CFG-M02-11 → 13 kèm mặc định
- [x] VI. Quyền khớp Phụ lục 27 dòng "Khai báo tạm trú, lưu trú" và 19.3
- [x] VII. Q-215 đã chốt (Clarification 2026-09-29, 24.2); không còn điểm "theo mặc định" hay [NEEDS CLARIFICATION]
- [x] VIII. Có SC-001 → SC-005 đo được
- [x] IX. Chỉ specify

## Notes

- Lần kiểm tra 1 (2026-09-28): mọi mục đạt.
- Lần kiểm tra 2 (2026-09-29, sau `/speckit-clarify`): mọi mục đạt. Q-215 chốt (mốc 30 ngày cộng dồn, gia hạn dưới ngưỡng khai báo lại, người thường trú cùng xã/phường dùng Thông báo lưu trú) và đã đưa vào 6.10, BR-M02-11, BR-M02-14, 24.2, BF-01. Điểm cần báo lại 2 đã đưa vào BR-M02-11; điểm 3 chỉ là ghi nhận (khớp 6.10).
- Lần kiểm tra 3 (2026-09-29, sau checklist cross-feature CHK051): Q-241 (chuỗi hợp đồng nối tiếp) vào Clarifications, FR-001, Edge Cases. Mọi mục vẫn đạt.
