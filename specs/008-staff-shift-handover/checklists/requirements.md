# Specification Quality Checklist: Nhân viên, ca trực, phân công và bàn giao ca

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
  - Phạm vi, mục "Ngoài phạm vi" (và FR-020) – ranh giới 008/015 về lịch ca, ghi nhận vắng ca (Q-77).
  - Edge Cases, FR-044 – ai giữ trách nhiệm khi ca sau chưa xác nhận hoặc bàn giao đang Có ý kiến (Q-78).
  - FR-034 – vai trò được tính và cách hiểu ngưỡng tỷ lệ phục vụ (Q-79).
- Sửa trong vòng 1: User Story 4 kịch bản 6 bỏ khái niệm "công bố lại lịch" không có trong vòng đời, thay bằng Trưởng tầng nhận nhắc việc và hoàn tất bàn giao khi ca thiếu người phụ trách (khớp FR-004, FR-019, FR-041).
- Vòng kiểm tra 2 (2026-09-26): 3 điểm được chốt theo đề xuất (Q1: A, Q2: B, Q3: A), ghi ở mục Clarifications của spec. Q-77 → mục Phạm vi, FR-020; Q-78 → Edge Cases, FR-044, thêm FR-044a và User Story 1 kịch bản 12; Q-79 → FR-034. Không còn dấu [NEEDS CLARIFICATION]; mọi mục đạt.
- /speckit-clarify (2026-09-26): 5 câu hỏi, đều chọn A (Q-80 → Q-84), ghi ở "Session 2026-09-26 (lượt 2, /speckit-clarify)". Đã tích hợp vào FR-016, FR-020, FR-024, FR-025, FR-027, FR-041, FR-042, Key Entities, Edge Cases, User Story 1 (kịch bản 4), User Story 2 (kịch bản 6, 9), User Story 6 (kịch bản 6). Kiểm tra lại: mọi mục vẫn đạt.
- /speckit-clarify lượt 3 (2026-09-26, sau checklist business-rules): 5 câu, đều chọn A (Q-85 → Q-89), xử lý CHK001, CHK002, CHK013, CHK022, CHK028 của business-rules.md. Tích hợp vào FR-016, FR-022, FR-024, FR-036, FR-038, FR-040, FR-042, FR-043, FR-044, FR-044a, FR-048, Edge Cases, User Story 1 (kịch bản 2, 8, 12), Điểm báo lại 13. Hai tham số mới đề xuất CFG-M09-08, CFG-M09-09 thay hằng số 24 giờ (Constitution V). Kiểm tra lại: mọi mục vẫn đạt.
- /speckit-clarify lượt 4 (2026-09-26, sau checklist consistency): 5 câu, đều chọn A (Q-90 → Q-94), xử lý CHK001, CHK005, CHK007, CHK012, CHK013, CHK015 của consistency.md. Tích hợp vào FR-020, FR-024, FR-025, FR-044a, FR-048, Edge Cases, Điểm báo lại 16; đồng bộ spec 002 FR-028, spec 005 FR-032 và FR-047a, spec 007 FR-037. Kiểm tra lại: mọi mục vẫn đạt.
- Sau checklist privacy-audit (2026-09-26): thêm nhóm G (FR-050 → FR-060) và SC-012; không có dấu [NEEDS CLARIFICATION] mới; mọi mục vẫn đạt.
- Tuân thủ constitution: mỗi FR có tham chiếu nguồn (BR/DBR/UC/mục); giá trị cấu hình gọi bằng mã CFG kèm mặc định (CFG-M09-01 → 05, CFG-M06-02, CFG-M15-07); dữ liệu phân theo 3 nhóm của mục 1.5 (bảng đầu mục Requirements); vòng đời thể hiện bằng bảng (trạng thái làm việc, ca, phân công, bàn giao); không có diagram; actor và quyền theo 4.1, 4.4, 19.3 (FR-048).
- DBR-20 (FR-022, FR-038, FR-039, FR-046; User Story 1 kịch bản 6, 10, 11) và DBR-21 (FR-013, FR-030; User Story 2 kịch bản 2, User Story 3 kịch bản 1, 2) có kịch bản chấp nhận.
- 16 điểm mâu thuẫn hoặc thiếu trong tài liệu nguồn được báo lại ở cuối spec; `docs/nghiep-vu.md` đã được cập nhật tới Q-89 (điểm 15), các quyết định lượt 4 (Q-90 → Q-94, điểm 16) và `docs/phan-tich-yeu-cau.md` chưa được sửa.
- Items marked incomplete require spec updates before `/speckit-clarify` or `/speckit-plan`
