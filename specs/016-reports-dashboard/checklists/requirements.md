# Specification Quality Checklist: Báo cáo và dashboard

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-09-27
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain (Q-192, Q-193, Q-194 đã chốt 2026-09-27)
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

- [x] I. Mỗi FR có nguồn (mục 18, BR, UC-67, Q, FR của feature nguồn)
- [x] II. Không nêu công nghệ, bảng, màn hình, API
- [x] III. Feature chỉ đọc; không có thao tác trên dữ liệu nhóm 1, 2, 3 (FR-001)
- [x] IV. Không có thực thể có vòng đời (lần xuất, nếu có, là nhóm 3 chỉ ghi thêm); mỗi nhóm chỉ tiêu có kịch bản Given/When/Then
- [x] V. Ngưỡng, thời hạn gọi bằng CFG kèm mặc định; một tham số mới CFG-M14-01 (Q-203) đã đưa vào Phụ lục 25
- [x] VI. Quyền suy ra từ 4.4 dòng "Dashboard, báo cáo" và các dòng nguồn (FR-010)
- [x] VII. Điểm chưa rõ gắn mã Q đề xuất (Q-192 → Q-194)
- [x] VIII. Có SC đo được
- [x] IX. Chỉ spec, không plan/tasks/code

## Notes

- Lần kiểm tra 1: sửa nguồn chỉ tiêu "Giấy phép cơ sở sắp hết hạn" (10.4, feature 002 FR-039) thay cho tham chiếu sai.
- Lần kiểm tra 2 (sau khi chốt Q-192 → Q-194): cập nhật FR-004, FR-012, FR-061, FR-062; thêm mục I (FR-070 → FR-073), User Story 8, 9, SC-010, SC-011; bảng FR-010 thêm dòng "Nhân sự và ca trực". Mọi mục đạt.
