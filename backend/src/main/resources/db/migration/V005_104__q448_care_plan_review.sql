-- Q-448 (2026-11-26): yêu cầu xem xét kế hoạch chăm sóc (FR-058). Mỗi nguồn của FR-011 (đánh giá đầu vào có nhu cầu,
-- đánh giá lại đổi mức hoặc nhu cầu, sự cố ngã, phụ lục đổi mức, lựa chọn chăm sóc giảm nhẹ) lưu một dòng OPEN trong cùng
-- lệnh với thông báo; cùng nguồn và mã tham chiếu chỉ một dòng. RESOLVED khi bản kế hoạch lập sau thời điểm phát sinh được
-- duyệt; NO_CHANGE khi người có quyền chọn "Không cần thay đổi" kèm lý do, hoặc hệ thống đóng khi kết thúc lưu trú.
-- Cú pháp dùng được ở MySQL 8.0 và 8.4.
CREATE TABLE care_plan_review (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 resident_id BIGINT NOT NULL,
 source VARCHAR(40) NOT NULL,
 source_id BIGINT NOT NULL,
 reference VARCHAR(200) NOT NULL,
 raised_at DATETIME(6) NOT NULL,
 status ENUM('OPEN','RESOLVED','NO_CHANGE') NOT NULL,
 closed_at DATETIME(6) NULL,
 closed_by BIGINT NULL,
 close_reason VARCHAR(500) NULL,
 resolved_plan_id BIGINT NULL,
 UNIQUE KEY uq_care_plan_review_source(resident_id,source,reference),
 CONSTRAINT fk_care_plan_review_plan FOREIGN KEY(resolved_plan_id) REFERENCES care_plan(id) ON DELETE RESTRICT,
 CONSTRAINT chk_care_plan_review_closed CHECK(
  (status='OPEN' AND closed_at IS NULL AND closed_by IS NULL AND close_reason IS NULL AND resolved_plan_id IS NULL)
  OR (status='RESOLVED' AND closed_at IS NOT NULL AND resolved_plan_id IS NOT NULL)
  OR (status='NO_CHANGE' AND closed_at IS NOT NULL AND close_reason IS NOT NULL AND CHAR_LENGTH(TRIM(close_reason))>0)),
 INDEX ix_care_plan_review_open(status,resident_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DataGroup=BUSINESS';
GRANT INSERT,UPDATE ON care_plan_review TO 'eldercare_app'@'%';
GRANT DELETE ON care_plan_review TO 'eldercare_retention'@'%';
