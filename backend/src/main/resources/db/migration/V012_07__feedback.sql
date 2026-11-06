-- 012 T030: phản hồi, khiếu nại (phan_hoi, nhóm 2 cho trạng thái; data-model mục 0, 0.1, 9; FR-058 → FR-066; BR-M10-05;
-- research F-13). Theo R-15: nội dung, trả lời, hướng xử lý, ghi chú, giao lại, đổi mức, mở lại (phan_hoi_muc, nhóm 3)
-- là record_event loại M10.FEEDBACK_ITEM gắn M10.FEEDBACK. Người gửi là người cao tuổi, loại Mong muốn, nguồn kênh người
-- cao tuổi, ghi trực tiếp và phan_hoi_y_kien thêm ở US9 (T065). Người cao tuổi, người thân, tài khoản là tham chiếu logic.
-- Cú pháp dùng được ở MySQL 8.0 và 8.4 (R-01).

CREATE TABLE feedback
(
    id                  BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id         BIGINT      NOT NULL,
    family_member_id    BIGINT      NOT NULL COMMENT 'Người gửi (FR-058)',
    type                ENUM ('SUGGESTION', 'COMPLAINT', 'QUESTION') NOT NULL,
    content_group       ENUM ('CARE', 'HEALTH_MEDICATION', 'EATING', 'COSTS_CONTRACT', 'OTHER') NOT NULL,
    source              ENUM ('PORTAL', 'COUNTER') NOT NULL COMMENT 'COUNTER: Hành chính ghi thay tại quầy',
    submitted_at        DATETIME(6) NOT NULL,
    priority            ENUM ('HIGH', 'NORMAL') NOT NULL,
    due_basis_at        DATETIME(6) NOT NULL COMMENT 'Thời điểm gửi hoặc Mở lại gần nhất (FR-061)',
    due_at              DATETIME(6) NOT NULL COMMENT 'due_basis_at + CFG-M10-01 theo mức, tính lúc đặt',
    assignee_role       ENUM ('FLOOR_MANAGER', 'NURSING_STAFF', 'OFFICE_STAFF', 'NURSING_HOME_MANAGER') NOT NULL,
    assignee_account_id BIGINT      NULL COMMENT 'Trống: cả nhóm vai trò tới khi một người Nhận xử lý (FR-059)',
    status              ENUM ('NEW', 'IN_PROGRESS', 'REPLIED', 'REOPENED', 'CLOSED') NOT NULL,
    overdue             BOOLEAN     NOT NULL DEFAULT FALSE,
    escalated_due_at    DATETIME(6) NULL COMMENT 'Hạn đã leo thang: một lần mỗi hạn (FR-063)',
    replied_at          DATETIME(6) NULL,
    closed_at           DATETIME(6) NULL,
    close_method        ENUM ('SENDER', 'AUTO') NULL,
    recorded_by         BIGINT      NOT NULL,
    version             INT         NOT NULL DEFAULT 0,
    created_at          DATETIME(6) NOT NULL,
    CONSTRAINT ck_feedback_closed CHECK ((status = 'CLOSED') = (closed_at IS NOT NULL AND close_method IS NOT NULL)),
    CONSTRAINT ck_feedback_replied CHECK (status NOT IN ('REPLIED', 'CLOSED') OR replied_at IS NOT NULL),
    CONSTRAINT ck_feedback_due CHECK (due_at > due_basis_at AND due_basis_at >= submitted_at),
    KEY ix_feedback_resident (resident_id, submitted_at),
    KEY ix_feedback_family_member (family_member_id, submitted_at),
    KEY ix_feedback_status_due (status, due_at),
    KEY ix_feedback_status_replied (status, replied_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=BUSINESS';

-- Ứng dụng không xóa; job loại bỏ dữ liệu xóa theo hồ sơ người cao tuổi (FeedbackRetention).
GRANT INSERT, UPDATE ON feedback TO 'eldercare_app'@'%';
GRANT DELETE ON feedback TO 'eldercare_retention'@'%';
