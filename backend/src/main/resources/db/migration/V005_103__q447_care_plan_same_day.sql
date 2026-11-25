-- Q-447 (2026-11-25): người chưa có kế hoạch Hiệu lực thì kế hoạch có hiệu lực ngay trong ngày duyệt, nên ngày hiệu lực
-- được bằng ngày duyệt; bản Chờ hiệu lực bị bản sau thay thì kết thúc với lý do SUPERSEDED. WITHDRAWN giữ cho dữ liệu cũ.
-- Cú pháp dùng được ở MySQL 8.0 và 8.4.
ALTER TABLE care_plan DROP CHECK chk_care_plan_approval;
ALTER TABLE care_plan ADD CONSTRAINT chk_care_plan_approval
    CHECK(approved_at IS NULL OR (effective_from IS NOT NULL AND effective_from>=DATE(approved_at)));
ALTER TABLE care_plan MODIFY end_reason
    ENUM('NEW_REVISION','FINAL_STATUS','RETURNED','CANCELLED','WITHDRAWN','SUPERSEDED') NULL;
-- Bản Chờ hiệu lực không còn chặn lập bản sửa: "bản mở" chỉ còn Nháp, Chờ duyệt; Chờ hiệu lực có ràng buộc riêng
-- (tối đa một bản Chờ hiệu lực mỗi người cao tuổi, FR-004).
ALTER TABLE care_plan MODIFY open_resident BIGINT
    GENERATED ALWAYS AS(IF(status IN('DRAFT','PENDING_APPROVAL'),resident_id,NULL)) STORED;
ALTER TABLE care_plan ADD COLUMN scheduled_resident BIGINT
    GENERATED ALWAYS AS(IF(status='PENDING_EFFECT',resident_id,NULL)) STORED;
ALTER TABLE care_plan ADD UNIQUE KEY uq_care_plan_scheduled(scheduled_resident);
