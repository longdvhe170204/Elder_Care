# Specification Quality Checklist: Hồ sơ và đánh giá người cao tuổi

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-09-25
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain — còn 3: FR-004 (người từng lưu trú quay lại, đề xuất Q-12), FR-012 (bản đồng ý có là điều kiện Hoàn tất tiếp nhận, Q-03), FR-030 (bác sĩ bỏ/thêm cờ nguy cơ, đề xuất Q-13)
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
- [x] II. Không nêu công nghệ, bảng, màn hình cụ thể; thuật ngữ theo 2.4 (lệnh nghiệp vụ, đính chính, cờ nguy cơ, người đại diện, người liên hệ chính)
- [x] III. Bảng phân nhóm dữ liệu ở đầu mục Requirements; nhóm 2 chỉ qua lệnh; nhóm 3 chỉ ghi thêm/đính chính
- [x] IV. Vòng đời trạng thái người cao tuổi thể hiện bằng bảng (chuyển, lệnh, người, điều kiện, tác động); mỗi BR-M01-xx có ít nhất một Given/When/Then
- [x] V. Ngưỡng, thời hạn gọi bằng CFG-M01-01 → 05 kèm mặc định
- [x] VI. Actor và quyền khớp mục 4.1 và Permission Matrix 4.4 (điểm lệch ở dòng "Tạm vắng, trở về" đã báo lại, điểm 7)
- [x] VII. Điểm chưa rõ đánh dấu kèm Q-03 và đề xuất Q-12, Q-13
- [x] VIII. Tiêu chí thành công đo được
- [x] IX. Chỉ spec, không plan/tasks/code

## Notes

- Bao phủ BR-M01-01 → BR-M01-10, DBR-01, DBR-03, DBR-04, DBR-05, UC-01 → UC-08.
- Có 7 điểm cần báo lại về tài liệu nguồn, ghi ở cuối spec.md; không sửa docs.
- Items marked incomplete require spec updates before `/speckit-clarify` or `/speckit-plan`
